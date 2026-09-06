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

/*
 * Bokeya design system.
 *
 * Brand direction: a ledger, not a bank app. The primary is a deep pine-teal that reads as
 * "settled accounts" rather than corporate blue; surfaces are warm paper-neutrals in light mode
 * and a five-tier tonal stack in dark mode so depth comes from tone, not shadow.
 */

// ---------------------------------------------------------------- brand ramps

private object Brand {
    // Pine — the default identity. Money, stability, ink on paper.
    val pine = Color(0xFF1B4D45)
    val pineDark = Color(0xFF7FD1BE)
    val pineContainer = Color(0xFFD3E9E2)
    val pineContainerDark = Color(0xFF1E4A43)

    // Indigo — cool, analytical.
    val indigo = Color(0xFF2A3D6B)
    val indigoDark = Color(0xFFA8BEF5)
    val indigoContainer = Color(0xFFDDE4F7)
    val indigoContainerDark = Color(0xFF29385C)

    // Clay — warm, human.
    val clay = Color(0xFF7A3B2E)
    val clayDark = Color(0xFFF0B3A2)
    val clayContainer = Color(0xFFF7E0D8)
    val clayContainerDark = Color(0xFF5A2C21)

    // Plum — quiet, premium.
    val plum = Color(0xFF4A2C55)
    val plumDark = Color(0xFFDCB4E8)
    val plumContainer = Color(0xFFEFDFF4)
    val plumContainerDark = Color(0xFF3D2647)
}

private fun primaryOf(accent: AccentColor, dark: Boolean) = when (accent) {
    AccentColor.DEFAULT -> if (dark) Brand.pineDark else Brand.pine
    AccentColor.BLUE -> if (dark) Brand.indigoDark else Brand.indigo
    AccentColor.GREEN -> if (dark) Brand.clayDark else Brand.clay
    AccentColor.PURPLE -> if (dark) Brand.plumDark else Brand.plum
}

private fun containerOf(accent: AccentColor, dark: Boolean) = when (accent) {
    AccentColor.DEFAULT -> if (dark) Brand.pineContainerDark else Brand.pineContainer
    AccentColor.BLUE -> if (dark) Brand.indigoContainerDark else Brand.indigoContainer
    AccentColor.GREEN -> if (dark) Brand.clayContainerDark else Brand.clayContainer
    AccentColor.PURPLE -> if (dark) Brand.plumContainerDark else Brand.plumContainer
}

/** Ink used on the hero canvas — always the deep end of the ramp, never the light tint. */
private fun canvasOf(accent: AccentColor) = when (accent) {
    AccentColor.DEFAULT -> Color(0xFF12352F) to Color(0xFF1F5A50)
    AccentColor.BLUE -> Color(0xFF1C2A4D) to Color(0xFF33487D)
    AccentColor.GREEN -> Color(0xFF53271D) to Color(0xFF864436)
    AccentColor.PURPLE -> Color(0xFF321D3A) to Color(0xFF573463)
}

// ---------------------------------------------------------------- extended tokens

@Immutable
data class BokeyaColors(
    /** Page background. */
    val canvas: Color,
    /** Tier 1 — grouped content resting on the canvas. */
    val surface1: Color,
    /** Tier 2 — cards that need separation. */
    val surface2: Color,
    /** Tier 3 — sheets, menus, dialogs. */
    val surface3: Color,
    /** Hero / brand canvas gradient. */
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroInset: Color,

    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val info: Color,
    val infoContainer: Color,

    val moneyIn: Color,
    val moneyOut: Color,

    /** Secondary text. */
    val muted: Color,
    /** Tertiary text, captions, disabled. */
    val faint: Color,
    val divider: Color,
    val hairline: Color,

    /** Category ramp for distribution bars — ordered shop / loan / emi / personal. */
    val series: List<Color>,
)

private val LightExtras = BokeyaColors(
    canvas = Color(0xFFF7F6F3),
    surface1 = Color(0xFFFCFBF9),
    surface2 = Color(0xFFFFFFFF),
    surface3 = Color(0xFFFFFFFF),
    heroStart = Color(0xFF12352F),
    heroEnd = Color(0xFF1F5A50),
    onHero = Color(0xFFF4FAF7),
    onHeroMuted = Color(0xB3F4FAF7),
    heroInset = Color(0x1FFFFFFF),
    success = Color(0xFF12704F),
    onSuccess = Color.White,
    successContainer = Color(0xFFDCEFE6),
    warning = Color(0xFF8F5A05),
    onWarning = Color.White,
    warningContainer = Color(0xFFFBECD4),
    danger = Color(0xFFA33A32),
    dangerContainer = Color(0xFFF8E3E0),
    info = Color(0xFF2A3D6B),
    infoContainer = Color(0xFFE2E8F6),
    moneyIn = Color(0xFF12704F),
    moneyOut = Color(0xFFA33A32),
    muted = Color(0xFF5F6B69),
    faint = Color(0xFF8C9694),
    divider = Color(0xFFE4E2DD),
    hairline = Color(0xFFEDEBE6),
    series = listOf(
        Color(0xFF1B4D45),
        Color(0xFF3D7A6B),
        Color(0xFF8F5A05),
        Color(0xFF6A5B8F),
    ),
)

private val DarkExtras = BokeyaColors(
    canvas = Color(0xFF0E1211),
    surface1 = Color(0xFF161B1A),
    surface2 = Color(0xFF1D2322),
    surface3 = Color(0xFF252C2B),
    heroStart = Color(0xFF11302B),
    heroEnd = Color(0xFF1B4A42),
    onHero = Color(0xFFEAF4F0),
    onHeroMuted = Color(0xB3EAF4F0),
    heroInset = Color(0x14FFFFFF),
    success = Color(0xFF63D3AE),
    onSuccess = Color(0xFF00291D),
    successContainer = Color(0xFF17372E),
    warning = Color(0xFFEDBA72),
    onWarning = Color(0xFF3A2400),
    warningContainer = Color(0xFF382A15),
    danger = Color(0xFFF19289),
    dangerContainer = Color(0xFF3C201D),
    info = Color(0xFFA8BEF5),
    infoContainer = Color(0xFF232C42),
    moneyIn = Color(0xFF63D3AE),
    moneyOut = Color(0xFFF19289),
    muted = Color(0xFF9BA8A5),
    faint = Color(0xFF6E7A78),
    divider = Color(0xFF2A312F),
    hairline = Color(0xFF222827),
    series = listOf(
        Color(0xFF7FD1BE),
        Color(0xFF4E9E8B),
        Color(0xFFEDBA72),
        Color(0xFFB3A3D8),
    ),
)

val LocalBokeyaColors = staticCompositionLocalOf { LightExtras }
val LocalBanglaDigits = staticCompositionLocalOf { true }

// ---------------------------------------------------------------- material schemes

private fun lightScheme(accent: AccentColor) = lightColorScheme(
    primary = primaryOf(accent, false),
    onPrimary = Color.White,
    primaryContainer = containerOf(accent, false),
    onPrimaryContainer = Color(0xFF0B231F),
    inversePrimary = primaryOf(accent, true),
    secondary = Color(0xFF4E5B58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E9E6),
    onSecondaryContainer = Color(0xFF1A211F),
    tertiary = Color(0xFF7A5A2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E7CE),
    onTertiaryContainer = Color(0xFF2A1D07),
    background = LightExtras.canvas,
    onBackground = Color(0xFF121615),
    surface = LightExtras.canvas,
    onSurface = Color(0xFF121615),
    surfaceVariant = Color(0xFFEBE9E4),
    onSurfaceVariant = Color(0xFF5F6B69),
    surfaceTint = primaryOf(accent, false),
    inverseSurface = Color(0xFF1F2523),
    inverseOnSurface = Color(0xFFF2F1ED),
    outline = Color(0xFFBFC4C1),
    outlineVariant = LightExtras.divider,
    error = Color(0xFFA33A32),
    onError = Color.White,
    errorContainer = Color(0xFFF8E3E0),
    onErrorContainer = Color(0xFF3E0F0B),
    scrim = Color(0xFF000000),
)

private fun darkScheme(accent: AccentColor) = darkColorScheme(
    primary = primaryOf(accent, true),
    onPrimary = Color(0xFF00271F),
    primaryContainer = containerOf(accent, true),
    onPrimaryContainer = Color(0xFFD6F2E9),
    inversePrimary = primaryOf(accent, false),
    secondary = Color(0xFFB4C0BD),
    onSecondary = Color(0xFF1F2A28),
    secondaryContainer = Color(0xFF2C3634),
    onSecondaryContainer = Color(0xFFD7E2DF),
    tertiary = Color(0xFFE3C08A),
    onTertiary = Color(0xFF3B2A0C),
    tertiaryContainer = Color(0xFF4F3B18),
    onTertiaryContainer = Color(0xFFFAE3BE),
    background = DarkExtras.canvas,
    onBackground = Color(0xFFE7EBE9),
    surface = DarkExtras.canvas,
    onSurface = Color(0xFFE7EBE9),
    surfaceVariant = Color(0xFF232928),
    onSurfaceVariant = Color(0xFF9BA8A5),
    surfaceTint = primaryOf(accent, true),
    inverseSurface = Color(0xFFE7EBE9),
    inverseOnSurface = Color(0xFF1A1F1E),
    outline = Color(0xFF3C4442),
    outlineVariant = DarkExtras.divider,
    error = Color(0xFFF19289),
    onError = Color(0xFF44120E),
    errorContainer = Color(0xFF3C201D),
    onErrorContainer = Color(0xFFFFDAD5),
    scrim = Color(0xFF000000),
)

// ---------------------------------------------------------------- typography

/**
 * Bangla conjuncts get clipped by tight line heights, so every style carries generous leading.
 * Weight is used sparingly: only display/headline go above SemiBold, which keeps the UI editorial
 * rather than shouty.
 */
private val leading = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private val BanglaStack = FontFamily.Default

private fun t(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    spacing: Double = 0.0,
) = TextStyle(
    fontFamily = BanglaStack,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = spacing.sp,
    lineHeightStyle = leading,
)

val BokeyaTypography = Typography(
    displayLarge = t(44, 54, FontWeight.Bold, (-1.0)),
    displayMedium = t(36, 46, FontWeight.Bold, (-0.8)),
    displaySmall = t(30, 40, FontWeight.Bold, (-0.6)),
    headlineLarge = t(27, 38, FontWeight.SemiBold, (-0.3)),
    headlineMedium = t(23, 33, FontWeight.SemiBold, (-0.2)),
    headlineSmall = t(20, 30, FontWeight.SemiBold),
    titleLarge = t(18, 27, FontWeight.SemiBold),
    titleMedium = t(16, 25, FontWeight.SemiBold),
    titleSmall = t(14, 22, FontWeight.SemiBold),
    bodyLarge = t(16, 26, FontWeight.Normal),
    bodyMedium = t(14, 23, FontWeight.Normal),
    bodySmall = t(12, 19, FontWeight.Normal),
    labelLarge = t(14, 20, FontWeight.Medium, 0.1),
    labelMedium = t(12, 17, FontWeight.Medium, 0.3),
    labelSmall = t(11, 15, FontWeight.Medium, 0.5),
)

/** Numerals get tighter tracking and heavier weight so amounts read as data, not prose. */
@Composable
fun moneyStyle(base: TextStyle): TextStyle = base.copy(
    fontWeight = if (base.fontSize.value >= 24f) FontWeight.Bold else FontWeight.SemiBold,
    letterSpacing = (-0.02 * base.fontSize.value).sp,
)

// ---------------------------------------------------------------- metrics

/** 4dp base grid. Every gap in the app comes from here. */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val xxxl = 40.dp

    /** Horizontal page gutter. */
    val gutter = 20.dp
}

object Radius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 26.dp
    val hero = 30.dp
    val sheet = 30.dp
    val pill = 999.dp
}

object Elevation {
    val flat = 0.dp
    val raised = 1.dp
    val floating = 3.dp
    val overlay = 8.dp
}

object IconSize {
    val xs = 14.dp
    val sm = 16.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 32.dp
}

object Durations {
    const val instant = 90
    const val fast = 160
    const val standard = 260
    const val slow = 420
    const val deliberate = 620
}


// ---------------------------------------------------------------- entry point

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
    val (heroStart, heroEnd) = canvasOf(accent)
    val extras = (if (dark) DarkExtras else LightExtras).let {
        if (dark) it.copy(heroStart = heroStart.darken(), heroEnd = heroEnd.darken())
        else it.copy(heroStart = heroStart, heroEnd = heroEnd)
    }

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

private fun Color.darken(factor: Float = 0.82f) =
    Color(red * factor, green * factor, blue * factor, alpha)

val MaterialTheme.bokeya: BokeyaColors
    @Composable get() = LocalBokeyaColors.current
