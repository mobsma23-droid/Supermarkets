package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen

@Composable
fun LiquidNavigationBar(
    screens: List<Screen>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    badgeCounts: Map<Screen, Int> = emptyMap(),
    titleProvider: (Screen) -> String = { it.title }
) {
    val colorScheme = MaterialTheme.colorScheme

    // Determine whether current background is dark or light
    val isDarkTheme = colorScheme.background.red < 0.5f && colorScheme.background.green < 0.5f && colorScheme.background.blue < 0.5f

    // Dynamic colors responsive to the active theme & primary palette
    val barBgColor = if (isDarkTheme) {
        colorScheme.surface
    } else {
        colorScheme.primary
    }

    val indicatorBgColor = if (isDarkTheme) {
        colorScheme.primary
    } else {
        colorScheme.surface
    }

    val activeContentColor = if (isDarkTheme) {
        colorScheme.onPrimary
    } else {
        colorScheme.primary
    }

    val inactiveContentColor = if (isDarkTheme) {
        colorScheme.onSurface.copy(alpha = 0.65f)
    } else {
        colorScheme.onPrimary.copy(alpha = 0.8f)
    }

    val badgeBgColor = if (isDarkTheme) {
        colorScheme.tertiary
    } else {
        colorScheme.tertiary
    }

    val badgeTextColor = Color.White

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val screenWidth = maxWidth
        val itemWidth = screenWidth / screens.size

        // Main Bar Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = barBgColor,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        // Indicator and Icons Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            screens.forEachIndexed { index, screen ->
                val isSelected = index == selectedIndex
                val badgeCount = badgeCounts[screen] ?: 0
                val screenDisplayTitle = titleProvider(screen)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(itemWidth, 72.dp)
                        .clickable { onItemSelected(index) }
                ) {
                    if (isSelected) {
                        // Floating Active Indicator (Bubble)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .offset(y = (-10).dp)
                                .shadow(6.dp, CircleShape)
                                .background(indicatorBgColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screenDisplayTitle,
                                        tint = activeContentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (badgeCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = badgeBgColor,
                                            modifier = Modifier
                                                .offset(x = 8.dp, y = (-4).dp)
                                                .size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (badgeCount > 99) "99+" else "$badgeCount",
                                                    color = badgeTextColor,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = screenDisplayTitle,
                                    color = activeContentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Inactive Icon with optional badge
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screenDisplayTitle,
                                tint = inactiveContentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            if (badgeCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = badgeBgColor,
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = (-4).dp)
                                        .size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (badgeCount > 99) "99+" else "$badgeCount",
                                            color = badgeTextColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

