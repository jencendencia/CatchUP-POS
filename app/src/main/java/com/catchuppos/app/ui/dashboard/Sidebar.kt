package com.catchuppos.app.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.R
import com.catchuppos.app.theme.*

enum class NavItem(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Speed, Icons.Outlined.Speed),
    ORDERS("Orders", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    PRODUCTS("Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    TRANSACTIONS("Transactions", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz),
    REPORTS("Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    CUSTOMERS("Customers", Icons.Filled.People, Icons.Outlined.People),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun Sidebar(
    activeItem: NavItem = NavItem.DASHBOARD,
    onNavItemClick: (NavItem) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var isCollapsed by remember { mutableStateOf(false) }
    val sidebarWidth = if (isCollapsed) 72.dp else 240.dp

    Box(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(Color.Black)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            if (!isCollapsed) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "CatchUP POS Logo",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "CatchUP POS Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Collapse/Expand toggle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .clickable { isCollapsed = !isCollapsed },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            HorizontalDivider(
                color = DarkBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = if (isCollapsed) 8.dp else 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isCollapsed) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                NavItem.entries.forEach { item ->
                    val isActive = item == activeItem
                    NavItemRow(
                        item = item,
                        isActive = isActive,
                        isCollapsed = isCollapsed,
                        onClick = { onNavItemClick(item) }
                    )
                }
            }

            // Divider before logout
            HorizontalDivider(
                color = DarkBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = if (isCollapsed) 8.dp else 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Logout Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCollapsed) 8.dp else 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onLogout() }
                    .padding(
                        horizontal = if (isCollapsed) 0.dp else 16.dp,
                        vertical = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = "Log Out",
                    tint = MutedRed,
                    modifier = Modifier.size(20.dp)
                )
                if (!isCollapsed) {
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NavItemRow(
    item: NavItem,
    isActive: Boolean,
    isCollapsed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isActive) Modifier.background(NavActiveOrange.copy(alpha = 0.12f))
                else Modifier
            )
            .clickable { onClick() }
            .padding(start = if (isActive) 3.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.Start
    ) {
        // Active indicator bar
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(
                        OrangeAccent,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isActive) 13.dp else if (isCollapsed) 0.dp else 16.dp,
                    end = if (isCollapsed) 0.dp else 16.dp,
                    top = 13.dp,
                    bottom = 13.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isActive) item.activeIcon else item.inactiveIcon,
                contentDescription = item.label,
                tint = if (isActive) OrangeAccent else NavInactiveText,
                modifier = Modifier.size(20.dp)
            )
            if (!isCollapsed) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) OrangeAccent else NavInactiveText,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
