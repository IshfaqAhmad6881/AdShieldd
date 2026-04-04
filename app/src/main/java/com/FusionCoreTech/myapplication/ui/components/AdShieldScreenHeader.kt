package com.FusionCoreTech.myapplication.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Common spacing so [ThemeBackButton] aligns across Settings, Preferences, Advanced, etc.
 */
object AdShieldScreenInsets {
    val headerHorizontal = 20.dp
    val belowStatusBar = 12.dp
    val backToTitleSpacing = 12.dp
}

/**
 * Single top bar row: back control first, optional [middle] (usually a weighted title column),
 * optional [trailing] (e.g. Advanced status pill or Speed Test title balance spacer).
 */
@Composable
fun AdShieldTopBarRow(
    isDarkMode: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    middle: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeBackButton(isDarkMode = isDarkMode, onClick = onBackClick)
        middle()
        trailing?.invoke(this)
    }
}
