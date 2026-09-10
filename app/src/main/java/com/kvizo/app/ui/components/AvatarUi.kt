package com.kvizo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.kvizo.app.R

object AvatarCatalog {
    const val COUNT = 24
    val ALL: IntRange = 1..COUNT

    private val drawables = intArrayOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4,
        R.drawable.avatar_5, R.drawable.avatar_6, R.drawable.avatar_7, R.drawable.avatar_8,
        R.drawable.avatar_9, R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12,
        R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15, R.drawable.avatar_16,
        R.drawable.avatar_17, R.drawable.avatar_18, R.drawable.avatar_19, R.drawable.avatar_20,
        R.drawable.avatar_21, R.drawable.avatar_22, R.drawable.avatar_23, R.drawable.avatar_24,
    )

    fun res(id: Int): Int? = if (id in 1..COUNT) drawables[id - 1] else null
}

@Composable
fun AvatarView(
    id: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val r = AvatarCatalog.res(id)
    if (r != null) {
        Image(
            painter = painterResource(r),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(modifier = modifier.size(size))
    }
}

@Composable
private fun AvatarSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.sp,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun AvatarTile(
    id: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    tileSize: Dp,
    selectedTint: Color,
    borderColor: Color,
) {
    val selScale = remember { Animatable(1f) }
    val isSelected = selected == id
    LaunchedEffect(isSelected) {
        if (isSelected) selScale.animateTo(1.15f, spring(dampingRatio = 0.45f, stiffness = 550f))
        else selScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f))
    }
    Box(
        modifier = Modifier
            .size(tileSize)
            .graphicsLayer { scaleX = selScale.value; scaleY = selScale.value }
            .clip(CircleShape)
            .background(
                if (isSelected) selectedTint.copy(alpha = 0.18f) else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                CircleShape,
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = if (isSelected) selectedTint else borderColor.copy(alpha = 0.25f),
                shape = CircleShape,
            )
            .clip(CircleShape)
            .clickable { onSelect(id) },
        contentAlignment = Alignment.Center,
    ) {
        AvatarView(
            id = id,
            size = tileSize * 0.82f,
            modifier = Modifier.clip(CircleShape),
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(selectedTint, CircleShape)
                    .border(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
fun AvatarPickerGrid(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 48.dp,
    selectedTint: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    borderColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AvatarSectionLabel("Avatar")
        AvatarCatalog.ALL.toList().chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val spacing = 12.dp
                row.forEachIndexed { idx, id ->
                    if (idx > 0) Box(modifier = Modifier.size(spacing))
                    AvatarTile(id, selected, onSelect, tileSize, selectedTint, borderColor)
                }
            }
        }
    }
}
