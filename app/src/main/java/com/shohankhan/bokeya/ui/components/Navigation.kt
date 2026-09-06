package com.shohankhan.bokeya.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.ui.theme.Durations
import com.shohankhan.bokeya.ui.theme.IconSize
import com.shohankhan.bokeya.ui.theme.Radius
import com.shohankhan.bokeya.ui.theme.Space
import com.shohankhan.bokeya.ui.theme.bokeya

data class DockItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Bottom dock.
 *
 * The add button is *inside* the bar rather than a detached FAB hovering over content — that
 * detachment was one of the things that made the old shell feel unresolved. Selection is shown
 * by a small indicator bar plus weight/colour change, never by an oversized pill.
 */
@Composable
fun BokeyaDock(
    items: List<DockItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = MaterialTheme.bokeya
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = extras.surface2,
        shadowElevation = 0.dp,
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(extras.divider),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Space.sm, vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // First half of the tabs, the add button, then the rest — a balanced split.
                val split = (items.size + 1) / 2
                items.take(split).forEach { item ->
                    DockTab(item, item.route == currentRoute, Modifier.weight(1f)) {
                        onSelect(item.route)
                    }
                }

                AddButton(onAdd)

                items.drop(split).forEach { item ->
                    DockTab(item, item.route == currentRoute, Modifier.weight(1f)) {
                        onSelect(item.route)
                    }
                }
            }
        }
    }
}

@Composable
private fun DockTab(
    item: DockItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val extras = MaterialTheme.bokeya
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else extras.faint,
        tween(Durations.fast),
        label = "dockTint",
    )
    val indicator by animateDpAsState(
        if (selected) 16.dp else 0.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dockIndicator",
    )

    Column(
        modifier
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = Space.sm)
            .semantics {
                this.selected = selected
                role = Role.Tab
                contentDescription = item.label
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(IconSize.lg))
        Spacer(Modifier.height(Space.xs + 1.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
        Spacer(Modifier.height(Space.xs))
        Box(
            Modifier
                .width(indicator)
                .height(2.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(1f, label = "addScale")
    Box(
        Modifier
            .padding(horizontal = Space.sm)
            .size(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "নতুন হিসাব যোগ করুন"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(IconSize.lg),
        )
    }
}

// ---------------------------------------------------------------- quick add sheet

data class QuickAddAction(
    val key: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val tone: QuickAddTone,
)

enum class QuickAddTone { NEUTRAL, OUTGOING, INCOMING, SETTLE }

/**
 * Content for the quick-add bottom sheet. Grouped by intent — obligations, cash flow, settle —
 * so the seven actions read as three short decisions instead of one long menu.
 */
@Composable
fun QuickAddSheet(
    actions: List<QuickAddAction>,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = MaterialTheme.bokeya
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter)
            .padding(bottom = Space.xxl),
    ) {
        Text(
            "নতুন যোগ করুন",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            "কী যোগ করতে চান বেছে নিন",
            style = MaterialTheme.typography.bodySmall,
            color = extras.faint,
        )
        Spacer(Modifier.height(Space.xl))

        val groups = listOf(
            "বকেয়া ও ধার" to actions.filter { it.tone == QuickAddTone.NEUTRAL },
            "আয় ও খরচ" to actions.filter {
                it.tone == QuickAddTone.INCOMING || it.tone == QuickAddTone.OUTGOING
            },
            "পরিশোধ" to actions.filter { it.tone == QuickAddTone.SETTLE },
        ).filter { it.second.isNotEmpty() }

        groups.forEachIndexed { index, (title, group) ->
            Eyebrow(title)
            Spacer(Modifier.height(Space.sm))
            BokeyaGroup {
                group.forEachIndexed { i, action ->
                    val tint = when (action.tone) {
                        QuickAddTone.INCOMING -> extras.moneyIn
                        QuickAddTone.OUTGOING -> extras.moneyOut
                        QuickAddTone.SETTLE -> MaterialTheme.colorScheme.primary
                        QuickAddTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
                    }
                    BokeyaRow(
                        title = action.label,
                        subtitle = action.description,
                        onClick = { onAction(action.key) },
                        leading = { IconBadge(action.icon, tint, size = 40.dp) },
                    )
                    if (i != group.lastIndex) RowDivider(inset = 68.dp)
                }
            }
            if (index != groups.lastIndex) Spacer(Modifier.height(Space.lg))
        }
    }
}
