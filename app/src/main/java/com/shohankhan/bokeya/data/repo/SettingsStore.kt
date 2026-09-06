package com.shohankhan.bokeya.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bokeya_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentColor { DEFAULT, BLUE, GREEN, PURPLE }

data class BokeyaSettings(
    val userName: String = "",
    val onboarded: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.DEFAULT,
    val banglaDigits: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val biometricEnabled: Boolean = false,
    val hideInRecents: Boolean = false,
    val blockScreenshots: Boolean = false,
    val confettiEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoArchivePaid: Boolean = true,
    val currencyCode: String = "BDT",
) {
    val hasPin: Boolean get() = pinHash.isNotBlank()
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val onboarded = booleanPreferencesKey("onboarded")
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val banglaDigits = booleanPreferencesKey("bangla_digits")
        val notifications = booleanPreferencesKey("notifications")
        val reminderHour = intPreferencesKey("reminder_hour")
        val appLock = booleanPreferencesKey("app_lock")
        val pinHash = stringPreferencesKey("pin_hash")
        val biometric = booleanPreferencesKey("biometric")
        val hideInRecents = booleanPreferencesKey("hide_recents")
        val blockScreenshots = booleanPreferencesKey("block_screenshots")
        val confetti = booleanPreferencesKey("confetti")
        val haptics = booleanPreferencesKey("haptics")
        val autoArchive = booleanPreferencesKey("auto_archive")
        val currency = stringPreferencesKey("currency")
    }

    val settings: Flow<BokeyaSettings> = context.dataStore.data.map { prefs ->
        BokeyaSettings(
            userName = prefs[Keys.userName] ?: "",
            onboarded = prefs[Keys.onboarded] ?: false,
            themeMode = prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accent = prefs[Keys.accent]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() }
                ?: AccentColor.DEFAULT,
            banglaDigits = prefs[Keys.banglaDigits] ?: true,
            notificationsEnabled = prefs[Keys.notifications] ?: true,
            reminderHour = prefs[Keys.reminderHour] ?: 9,
            appLockEnabled = prefs[Keys.appLock] ?: false,
            pinHash = prefs[Keys.pinHash] ?: "",
            biometricEnabled = prefs[Keys.biometric] ?: false,
            hideInRecents = prefs[Keys.hideInRecents] ?: false,
            blockScreenshots = prefs[Keys.blockScreenshots] ?: false,
            confettiEnabled = prefs[Keys.confetti] ?: true,
            hapticsEnabled = prefs[Keys.haptics] ?: true,
            autoArchivePaid = prefs[Keys.autoArchive] ?: true,
            currencyCode = prefs[Keys.currency] ?: "BDT",
        )
    }

    suspend fun setUserName(value: String) = edit { it[Keys.userName] = value }
    suspend fun setOnboarded(value: Boolean) = edit { it[Keys.onboarded] = value }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.themeMode] = value.name }
    suspend fun setAccent(value: AccentColor) = edit { it[Keys.accent] = value.name }
    suspend fun setBanglaDigits(value: Boolean) = edit { it[Keys.banglaDigits] = value }
    suspend fun setNotifications(value: Boolean) = edit { it[Keys.notifications] = value }
    suspend fun setReminderHour(value: Int) = edit { it[Keys.reminderHour] = value.coerceIn(0, 23) }
    suspend fun setAppLock(value: Boolean) = edit { it[Keys.appLock] = value }
    suspend fun setPinHash(value: String) = edit { it[Keys.pinHash] = value }
    suspend fun setBiometric(value: Boolean) = edit { it[Keys.biometric] = value }
    suspend fun setHideInRecents(value: Boolean) = edit { it[Keys.hideInRecents] = value }
    suspend fun setBlockScreenshots(value: Boolean) = edit { it[Keys.blockScreenshots] = value }
    suspend fun setConfetti(value: Boolean) = edit { it[Keys.confetti] = value }
    suspend fun setHaptics(value: Boolean) = edit { it[Keys.haptics] = value }
    suspend fun setAutoArchive(value: Boolean) = edit { it[Keys.autoArchive] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
