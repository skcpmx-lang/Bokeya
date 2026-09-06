package com.shohankhan.bokeya.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.domain.AccountStatus
import com.shohankhan.bokeya.ui.theme.Durations
import com.shohankhan.bokeya.ui.theme.IconSize
import com.shohankhan.bokeya.ui.theme.LocalBanglaDigits
import com.shohankhan.bokeya.ui.theme.Radius
import com.shohankhan.bokeya.ui.theme.Space
import com.shohankhan.bokeya.ui.theme.bokeya
import com.shohankhan.bokeya.ui.theme.moneyStyle
import kotlin.math.roundToLong

// ---------------------------------------------------------------- money

/**
 * The single place an amount is rendered. Numerals get tightened tracking via [moneyStyle], and
 * the accessibility label always reads the true value even while the counter is animating.
 */
@Composable
fun MoneyText(
    money: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    animate: Boolean = false,
    signed: Boolean = false,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
) {
    val bangla = LocalBanglaDigits.current
    val animated by animateFloatAsState(
        targetValue = money.poisha.toFloat(),
        animationSpec = tween(
            durationMillis = if (animate) Durations.deliberate else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "money",
    )
    val shown = Money(if (animate) animated.roundToLong() else money.poisha)
    val text = if (signed) {
        CurrencyFormatter.formatSigned(shown, bangla)
    } else {
        CurrencyFormatter.format(shown, banglaDigits = bangla)
    }
    val resolved = moneyStyle(style)
    Text(
        text = text,
        modifier = modifier.semantics {
            contentDescription = CurrencyFormatter.format(money, banglaDigits = false) + " টাকা"
        },
        style = resolved,
        color = color,
        fontWeight = fontWeight ?: resolved.fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

// ---------------------------------------------------------------- status

/**
 * Status is communicated by dot + text + tone together, never colour alone, so it survives
 * colour-blindness and greyscale.
 */
@Composable
fun StatusBadge(status: AccountStatus, modifier: Modifier = Modifier, compact: Boolean = false) {
    val extras = MaterialTheme.bokeya
    val (bg, fg) = when (status) {
        AccountStatus.PAID -> extras.successContainer to extras.success
        AccountStatus.OVERDUE -> extras.dangerContainer to extras.danger
        AccountStatus.DUE_TODAY, AccountStatus.DUE_SOON -> extras.warningContainer to extras.warning
        AccountStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.secondaryContainer to
            MaterialTheme.colorScheme.onSecondaryContainer
        AccountStatus.PAUSED, AccountStatus.ARCHIVED ->
            MaterialTheme.colorScheme.surfaceVariant to extras.muted
        AccountStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(modifier = modifier, color = bg, shape = RoundedCornerShape(Radius.xs)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (compact) Space.sm else 10.dp,
                vertical = if (compact) 3.dp else 5.dp,
            ),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Spacer(Modifier.width(6.dp))
            Text(status.label, style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}

// ---------------------------------------------------------------- actions

@Composable
fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .clickable(onClick = onClick)
            .padding(vertical = Space.sm, horizontal = Space.xs)
            .sizeIn(minWidth = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(IconSize.md))
        }
        Spacer(Modifier.height(Space.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.bokeya.muted,
            maxLines = 1,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.md),
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = Space.xxl),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(IconSize.sm))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.md),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(IconSize.sm))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Low-emphasis text action. */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = color ?: MaterialTheme.colorScheme.primary,
        )
    }
}

// ---------------------------------------------------------------- empty state

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.xxl, vertical = Space.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(Radius.xl))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(Space.lg))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.xs + 2.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.bokeya.muted,
            textAlign = TextAlign.Center,
        )
        if (ctaLabel != null && onCta != null) {
            Spacer(Modifier.height(Space.xl))
            PrimaryButton(text = ctaLabel, onClick = onCta)
        }
    }
}

// ---------------------------------------------------------------- loading

@Composable
fun Skeleton(modifier: Modifier = Modifier, height: Dp = 20.dp, corner: Dp = Radius.xs) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
    )
}

/** Skeleton shaped like the hero, so the first frame does not jump. */
@Composable
fun SkeletonHero(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(232.dp)
            .clip(RoundedCornerShape(Radius.hero))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    )
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    BokeyaCard(modifier) {
        Skeleton(Modifier.fillMaxWidth(0.35f), 12.dp)
        Spacer(Modifier.height(Space.md))
        Skeleton(Modifier.fillMaxWidth(0.65f), 26.dp)
        Spacer(Modifier.height(Space.md))
        Skeleton(Modifier.fillMaxWidth(), 8.dp)
    }
}

// ---------------------------------------------------------------- chips

@Composable
fun FilterChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = Space.gutter,
) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                tween(Durations.fast),
                label = "chipbg",
            )
            val fg by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.bokeya.muted,
                tween(Durations.fast),
                label = "chipfg",
            )
            Surface(
                color = bg,
                shape = RoundedCornerShape(Radius.pill),
                border = if (selected) null else BorderStroke(1.dp, MaterialTheme.bokeya.divider),
                modifier = Modifier.clickable { onSelect(index) },
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = Space.lg, vertical = Space.sm + 1.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = fg,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- success

/** Restrained success mark: ring draws, check scales once. No confetti storm. */
@Composable
fun SuccessCheck(modifier: Modifier = Modifier, size: Dp = 76.dp) {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(Durations.slow, easing = FastOutSlowInEasing))
    }
    val color = MaterialTheme.bokeya.success
    Box(
        modifier
            .size(size)
            .scale(scale.value)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

// ---------------------------------------------------------------- misc

/** Kept for compatibility; new code should use BokeyaCanvas. */
@Composable
fun HeroGradient(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BokeyaCanvas(modifier = modifier, content = content)
}

@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.sm))
                .clickable(onClick = onToggle)
                .padding(vertical = Space.md, horizontal = Space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "rot")
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                Modifier
                    .size(IconSize.md)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(Durations.fast)) + expandVertically(tween(Durations.standard)),
            exit = fadeOut(tween(Durations.instant)) + shrinkVertically(tween(Durations.fast)),
        ) {
            Column { content() }
        }
    }
}

@Composable
fun rememberInteraction() = remember { MutableInteractionSource() }
