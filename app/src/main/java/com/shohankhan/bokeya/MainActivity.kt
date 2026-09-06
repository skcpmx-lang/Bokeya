package com.shohankhan.bokeya

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.shohankhan.bokeya.data.repo.BokeyaSettings
import com.shohankhan.bokeya.ui.BokeyaRoot
import com.shohankhan.bokeya.ui.theme.BokeyaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val deepLink = MutableStateFlow<String?>(null)
    private var settingsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !settingsLoaded }
        // Hand the splash over with a slow lift-and-fade rather than a cut.
        splash.setOnExitAnimationListener { provider ->
            val view = provider.view
            val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.06f)
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.06f)
            AnimatorSet().apply {
                playTogether(fade, scaleX, scaleY)
                duration = 380L
                interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = provider.remove()
                })
                start()
            }
        }
        enableEdgeToEdge()

        val container = appContainer
        deepLink.value = intent?.data?.toString()

        lifecycleScope.launch {
            val initial = container.settings.settings.first()
            applySecurityFlags(initial)
            settingsLoaded = true
        }

        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = BokeyaSettings())
            val link by deepLink.collectAsStateWithLifecycle()

            BokeyaTheme(
                themeMode = settings.themeMode,
                accent = settings.accent,
                banglaDigits = settings.banglaDigits,
            ) {
                BokeyaRoot(
                    container = container,
                    settings = settings,
                    activity = this,
                    deepLink = link,
                    onDeepLinkHandled = { deepLink.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = intent.data?.toString()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            applySecurityFlags(appContainer.settings.settings.first())
        }
    }

    private fun applySecurityFlags(settings: BokeyaSettings) {
        if (settings.blockScreenshots || settings.hideInRecents) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
