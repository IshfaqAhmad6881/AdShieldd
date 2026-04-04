package com.FusionCoreTech.myapplication.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.ProductDetails.PricingPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play **subscription** product IDs. Must match exactly:
 * Play Console → Monetize → Products → **Subscriptions** → create two products:
 * - [PremiumProductIds.MONTHLY] (e.g. 1-month base plan)
 * - [PremiumProductIds.ANNUAL] (e.g. 1-year base plan, optional free trial / intro)
 *
 * Do **not** use one-time “In-app products” for these IDs if you query with [ProductType.SUBS].
 */
object PremiumProductIds {
    const val MONTHLY = "premium_monthly"
    const val ANNUAL = "premium_annual"
}

private const val PREFS_NAME = "adshield_prefs"
private const val KEY_PREMIUM = "has_active_premium_subscription"

class BillingManager(private val activity: Activity) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _isPremium = MutableStateFlow(readPremiumFlag())
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private var onPurchaseSuccess: ((String) -> Unit)? = null
    private var onPurchaseError: ((String) -> Unit)? = null

    private fun appContext(): Context = activity.applicationContext

    private fun readPremiumFlag(): Boolean =
        appContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_PREMIUM, false)

    private fun writePremiumFlag(active: Boolean) {
        appContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PREMIUM, active).apply()
        _isPremium.value = active
    }

    fun startConnection() {
        if (billingClient?.isReady == true) return
        billingClient = BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                _connectionState.value = ok
                if (ok) {
                    querySubscriptionProducts()
                    syncActivePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = false
            }
        })
    }

    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
        _connectionState.value = false
    }

    private fun querySubscriptionProducts() {
        val products = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PremiumProductIds.MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PremiumProductIds.ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        billingClient?.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && !list.isNullOrEmpty()) {
                _productDetails.value = list.associateBy { it.productId }
            }
        }
    }

    /** Active subscriptions from Play (after buy, restore, or app start). */
    private fun syncActivePurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val active = purchases.orEmpty().any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.any { it == PremiumProductIds.MONTHLY || it == PremiumProductIds.ANNUAL }
            }
            writePremiumFlag(active)
            purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .forEach { acknowledgeIfNeeded(it) }
        }
    }

    /** Use after user taps “Restore purchases”. */
    fun restorePurchases(onDone: (ok: Boolean, errorMessage: String?) -> Unit) {
        val client = billingClient
        if (client == null || !client.isReady) {
            onDone(false, "Billing not ready")
            return
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val active = purchases.orEmpty().any { p ->
                        p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            p.products.any { it == PremiumProductIds.MONTHLY || it == PremiumProductIds.ANNUAL }
                    }
                    purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                        .forEach { acknowledgeIfNeeded(it) }
                    writePremiumFlag(active)
                    onDone(active, if (active) null else "No subscription found")
                }
                else -> onDone(false, result.debugMessage)
            }
        }
    }

    /**
     * Same subscription offer Play uses for [launchPurchaseFlow] (first available base offer).
     */
    private fun primarySubscriptionOffer(productId: String) =
        _productDetails.value[productId]?.subscriptionOfferDetails?.firstOrNull()

    /**
     * Recurring charge phase (last phase = paid period after any free/intro trial phases).
     */
    fun getRecurringPricingPhase(productId: String): PricingPhase? {
        val offer = primarySubscriptionOffer(productId) ?: return null
        val phases = offer.pricingPhases?.pricingPhaseList ?: return null
        if (phases.isEmpty()) return null
        return phases.last()
    }

    /**
     * Formatted price from Google Play for the user’s Play country / currency (localized).
     * Uses the recurring phase of the primary subscription offer.
     */
    fun getFormattedPrice(productId: String): String =
        getRecurringPricingPhase(productId)?.formattedPrice.orEmpty()

    /** ISO 8601 duration, e.g. P1M, P1Y — from the recurring phase. */
    fun getRecurringBillingPeriodIso(productId: String): String? =
        getRecurringPricingPhase(productId)?.billingPeriod

    fun getSubscriptionOfferDetails(productId: String) =
        _productDetails.value[productId]?.subscriptionOfferDetails?.firstOrNull()

    fun launchPurchaseFlow(
        productId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onPurchaseSuccess = onSuccess
        onPurchaseError = onError
        val details = _productDetails.value[productId]
        if (details == null) {
            onError("Products still loading or not configured in Play Console for: $productId")
            clearCallbacks()
            return
        }
        val offer = details.subscriptionOfferDetails?.firstOrNull()
        if (offer == null) {
            onError("No subscription offer (add a base plan in Play Console for this product).")
            clearCallbacks()
            return
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))
            .build()
        val result = billingClient?.launchBillingFlow(activity, flow)
        if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
            onError(result?.debugMessage ?: "Could not open Play purchase flow")
            clearCallbacks()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgeIfNeeded(purchase)
                        if (purchase.products.any { it == PremiumProductIds.MONTHLY || it == PremiumProductIds.ANNUAL }) {
                            writePremiumFlag(true)
                            onPurchaseSuccess?.invoke(purchase.products.firstOrNull() ?: "")
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                onPurchaseError?.invoke("Cancelled")
            }
            else -> {
                onPurchaseError?.invoke(result.debugMessage.ifBlank { "Purchase failed (${result.responseCode})" })
            }
        }
        clearCallbacks()
        syncActivePurchases()
    }

    private fun clearCallbacks() {
        onPurchaseSuccess = null
        onPurchaseError = null
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            billingClient?.acknowledgePurchase(params) { }
        }
    }
}

/** Cached flag written when Play Billing syncs; use for UI (e.g. hide ads) without opening BillingClient. */
fun isPremiumUnlocked(context: Context): Boolean =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_PREMIUM, false)
