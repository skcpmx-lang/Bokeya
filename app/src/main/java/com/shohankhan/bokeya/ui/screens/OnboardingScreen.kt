package com.shohankhan.bokeya.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.ui.components.BokeyaTextField
import com.shohankhan.bokeya.ui.components.PrimaryButton
import com.shohankhan.bokeya.ui.theme.bokeya

private data class OnboardPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val pages = listOf(
    OnboardPage(
        Icons.Filled.AccountBalanceWallet,
        "সব বকেয়া এক জায়গায়",
        "দোকানের বাকি, ব্যক্তিগত ধার, Loan আর EMI — সব একসাথে দেখুন।",
    ),
    OnboardPage(
        Icons.Filled.EventAvailable,
        "Loan আর EMI-এর কিস্তি ভুলবেন না",
        "কোন দিন কত দিতে হবে, app নিজেই মনে করিয়ে দেবে।",
    ),
    OnboardPage(
        Icons.Filled.Insights,
        "আয়-ব্যয়ের হিসাব রাখুন",
        "মাসে কত এলো, কত গেল আর হাতে কত থাকল — সব পরিষ্কার।",
    ),
    OnboardPage(
        Icons.Filled.Lock,
        "আপনার টাকার হিসাব, আপনার নিয়ন্ত্রণে",
        "সব তথ্য শুধু আপনার ফোনেই থাকে। কোনো বিজ্ঞাপন নেই, কোনো server নেই।",
    ),
)

@Composable
fun OnboardingScreen(onFinish: (String) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    val isLast = page == pages.size

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!isLast) {
                    TextButton(onClick = { page = pages.size }) { Text("Skip") }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(280)))
                        .togetherWith(slideOutHorizontally(tween(320)) { -it / 3 } + fadeOut(tween(200)))
                },
                label = "onboard",
            ) { index ->
                if (index < pages.size) {
                    val item = pages[index]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(112.dp)
                                .clip(RoundedCornerShape(34.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.bokeya.heroStart,
                                            MaterialTheme.bokeya.heroEnd,
                                        ),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                item.icon,
                                null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(46.dp),
                            )
                        }
                        Spacer(Modifier.height(34.dp))
                        Text(
                            item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            item.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.bokeya.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "আপনাকে কী নামে ডাকব?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "চাইলে খালি রেখেও শুরু করতে পারেন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.bokeya.muted,
                        )
                        Spacer(Modifier.height(26.dp))
                        BokeyaTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "আপনার নাম",
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                repeat(pages.size + 1) { index ->
                    val active = index == page
                    Box(
                        Modifier
                            .height(7.dp)
                            .width(if (active) 22.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }

            PrimaryButton(
                text = if (isLast) "শুরু করি" else "পরবর্তী",
                onClick = { if (isLast) onFinish(name) else page++ },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
