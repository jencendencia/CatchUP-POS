package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.theme.*

data class QuickActionData(
    val label: String,
    val icon: ImageVector,
    val contentColor: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit = {}
)

@Composable
fun QuickActionsSection(
    onNewOrderClick: () -> Unit = {},
    onViewOrdersClick: () -> Unit = {},
    onProductsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickActionData(
            label = "New Order",
            icon = Icons.Default.AddCircle,
            contentColor = OrangeAccent,
            onClick = onNewOrderClick
        ),
        QuickActionData(
            label = "View Orders",
            icon = Icons.Default.Assignment,
            contentColor = MutedRed,
            onClick = onViewOrdersClick
        ),
        QuickActionData(
            label = "Products",
            icon = Icons.Default.Coffee,
            contentColor = OrangeAccent,
            onClick = onProductsClick
        ),
        QuickActionData(
            label = "Reports",
            icon = Icons.Default.BarChart,
            contentColor = MutedRed,
            onClick = onReportsClick
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    ) {
        // Section Title
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            actions.forEach { action ->
                QuickActionButton(
                    data = action,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    data: QuickActionData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { data.onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D0D0D)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = data.label,
                tint = data.contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = data.label,
                style = MaterialTheme.typography.labelLarge,
                color = data.contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
