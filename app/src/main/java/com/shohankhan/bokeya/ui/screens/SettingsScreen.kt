package com.shohankhan.bokeya.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.BuildConfig
import com.shohankhan.bokeya.backup.ReportKind
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.data.repo.AccentColor
import com.shohankhan.bokeya.data.repo.ThemeMode
import com.shohankhan.bokeya.security.BiometricHelper
import com.shohankhan.bokeya.ui.Route
import com.shohankhan.bokeya.ui.SettingsViewModel
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaTextField
import com.shohankhan.bokeya.ui.components.ChipSelector
import com.shohankhan.bokeya.ui.components.InfoRow
import com.shohankhan.bokeya.ui.components.PrimaryButton
import com.shohankhan.bokeya.ui.components.SecondaryButton
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.components.SelectorOption
import com.shohankhan.bokeya.ui.theme.bokeya

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.bokeya.muted)
            }
        }
        when {
            checked != null && onCheckedChange != null ->
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            trailing != null ->
                Text(
                    trailing,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    activity: FragmentActivity,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showHourDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ScreenScaffold(title = "Settings", onBack = onBack, snackbarHostState = snackbarHostState) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("profile") {
                BokeyaCard(onClick = { showNameDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                settings.userName.take(1).ifBlank { "ব" },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                settings.userName.ifBlank { "নাম যোগ করুন" },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Bokeya " + BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.bokeya.muted,
                            )
                        }
                    }
                }
            }

            item("appearance_h") { SectionHeader("Appearance") }
            item("appearance") {
                BokeyaCard {
                    ChipSelector(
                        options = listOf(
                            SelectorOption(ThemeMode.SYSTEM.name, "System"),
                            SelectorOption(ThemeMode.LIGHT.name, "Light"),
                            SelectorOption(ThemeMode.DARK.name, "Dark"),
                        ),
                        selectedId = settings.themeMode.name,
                        onSelect = { viewModel.setTheme(ThemeMode.valueOf(it)) },
                        label = "Theme",
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Accent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.bokeya.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            AccentColor.DEFAULT to Color(0xFF1F4B63),
                            AccentColor.BLUE to Color(0xFF1D4ED8),
                            AccentColor.GREEN to Color(0xFF11624A),
                            AccentColor.PURPLE to Color(0xFF4C1D95),
                        ).forEach { (accent, color) ->
                            val selected = settings.accent == accent
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { viewModel.setAccent(accent) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    SettingRow(
                        "বাংলা সংখ্যা",
                        "১২৩৪ নাকি 1234",
                        checked = settings.banglaDigits,
                        onCheckedChange = { viewModel.setBanglaDigits(it) },
                    )
                }
            }

            item("notif_h") { SectionHeader("Notifications") }
            item("notif") {
                BokeyaCard {
                    SettingRow(
                        "Notification চালু",
                        "কিস্তি ও বকেয়ার কথা মনে করিয়ে দেবে",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotifications(it) },
                    )
                    SettingRow(
                        "কখন মনে করাবে",
                        "প্রতিদিনের সারাংশের সময়",
                        onClick = { showHourDialog = true },
                        trailing = BanglaNumbers.toBanglaDigits(settings.reminderHour.toString()) + ":০০",
                    )
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(
                        text = "এখনই একটি notification দেখুন",
                        onClick = { viewModel.testNotification() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item("security_h") { SectionHeader("Security") }
            item("security") {
                BokeyaCard {
                    SettingRow(
                        "App Lock",
                        if (settings.hasPin) "PIN চালু আছে" else "PIN দিয়ে app সুরক্ষিত করুন",
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) showPinDialog = true else viewModel.disableLock()
                        },
                    )
                    if (settings.appLockEnabled && settings.hasPin) {
                        SettingRow(
                            "Biometric",
                            if (BiometricHelper.isAvailable(activity)) {
                                "আঙুলের ছাপ দিয়ে খুলুন"
                            } else {
                                "এই device-এ পাওয়া যাচ্ছে না"
                            },
                            checked = settings.biometricEnabled,
                            onCheckedChange = { viewModel.setBiometric(it) },
                        )
                        SettingRow("PIN বদলান", onClick = { showPinDialog = true }, trailing = "বদলান")
                    }
                    SettingRow(
                        "Screenshot বন্ধ রাখুন",
                        "Recents-এও তথ্য লুকানো থাকবে",
                        checked = settings.blockScreenshots,
                        onCheckedChange = { viewModel.setBlockScreenshots(it) },
                    )
                }
            }

            item("behaviour_h") { SectionHeader("অন্যান্য") }
            item("behaviour") {
                BokeyaCard {
                    SettingRow(
                        "Haptic feedback",
                        "গুরুত্বপূর্ণ কাজে হালকা কাঁপুনি",
                        checked = settings.hapticsEnabled,
                        onCheckedChange = { viewModel.setHaptics(it) },
                    )
                    SettingRow(
                        "পরিশোধ শেষে confetti",
                        "পুরো টাকা শোধ হলে ছোট্ট উদযাপন",
                        checked = settings.confettiEnabled,
                        onCheckedChange = { viewModel.setConfetti(it) },
                    )
                    SettingRow(
                        "পরিশোধ হলে archive",
                        "শেষ হওয়া হিসাব নিজে থেকে গুছিয়ে রাখবে",
                        checked = settings.autoArchivePaid,
                        onCheckedChange = { viewModel.setAutoArchive(it) },
                    )
                }
            }

            item("data_h") { SectionHeader("Data") }
            item("data") {
                BokeyaCard {
                    SettingRow("Backup ও Export", onClick = { onNavigate(Route.Backup.path) }, trailing = "খুলুন")
                    SettingRow("Categories", onClick = { onNavigate(Route.Categories.path) }, trailing = "খুলুন")
                    SettingRow("Currency", trailing = "৳ BDT")
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(
                        text = "সব তথ্য মুছে ফেলুন",
                        onClick = { showWipeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item("about") {
                BokeyaCard(onClick = { onNavigate(Route.About.path) }) {
                    InfoRow("About", "Bokeya " + BuildConfig.VERSION_NAME)
                    InfoRow("Developer", "Shohan Khan")
                }
            }
        }
    }

    if (showNameDialog) {
        var name by remember { mutableStateOf(settings.userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("আপনার নাম") },
            text = { BokeyaTextField(name, { name = it }, "নাম") },
            confirmButton = {
                TextButton(onClick = { viewModel.setName(name); showNameDialog = false }) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("বাতিল") } },
        )
    }

    if (showPinDialog) {
        var pin by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val mismatch = confirm.isNotEmpty() && pin != confirm
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("PIN দিন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "৪ থেকে ৬ সংখ্যার একটি PIN দিন। ভুলে গেলে app-এর তথ্য আর খোলা যাবে না, " +
                            "তাই মনে রাখার মতো PIN দিন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.bokeya.muted,
                    )
                    BokeyaTextField(
                        pin,
                        { pin = it.filter { c -> c.isDigit() }.take(6) },
                        "নতুন PIN",
                        keyboardType = KeyboardType.NumberPassword,
                    )
                    BokeyaTextField(
                        confirm,
                        { confirm = it.filter { c -> c.isDigit() }.take(6) },
                        "আবার লিখুন",
                        keyboardType = KeyboardType.NumberPassword,
                        isError = mismatch,
                        supportingText = if (mismatch) "PIN দুটি মিলছে না।" else null,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pin.length >= 4 && pin == confirm,
                    onClick = { viewModel.setPin(pin); showPinDialog = false },
                ) { Text("চালু করুন") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("বাতিল") } },
        )
    }

    if (showHourDialog) {
        AlertDialog(
            onDismissRequest = { showHourDialog = false },
            title = { Text("কখন মনে করাবে?") },
            text = {
                ChipSelector(
                    options = listOf(7, 8, 9, 10, 12, 18, 20).map {
                        SelectorOption(it.toString(), BanglaNumbers.toBanglaDigits(it.toString()) + ":০০")
                    },
                    selectedId = settings.reminderHour.toString(),
                    onSelect = { viewModel.setReminderHour(it.toInt()); showHourDialog = false },
                )
            },
            confirmButton = { TextButton(onClick = { showHourDialog = false }) { Text("বন্ধ") } },
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("সব তথ্য মুছে ফেলবেন?") },
            text = {
                Text(
                    "সব হিসাব, payment history, আয়-ব্যয় এবং লক্ষ্য চিরতরে মুছে যাবে। " +
                        "আগে একটি backup নিয়ে রাখুন।",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.wipeAll(); showWipeDialog = false }) {
                    Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showWipeDialog = false }) { Text("বাতিল") } },
        )
    }
}

@Composable
fun BackupScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val preview by viewModel.restorePreview.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.previewRestore(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importCsv(it) } }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let {
            viewModel.exportCsv(it, ReportKind.FULL, Clocks.today().minusYears(5), Clocks.today())
        }
    }

    ScreenScaffold(
        title = "Backup ও Export",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("info") {
                BokeyaCard {
                    Text(
                        "Backup ফাইল আপনার নিজের ফোনে সংরক্ষিত হয়। কোথায় রাখবেন সেটি আপনি ঠিক করবেন — " +
                            "Bokeya কোনো server-এ কিছু পাঠায় না।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.bokeya.muted,
                    )
                }
            }

            item("backup_h") { SectionHeader("Backup") }
            item("backup") {
                BokeyaCard {
                    Text(
                        "সব হিসাব, payment, আয়-ব্যয়, category ও schedule একটি ফাইলে সংরক্ষিত হবে।",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        text = "Backup তৈরি করুন",
                        onClick = { exportLauncher.launch(viewModel.backupFileName()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = "Backup থেকে Restore",
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item("export_h") { SectionHeader("Export ও Import") }
            item("export") {
                BokeyaCard {
                    SecondaryButton(
                        text = "সব তথ্য CSV হিসেবে",
                        onClick = { csvLauncher.launch("bokeya-full-${Clocks.today()}.csv") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text = "CSV থেকে Import",
                        onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Import করার সময় একই তারিখ, নাম ও টাকার entry আবার যোগ হবে না।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.bokeya.muted,
                    )
                }
            }
        }
    }

    preview?.let { payload ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            title = { Text("Restore করবেন?") },
            text = {
                Column {
                    Text(
                        "এই backup-এ যা আছে:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.bokeya.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow("হিসাব", BanglaNumbers.toBanglaDigits(payload.accounts.size.toString()))
                    InfoRow("লেনদেন", BanglaNumbers.toBanglaDigits(payload.transactions.size.toString()))
                    InfoRow("Payment", BanglaNumbers.toBanglaDigits(payload.payments.size.toString()))
                    InfoRow("মানুষ", BanglaNumbers.toBanglaDigits(payload.people.size.toString()))
                    InfoRow("মোট রেকর্ড", BanglaNumbers.toBanglaDigits(payload.recordCount.toString()))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "সতর্কতা: Restore করলে এখনকার সব তথ্য মুছে গিয়ে backup-এর তথ্য বসবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.bokeya.danger,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) { Text("Restore করুন") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRestore() }) { Text("বাতিল") }
            },
        )
    }
}
