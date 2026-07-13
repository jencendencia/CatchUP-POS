package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.theme.*

data class KPICardData(
    val icon: ImageVector,
    val iconBgColor: androidx.compose.ui.graphics.Color,
    val metric: String,
    val label: String,
    val footer: String,
    val footerColor: androidx.compose.ui.graphics.Color,
    val iconTint: androidx.compose.ui.graphics.Color = TextWhite,
    val isClickable: Boolean = false
)

@Composable
fun KPICardsGrid(
    totalOrders: Int = 0,
    drinksSold: Int = 0,
    todaySales: Double = 0.0,
    totalDrinksAvailable: Int = 0,
    hotCupsAvailable: Int = 0,
    coldCupsAvailable: Int = 0,
    onCupsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cupsAvailable = hotCupsAvailable + coldCupsAvailable
    val cards = listOf(
        KPICardData(
            icon = Icons.Default.Receipt,
            iconBgColor = IconBgOrange,
            metric = "$totalOrders",
            label = "Number of Orders",
            footer = "Today's Sales: ₱${String.format("%,.2f", todaySales)}",
            footerColor = OrangeMuted
        ),
        KPICardData(
            icon = Icons.Default.Coffee,
            iconBgColor = IconBgRed,
            metric = "$drinksSold",
            label = "Drinks Sold",
            footer = "Items ($totalDrinksAvailable drinks available)",
            footerColor = MutedRed
        ),
        KPICardData(
            icon = Icons.Default.TrendingUp,
            iconBgColor = IconBgAmber,
            metric = "₱${String.format("%,.2f", todaySales)}",
            label = "Today's Sales",
            footer = if (todaySales > 0) "Sales today" else "No orders yet today",
            footerColor = OrangeMuted
        ),
        KPICardData(
            icon = Icons.Default.Coffee,
            iconBgColor = Color(0xFF2196F3),
            metric = "$cupsAvailable",
            label = "Cups Available",
            footer = "Hot: $hotCupsAvailable | Cold: $coldCupsAvailable",
            footerColor = Color(0xFF2196F3),
            isClickable = true
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        cards.forEachIndexed { index, card ->
            KPIStatCard(
                data = card,
                modifier = Modifier.weight(1f),
                onClick = if (card.isClickable) onCupsClick else null
            )
        }
    }
}

@Composable
private fun KPIStatCard(
    data: KPICardData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: icon + metric
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = data.iconBgColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = data.icon,
                            contentDescription = data.label,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Metric
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = data.metric,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = data.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }

            // Footer
            Text(
                text = data.footer,
                style = MaterialTheme.typography.bodySmall,
                color = data.footerColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
