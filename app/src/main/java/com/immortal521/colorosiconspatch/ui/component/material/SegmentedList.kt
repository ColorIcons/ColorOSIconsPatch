package com.immortal521.colorosiconspatch.ui.component.material

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        content.forEachIndexed { index, item ->
            CompositionLocalProvider(
                LocalListItemShapes provides ListItemDefaults.segmentedShapes(index, content.size)
            ) {
                item()
            }
        }
    }
}

@Composable
fun ExpressiveToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier,
    colors: ToggleButtonColors = ToggleButtonDefaults.colors(
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        modifier = modifier,
        colors = colors,
        content = content,
    )
}

@Composable
fun SegmentedDropdownItem(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(IntOffset.Zero) }
    val safeIndex = selectedIndex.takeIf { items.isNotEmpty() }?.coerceIn(items.indices) ?: -1
    val shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)
    val density = LocalDensity.current

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pressOffset = IntOffset(down.position.x.toInt(), down.position.y.toInt())
            }
        }
    ) {
        SegmentedListItem(
            onClick = { expanded = true },
            shapes = shapes,
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
                supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            leadingContent = icon?.let { { Icon(it, title) } },
            content = { Text(title) },
            supportingContent = summary?.let { { Text(it) } },
            trailingContent = {
                Text(
                    text = items.getOrNull(safeIndex).orEmpty(),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(0.3f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = with(density) { DpOffset(pressOffset.x.toDp(), pressOffset.y.toDp()) },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            item,
                            color = if (index == safeIndex) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        onItemSelected(index)
                        expanded = false
                    },
                    modifier = if (index == safeIndex) {
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    } else Modifier,
                    trailingIcon = if (index == safeIndex) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                            )
                        }
                    } else null,
                    contentPadding = MenuDefaults.DropdownMenuItemContentPadding
                )
            }
        }
    }
}
