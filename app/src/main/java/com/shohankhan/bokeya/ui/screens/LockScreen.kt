package com.shohankhan.bokeya.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.security.BiometricHelper
import com.shohankhan.bokeya.ui.theme.bokeya
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    activity: FragmentActivity,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    fun submit(candidate: String) {
        if (onVerify(candidate)) {
            onUnlocked()
        } else {
            error = "PIN মিলছে না। আবার চেষ্টা করুন।"
            pin = ""
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                shake.snapTo(0f)
                repeat(3) {
                    shake.animateTo(1f, tween(50))
                    shake.animateTo(-1f, tween(50))
                }
                shake.animateTo(0f, tween(50))
            }
        }
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && BiometricHelper.isAvailable(activity)) {
            BiometricHelper.prompt(activity, onSuccess = onUnlocked, onError = { })
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("বকেয়া", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "আপনার PIN দিন",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.bokeya.muted,
            )

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.offset { IntOffset((shake.value * 14).roundToInt(), 0) },
            ) {
                repeat(6) { index ->
                    val filled = index < pin.length
                    Box(
                        Modifier
                            .size(if (filled) 14.dp else 11.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                error ?: " ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            Spacer(Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        row.forEach { digit ->
                            PinKey(digit) {
                                if (pin.length < 6) {
                                    pin += digit
                                    error = null
                                    if (pin.length >= 4) {
                                        // auto-submit at common PIN lengths
                                        if (onVerify(pin)) onUnlocked()
                                    }
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(70.dp)) {
                        if (biometricEnabled) {
                            PinIconKey(Icons.Filled.Fingerprint, "Biometric") {
                                BiometricHelper.prompt(activity, onSuccess = onUnlocked, onError = { error = it })
                            }
                        }
                    }
                    PinKey("0") {
                        if (pin.length < 6) {
                            pin += "0"
                            error = null
                            if (pin.length >= 4 && onVerify(pin)) onUnlocked()
                        }
                    }
                    PinIconKey(Icons.AutoMirrored.Filled.Backspace, "মুছুন") {
                        pin = pin.dropLast(1)
                        error = null
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            TextButton(onClick = { submit(pin) }, enabled = pin.length >= 4) {
                Text("খুলুন")
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PinKey(digit: String, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .size(70.dp)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                BanglaNumbers.toBanglaDigits(digit),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PinIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier
            .size(70.dp)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(24.dp))
        }
    }
}
