package com.shohankhan.bokeya.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.ui.theme.Durations
import com.shohankhan.bokeya.ui.theme.Radius
import com.shohankhan.bokeya.ui.theme.Space
import com.shohankhan.bokeya.ui.theme.bokeya
import kotlin.math.max

/**
 * Charts here are deliberately minimal and always derived from real data. Each one exposes a
 * content description so the visual is never the only channel carrying the information.
 */

// ---------------------------------------------------------------- linear progress

@Composable
fun BokeyaProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color? = null,
    track: Color? = null,
    animate: Boolean = true,
    label: String? = null,
) {
    val target = progress.coerceIn(0f, 1f)
    val value by animateFloatAsState(
        targetValue = if (animate) target else target,
        animationSpec = tween(Durations.deliberate, easing = LinearOutSlowInEasing),
        label = "progress",
    )
    val bar = color ?: MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .semantics {
                contentDescription = label
                    ?: (BanglaNumbers.toBanglaDigits((target * 100).toInt().toString()) + " শতাংশ")
            },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(value)
                .clip(RoundedCornerShape(height / 2))
                .background(bar),
        )
    }
}

// ---------------------------------------------------------------- stacked distribution

data class Slice(val label: String, val value: Long, val color: Color)

/**
 * Single-bar stacked distribution. Used for the hero's category split — it communicates
 * proportion in one glance and uses the full available width, unlike a row of pills.
 */
@Composable
fun StackedBar(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    trackColor: Color? = null,
    gap: Dp = 2.dp,
) {
    val total = slices.sumOf { max(it.value, 0L) }
    val track = trackColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    if (total <= 0L) {
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(track),
        )
        return
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        slices.filter { it.value > 0L }.forEach { slice ->
            val fraction by animateFloatAsState(
                targetValue = slice.value.toFloat() / total.toFloat(),
                animationSpec = tween(Durations.deliberate, easing = LinearOutSlowInEasing),
                label = "slice_${slice.label}",
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(max(fraction, 0.0001f))
                    .clip(RoundedCornerShape(height / 2))
                    .background(slice.color),
            )
        }
    }
}

/** Legend entry: dot + label + value, aligned in a grid by the caller. */
@Composable
fun LegendItem(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color? = null,
    valueColor: Color? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(Space.sm))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor ?: MaterialTheme.bokeya.faint,
                maxLines = 1,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------- ring

/**
 * Progress ring with an optional caption in the middle. Used in the hero for "how much of
 * everything is already settled" — a shape that reads instantly at a glance.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    stroke: Dp = 8.dp,
    color: Color? = null,
    trackColor: Color? = null,
    caption: String? = null,
    captionColor: Color? = null,
    subCaption: String? = null,
) {
    val target = progress.coerceIn(0f, 1f)
    val value by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(Durations.deliberate, easing = LinearOutSlowInEasing),
        label = "ring",
    )
    val arcColor = color ?: MaterialTheme.colorScheme.primary
    val track = trackColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier
            .size(size)
            .semantics {
                contentDescription =
                    BanglaNumbers.toBanglaDigits((target * 100).toInt().toString()) + " শতাংশ সম্পন্ন"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            val inset = width / 2
            val arcSize = Size(this.size.width - width, this.size.height - width)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
            if (value > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
            }
        }
        if (caption != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    caption,
                    style = MaterialTheme.typography.titleMedium,
                    color = captionColor ?: MaterialTheme.colorScheme.onSurface,
                )
                if (subCaption != null) {
                    Text(
                        subCaption,
                        style = MaterialTheme.typography.labelSmall,
                        color = (captionColor ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- sparkline

/**
 * Compact trend line. Values are plotted as given; no smoothing tricks that would misrepresent
 * the data. Renders nothing meaningful (a flat baseline) when there is not enough history.
 */
@Composable
fun Sparkline(
    values: List<Long>,
    modifier: Modifier = Modifier,
    color: Color? = null,
    fill: Boolean = true,
    strokeWidth: Dp = 2.dp,
    contentDescription: String? = null,
) {
    val line = color ?: MaterialTheme.colorScheme.primary
    Canvas(
        modifier.clearAndSetSemantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
    ) {
        if (values.size < 2) return@Canvas
        val minValue = values.min()
        val maxValue = values.max()
        val range = (maxValue - minValue).coerceAtLeast(1L).toFloat()
        val stepX = size.width / (values.size - 1).toFloat()
        val pad = strokeWidth.toPx()
        val usableHeight = size.height - pad * 2

        fun pointAt(index: Int): Offset {
            val normalized = (values[index] - minValue).toFloat() / range
            return Offset(stepX * index, pad + usableHeight * (1f - normalized))
        }

        val path = Path().apply {
            moveTo(pointAt(0).x, pointAt(0).y)
            for (i in 1 until values.size) {
                val previous = pointAt(i - 1)
                val current = pointAt(i)
                val midX = (previous.x + current.x) / 2f
                cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
            }
        }

        if (fill) {
            val area = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                area,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(line.copy(alpha = 0.22f), Color.Transparent),
                ),
            )
        }
        drawPath(path, color = line, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
    }
}

// ---------------------------------------------------------------- comparison bars

/**
 * Two-value comparison (income vs expense). Bars share a scale so their lengths are honestly
 * comparable to each other.
 */
@Composable
fun ComparisonBars(
    leftLabel: String,
    leftValue: Long,
    leftColor: Color,
    rightLabel: String,
    rightValue: Long,
    rightColor: Color,
    leftText: String,
    rightText: String,
    modifier: Modifier = Modifier,
) {
    val scale = max(max(leftValue, rightValue), 1L).toFloat()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.md)) {
        listOf(
            Triple(leftLabel, leftValue to leftColor, leftText),
            Triple(rightLabel, rightValue to rightColor, rightText),
        ).forEach { (label, data, text) ->
            val (value, color) = data
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.bokeya.muted,
                    )
                    Text(
                        text,
                        style = MaterialTheme.typography.titleSmall,
                        color = color,
                    )
                }
                Spacer(Modifier.height(Space.xs + 2.dp))
                BokeyaProgress(
                    progress = value / scale,
                    height = 7.dp,
                    color = color,
                    label = "$label $text",
                )
            }
        }
    }
}

/** Horizontal ranked bar used in category breakdowns. */
@Composable
fun RankedBar(
    label: String,
    valueText: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Space.sm)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(Space.xs + 2.dp))
        BokeyaProgress(fraction, height = 6.dp, color = color, label = "$label $valueText")
        if (caption != null) {
            Spacer(Modifier.height(Space.xs))
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.bokeya.faint)
        }
    }
}
