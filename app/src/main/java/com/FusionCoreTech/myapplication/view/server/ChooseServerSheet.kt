package com.FusionCoreTech.myapplication.view.server

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.model.Location
import com.FusionCoreTech.myapplication.ui.theme.PremiumOrange

/** Ad-blocking DNS servers (Open DNS and others that block ads). */
val DNS_SERVER_OPTIONS: List<Location> = listOf(
    Location(name = "Open DNS"),
    Location(name = "AdGuard DNS"),
    Location(name = "Cloudflare DNS"),
    Location(name = "Quad9"),
    Location(name = "NextDNS"),
    Location(name = "CleanBrowsing"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseServerSheet(
    selectedLocation: Location,
    isDarkMode: Boolean,
    onSelect: (Location) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetBackground = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val contentBg = if (isDarkMode) Color(0xFF2C2C2C) else Color.White
    val textDark = if (isDarkMode) Color.White else Color.Black
    val textLight = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF777777)
    val selectedBg = PremiumOrange

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(textLight.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(contentBg, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 0.dp)
        ) {
            Text(
                text = "Choose server",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(bottom = 40.dp, top = 4.dp)
            ) {
                items(DNS_SERVER_OPTIONS) { dns ->
                    val isSelected = dns.name == selectedLocation.name
                    val cardElevation = if (isSelected) 4.dp else 6.dp
                    val cardShape = RoundedCornerShape(14.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = cardElevation,
                                shape = cardShape,
                                spotColor = Color.Black.copy(alpha = 0.15f),
                                ambientColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .clickable { onSelect(dns) },
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) selectedBg else contentBg
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        ),
                        border = if (!isSelected) {
                            BorderStroke(
                                0.5.dp,
                                if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                            )
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dns.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else textDark
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
