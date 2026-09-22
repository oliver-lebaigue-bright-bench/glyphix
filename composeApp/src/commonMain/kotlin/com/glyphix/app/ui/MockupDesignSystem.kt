package com.glyphix.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.glyphix.shared.model.Tab
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Trophy
import kotlin.math.roundToInt

@Composable
fun mockupSurfaceColor(): Color {
    return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
}

@Composable
fun mockupTextColor(): Color {
    return MaterialTheme.colorScheme.onSurface
}

@Composable
fun mockupSubtextColor(): Color {
    return MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun mockupAccentColor(): Color {
    return MaterialTheme.colorScheme.primary
}

val GlyphGreen = Color(0xFFD5FC2D)

@Composable
fun FloatingTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    avatarUrl: String? = null,
    isProfileActive: Boolean = false,
    showBackButton: Boolean = false,
    onTitleLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val contentColor = mockupTextColor()
    val accentColor = mockupAccentColor()

    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val borderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        color = surfaceColor,
        contentColor = contentColor,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Using a common one
                    onMenuClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = GlyphGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTitleLongClick?.invoke()
                        }
                    )
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onProfileClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isProfileActive) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                        contentDescription = "Profile",
                        tint = if (isProfileActive) accentColor else contentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingBottomBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    isRunning: Boolean,
    onToggleVisualizer: () -> Unit,
    isFabMenuExpanded: Boolean,
    onToggleFabMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accentColor = mockupAccentColor()
    val contentColor = mockupTextColor()

    val navItems = listOf(
        getTabItem(Tab.Audio),
        getTabItem(Tab.Leaderboard),
        getTabItem(Tab.Settings),
        getTabItem(Tab.Info)
    )
    val itemCount = navItems.size

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    val itemExtraWidthsPx = remember(navItems) {
        navItems.map { item ->
            val textWidth = textMeasurer.measure(
                text = item.label,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false
            ).size.width.toFloat()
            textWidth + with(density) { 20.dp.toPx() }
        }
    }

    val animActiveIndex = remember { Animatable(navItems.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0).toFloat()) }
    val animExpansion = remember { Animatable(1f) }
    
    var targetIndex by remember { mutableIntStateOf(navItems.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)) }

    LaunchedEffect(selectedTab) {
        val newIndex = navItems.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
        targetIndex = newIndex
        launch {
            animExpansion.animateTo(0.10f, tween(140))
            animExpansion.animateTo(1f, spring(dampingRatio = 0.45f))
        }
        launch {
            animActiveIndex.animateTo(newIndex.toFloat(), spring(dampingRatio = 0.58f))
        }
    }

    val navBgColor = Color(0xFF0D0D0D).copy(alpha = 0.95f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(36.dp)),
            shape = RoundedCornerShape(36.dp),
            color = navBgColor
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val totalWidthPx = constraints.maxWidth.toFloat()
                val targetExtraWidthPx = itemExtraWidthsPx.getOrNull(targetIndex) ?: 0f
                val expansion = animExpansion.value
                val pillWidth = with(density) { (56.dp.toPx() + targetExtraWidthPx * expansion).toDp() }
                
                Box(
                    modifier = Modifier
                        .offset {
                            val slotWidth = totalWidthPx / itemCount
                            val centerX = animActiveIndex.value * slotWidth + slotWidth / 2f
                            IntOffset((centerX - pillWidth.toPx() / 2f).roundToInt(), 0)
                        }
                        .size(pillWidth, 56.dp)
                        .background(accentColor, RoundedCornerShape(28.dp))
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    navItems.forEachIndexed { i, item ->
                        val isSelected = selectedTab == item.tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    if (!isSelected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(item.tab)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = item.icon ?: Icons.Default.MusicNote,
                                    contentDescription = item.label,
                                    tint = if (isSelected) Color.Black else contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                if (isSelected && expansion > 0.3f) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = item.label,
                                        color = Color.Black.copy(alpha = expansion),
                                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.size(72.dp).shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = if (isRunning) MaterialTheme.colorScheme.error else GlyphGreen
        ) {
            Box(
                modifier = Modifier.fillMaxSize().clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isRunning) onToggleVisualizer() else onToggleFabMenu()
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isRunning) Color.White else Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private data class TabItem(
    val tab: Tab,
    val icon: ImageVector? = null,
    val label: String
)

private fun getTabItem(tab: Tab): TabItem = when (tab) {
    Tab.Audio -> TabItem(Tab.Audio, icon = Icons.Default.MusicNote, label = "Audio")
    Tab.Leaderboard -> TabItem(Tab.Leaderboard, icon = FontAwesomeIcons.Solid.Trophy, label = "Leaderboard")
    Tab.Settings -> TabItem(Tab.Settings, icon = Icons.Default.Settings, label = "Settings")
    Tab.Info -> TabItem(Tab.Info, icon = Icons.Default.Info, label = "Info")
    else -> TabItem(tab, label = tab.label)
}

@Composable
fun MockupCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    containerColor: Color = mockupSurfaceColor(),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = shape),
        shape = shape,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}
