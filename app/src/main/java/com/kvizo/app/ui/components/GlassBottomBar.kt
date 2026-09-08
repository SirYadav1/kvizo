package com.kvizo.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kvizo.app.ui.theme.Indigo

/** A single item shown in the glass bottom bar. */
data class GlassBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * AirBeats-style floating glass bottom navigation bar.
 *
 * - Floating pill container: translucent white ("glass") with a soft lavender
 *   hairline border and gentle shadow — sits above the content with margins.
 * - Sliding lavender pill indicator that springs to the selected item's
 *   position/width (same interaction as AirBeats' classic toolbar).
 * - Selected item tinted with the pastel lavender primary.
 */
@Composable
fun GlassBottomBar(
    items: List<GlassBarItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemWidths = remember { mutableStateMapOf<Int, Dp>() }
    val itemPositions = remember { mutableStateMapOf<Int, Dp>() }

    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val targetWidth = itemWidths[selectedIndex] ?: 0.dp
    val targetPosition = itemPositions[selectedIndex] ?: 0.dp

    val pillWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "glassPillWidth"
    )
    val pillOffset by animateDpAsState(
        targetValue = targetPosition,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "glassPillOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val glassColor = if (isDark) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.82f)
        }
        val glassBorder = if (isDark) {
            Indigo.copy(alpha = 0.45f)
        } else {
            Indigo.copy(alpha = 0.28f)
        }
        val shadowTint = if (isDark) Color.Black.copy(alpha = 0.45f) else Indigo.copy(alpha = 0.35f)
        // glass pill container
        Box(
            modifier = Modifier
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp), ambientColor = shadowTint, spotColor = shadowTint)
                .background(
                    color = glassColor,
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = glassBorder,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Box(modifier = Modifier.height(IntrinsicSize.Min)) {
                // sliding lavender indicator pill
                if (targetWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = pillOffset)
                            .width(pillWidth)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(22.dp)
                            )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    items.forEachIndexed { index, item ->
                        val selected = item.route == selectedRoute
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    itemWidths[index] = with(density) { coordinates.size.width.toDp() }
                                    itemPositions[index] = with(density) { coordinates.positionInParent().x.toDp() }
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    role = Role.Tab
                                ) { onSelect(item.route) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp)
                            )
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
