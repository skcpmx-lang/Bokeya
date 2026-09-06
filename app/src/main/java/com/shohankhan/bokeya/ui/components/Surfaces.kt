package com.shohankhan.bokeya.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.ui.theme.Durations
import com.shohankhan.bokeya.ui.theme.Radius
import com.shohankhan.bokeya.ui.theme.Space
import com.shohankhan.bokeya.ui.theme.bokeya

/**
 * Surface vocabulary. Deliberately four options, not one card component used everywhere:
 *
 *  - [BokeyaSection]  flat, no container — the default for most content
 *  - [BokeyaGroup]    tier-1 tonal surface for grouping related rows
 *  - [BokeyaCard]     tier-2 card, only when a block must be separable/tappable
 *  - [BokeyaCanvas]   the brand hero surface
 */

// ---------------------------------------------------------------- section (flat)

/**
 * A titled block with NO container. This is the workhorse: content sits directly on the page,
 * which is what stops the UI from looking like a wall of identical cards.
 */
@Composable
fun BokeyaSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    spacing: Dp = Space.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            SectionHeader(title, subtitle, actionLabel, onAction)
            Spacer(Modifier.height(spacing))
        }
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.bokeya.faint,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xs))
                    .clickable(onClick = onAction)
                    .padding(horizontal = Space.sm, vertical = Space.xs),
            )
        }
    }
}

/** Small uppercase-ish eyebrow label for dense areas. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.bokeya.faint,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------- group (tier 1)

/**
 * Tier-1 tonal surface. Use to bind a set of related rows together — a settings block, a list of
 * transactions — without the visual weight of a card.
 */
@Composable
fun BokeyaGroup(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Space.xs),
    shape: Shape = RoundedCornerShape(Radius.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.bokeya.surface1,
        border = BorderStroke(1.dp, MaterialTheme.bokeya.hairline),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Hairline separator sized for inset list rows. */
@Composable
fun RowDivider(modifier: Modifier = Modifier, inset: Dp = Space.lg) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(1.dp)
            .background(MaterialTheme.bokeya.divider),
    )
}

// ---------------------------------------------------------------- card (tier 2)

@Composable
fun BokeyaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Space.lg,
    shape: Shape = RoundedCornerShape(Radius.lg),
    color: Color? = null,
    bordered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = tween(Durations.fast),
        label = "cardPress",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = color ?: MaterialTheme.bokeya.surface2,
        border = if (bordered) BorderStroke(1.dp, MaterialTheme.bokeya.hairline) else null,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Tinted card for status-bearing blocks (overdue, success, info). Color carries an icon too. */
@Composable
fun BokeyaToneCard(
    tone: Color,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Space.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(Radius.lg),
        color = container,
        border = BorderStroke(1.dp, tone.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

// ---------------------------------------------------------------- canvas (hero)

/**
 * The brand surface. A soft two-stop wash plus a very subtle top-light inset — no hard gradient
 * banding, no neon. Content is laid out by the caller so each hero can have its own composition.
 */
@Composable
fun BokeyaCanvas(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.hero),
    contentPadding: PaddingValues = PaddingValues(Space.xl),
    content: @Composable ColumnScope.() -> Unit,
) {
    val extras = MaterialTheme.bokeya
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    0f to extras.heroStart,
                    0.55f to extras.heroEnd,
                    1f to extras.heroStart,
                ),
            ),
    ) {
        // Faint top-edge light so the surface reads as a physical panel, not a flat fill.
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(extras.heroInset, Color.Transparent),
                    ),
                ),
        )
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Inset panel used *inside* the canvas to nest secondary info. */
@Composable
fun CanvasInset(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Space.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.bokeya.heroInset,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

// ---------------------------------------------------------------- list rows

/**
 * Canonical list row: leading slot, title/subtitle, trailing slot. Every list in the app uses
 * this so density and alignment never drift between screens.
 */
@Composable
fun BokeyaRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    minHeight: Dp = 56.dp,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = minHeight)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.bokeya.muted,
                    maxLines = 1,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.sm))
            trailing()
        }
    }
}

/** Circular tonal icon container used as the leading slot in rows. */
@Composable
fun IconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    container: Color? = null,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2.6f))
            .background(container ?: tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

/** Label/value line for detail blocks. */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    emphasize: Boolean = false,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = Space.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.bokeya.muted,
        )
        Text(
            value,
            style = if (emphasize) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}
