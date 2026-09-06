package com.shohankhan.bokeya.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shohankhan.bokeya.data.repo.AccentColor
import com.shohankhan.bokeya.data.repo.ThemeMode

// A restrained palette: deep indigo-teal primary, warm neutral surfaces, semantic accents.
private val IndigoPrimary = Color(0xFF1F4B63)
private val IndigoPrimaryDark = Color(0xFF7FC4E8)
private val BluePrimary = Color(0xFF1D4ED8)
private val BluePrimaryDark = Color(0xFF93B4FF)
private val GreenPrimary = Color(0xFF11624A)
private val GreenPrimaryDark = Color(0xFF6FD9B4)
private val PurplePrimary = Color(0xFF4C1D95)
private val PurplePrimaryDark = Color(0xFFC4A9FF)

@Immutable
data class BokeyaColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val moneyIn: Color,
    val moneyOut: Color,
    val muted: Color,
    val elevatedSurface: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val divider: Color,
)

private val LightExtras = BokeyaColors(
    success = Color(0xFF0F7A54),
    onSuccess = Color.White,
    successContainer = Color(0xFFD8F3E7),
    warning = Color(0xFFA35A00),
    onWarning = Color.White,
    warningContainer = Color(0xFFFDECD2),
    danger = Color(0xFFB3261E),
    dangerContainer = Color(0xFFFBE0DE),
    moneyIn = Color(0xFF0F7A54),
    moneyOut = Color(0xFFB3261E),
    muted = Color(0xFF6B7280),
    elevatedSurface = Color(0xFFFFFFFF),
    heroStart = Color(0xFF17394C),
    heroEnd = Color(0xFF2C6C86),
    divider = Color(0xFFE6E8EC),
)

private val DarkExtras = BokeyaColors(
    success = Color(0xFF5DD3A8),
    onSuccess = Color(0xFF00382A),
    successContainer = Color(0xFF16382E),
    warning = Color(0xFFF2B366),
    onWarning = Color(0xFF3B2200),
    warningContainer = Color(0xFF3A2A14),
    danger = Color(0xFFFF8A80),
    dangerContainer = Color(0xFF3E1B19),
    moneyIn = Color(0xFF5DD3A8),
    moneyOut = Color(0xFFFF8A80),
    muted = Color(0xFF9AA3AF),
    elevatedSurface = Color(0xFF1A1D21),
    heroStart = Color(0xFF13303E),
    heroEnd = Color(0xFF1E4E63),
    divider = Color(0xFF2A2E34),
)

val LocalBokeyaColors = staticCompositionLocalOf { LightExtras }
val LocalBanglaDigits = staticCompositionLocalOf { true }

private fun lightScheme(accent: AccentColor) = lightColorScheme(
    primary = when (accent) {
        AccentColor.DEFAULT -> IndigoPrimary
        AccentColor.BLUE -> BluePrimary
        AccentColor.GREEN -> GreenPrimary
        AccentColor.PURPLE -> PurplePrimary
    },
    onPrimary = Color.White,
    primaryContainer = when (accent) {
        AccentColor.DEFAULT -> Color(0xFFD6E9F3)
        AccentColor.BLUE -> Color(0xFFDCE6FF)
        AccentColor.GREEN -> Color(0xFFD3F0E3)
        AccentColor.PURPLE -> Color(0xFFE9DDFF)
    },
    onPrimaryContainer = Color(0xFF0C2430),
    secondary = Color(0xFF4A6572),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EAEF),
    onSecondaryContainer = Color(0xFF16242B),
    tertiary = Color(0xFF7A5A2E),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF14171A),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF14171A),
    surfaceVariant = Color(0xFFEBEEF2),
    onSurfaceVariant = Color(0xFF565E68),
    outline = Color(0xFFC4CAD2),
    outlineVariant = Color(0xFFE0E4E9),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFBE0DE),
    onErrorContainer = Color(0xFF410E0B),
)

private fun darkScheme(accent: AccentColor) = darkColorScheme(
    primary = when (accent) {
        AccentColor.DEFAULT -> IndigoPrimaryDark
        AccentColor.BLUE -> BluePrimaryDark
        AccentColor.GREEN -> GreenPrimaryDark
        AccentColor.PURPLE -> PurplePrimaryDark
    },
    onPrimary = Color(0xFF04222F),
    primaryContainer = when (accent) {
        AccentColor.DEFAULT -> Color(0xFF1B4457)
        AccentColor.BLUE -> Color(0xFF23386E)
        AccentColor.GREEN -> Color(0xFF104736)
        AccentColor.PURPLE -> Color(0xFF3A2566)
    },
    onPrimaryContainer = Color(0xFFD8EEF9),
    secondary = Color(0xFFB2C4CD),
    onSecondary = Color(0xFF1D2C33),
    secondaryContainer = Color(0xFF2C3A42),
    onSecondaryContainer = Color(0xFFD9E5EB),
    tertiary = Color(0xFFE3C08A),
    background = Color(0xFF101315),
    onBackground = Color(0xFFE6E9EC),
    surface = Color(0xFF101315),
    onSurface = Color(0xFFE6E9EC),
    surfaceVariant = Color(0xFF1E2226),
    onSurfaceVariant = Color(0xFFB6BDC5),
    outline = Color(0xFF3A4048),
    outlineVariant = Color(0xFF2A2E34),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF4A0F0B),
    errorContainer = Color(0xFF3E1B19),
    onErrorContainer = Color(0xFFFFD9D6),
)

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun bn(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    spacing: Double = 0.0,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = spacing.sp,
    lineHeightStyle = lineHeightStyle,
)

// Bangla script needs generous line height; amounts need tight, heavy numerals.
val BokeyaTypography = Typography(
    displayLarge = bn(46, 56, FontWeight.Bold, (-0.5)),
    displayMedium = bn(38, 48, FontWeight.Bold, (-0.4)),
    displaySmall = bn(31, 40, FontWeight.Bold, (-0.3)),
    headlineLarge = bn(28, 38, FontWeight.SemiBold),
    headlineMedium = bn(24, 34, FontWeight.SemiBold),
    headlineSmall = bn(21, 30, FontWeight.SemiBold),
    titleLarge = bn(19, 28, FontWeight.SemiBold),
    titleMedium = bn(16, 24, FontWeight.SemiBold),
    titleSmall = bn(14, 21, FontWeight.SemiBold),
    bodyLarge = bn(16, 26, FontWeight.Normal),
    bodyMedium = bn(14, 23, FontWeight.Normal),
    bodySmall = bn(12, 19, FontWeight.Normal),
    labelLarge = bn(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = bn(12, 17, FontWeight.Medium, 0.2),
    labelSmall = bn(11, 15, FontWeight.Medium, 0.3),
)

object BokeyaShapes {
    val card = 20.dp
    val sheet = 28.dp
    val chip = 12.dp
    val button = 16.dp
    val field = 14.dp
}

@Composable
fun BokeyaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.DEFAULT,
    banglaDigits: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) darkScheme(accent) else lightScheme(accent)
    val extras = if (dark) DarkExtras else LightExtras

    CompositionLocalProvider(
        LocalBokeyaColors provides extras,
        LocalBanglaDigits provides banglaDigits,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BokeyaTypography,
            content = content,
        )
    }
}

val MaterialTheme.bokeya: BokeyaColors
    @Composable get() = LocalBokeyaColors.current
