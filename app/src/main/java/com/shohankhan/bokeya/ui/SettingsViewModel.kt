package com.shohankhan.bokeya.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohankhan.bokeya.AppContainer
import com.shohankhan.bokeya.BokeyaApplication
import com.shohankhan.bokeya.backup.BackupManager
import com.shohankhan.bokeya.backup.BackupPayload
import com.shohankhan.bokeya.backup.BackupResult
import com.shohankhan.bokeya.backup.CsvExporter
import com.shohankhan.bokeya.backup.PdfExporter
import com.shohankhan.bokeya.backup.ReportBuilder
import com.shohankhan.bokeya.backup.ReportData
import com.shohankhan.bokeya.backup.ReportKind
import com.shohankhan.bokeya.backup.RestoreResult
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.data.repo.AccentColor
import com.shohankhan.bokeya.data.repo.BokeyaSettings
import com.shohankhan.bokeya.data.repo.SettingsStore
import com.shohankhan.bokeya.data.repo.ThemeMode
import com.shohankhan.bokeya.security.PinCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(
    application: Application,
    private val container: AppContainer,
) : AndroidViewModel(application) {

    private val store: SettingsStore = container.settings

    val settings: StateFlow<BokeyaSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BokeyaSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _restorePreview = MutableStateFlow<BackupPayload?>(null)
    val restorePreview = _restorePreview.asStateFlow()

    private val backupManager = BackupManager(application, container.database)
    private val reportBuilder = ReportBuilder(container.repository, container.database)

    fun clearMessage() { _message.value = null }

    fun setName(value: String) = viewModelScope.launch { store.setUserName(value.trim()) }
    fun setOnboarded(value: Boolean) = viewModelScope.launch { store.setOnboarded(value) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }
    fun setAccent(accent: AccentColor) = viewModelScope.launch { store.setAccent(accent) }
    fun setBanglaDigits(value: Boolean) = viewModelScope.launch { store.setBanglaDigits(value) }
    fun setConfetti(value: Boolean) = viewModelScope.launch { store.setConfetti(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { store.setHaptics(value) }
    fun setAutoArchive(value: Boolean) = viewModelScope.launch { store.setAutoArchive(value) }
    fun setBlockScreenshots(value: Boolean) = viewModelScope.launch { store.setBlockScreenshots(value) }
    fun setHideInRecents(value: Boolean) = viewModelScope.launch { store.setHideInRecents(value) }
    fun setBiometric(value: Boolean) = viewModelScope.launch { store.setBiometric(value) }

    fun setNotifications(value: Boolean) = viewModelScope.launch {
        store.setNotifications(value)
        if (value) container.scheduler.scheduleDailyDigest() else container.scheduler.cancelAll()
    }

    fun setReminderHour(hour: Int) = viewModelScope.launch {
        store.setReminderHour(hour)
        container.scheduler.scheduleDailyDigest(hour)
    }

    fun setPin(pin: String) = viewModelScope.launch {
        if (pin.length < 4) {
            _message.value = "PIN কমপক্ষে ৪ সংখ্যার হতে হবে।"
            return@launch
        }
        store.setPinHash(PinCodec.encode(pin))
        store.setAppLock(true)
        _message.value = "App Lock চালু হয়েছে।"
    }

    fun disableLock() = viewModelScope.launch {
        store.setAppLock(false)
        store.setPinHash("")
        store.setBiometric(false)
        _message.value = "App Lock বন্ধ হয়েছে।"
    }

    fun verifyPin(pin: String): Boolean = PinCodec.verify(pin, settings.value.pinHash)

    fun testNotification() {
        container.scheduler.runDigestNow()
        _message.value = "আজকের সারাংশ পাঠানো হয়েছে।"
    }

    // ------------------------------------------------------------------ backup

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        when (val result = backupManager.exportTo(uri)) {
            is BackupResult.Success ->
                _message.value = "Backup সংরক্ষিত হয়েছে (${result.records}টি রেকর্ড)।"
            is BackupResult.Failure -> _message.value = result.message
        }
    }

    fun previewRestore(uri: Uri) = viewModelScope.launch {
        when (val result = backupManager.preview(uri)) {
            is RestoreResult.Preview -> _restorePreview.value = result.payload
            is RestoreResult.Failure -> _message.value = result.message
            else -> Unit
        }
    }

    fun cancelRestore() { _restorePreview.value = null }

    fun confirmRestore() = viewModelScope.launch {
        val payload = _restorePreview.value ?: return@launch
        when (val result = backupManager.restore(payload)) {
            is RestoreResult.Success -> _message.value = "${result.records}টি রেকর্ড restore হয়েছে।"
            is RestoreResult.Failure -> _message.value = result.message
            else -> Unit
        }
        _restorePreview.value = null
    }

    fun backupFileName() = backupManager.defaultFileName()

    fun wipeAll() = viewModelScope.launch {
        container.repository.wipeAllData()
        _message.value = "সব তথ্য মুছে ফেলা হয়েছে।"
    }

    // ------------------------------------------------------------------ export

    fun exportCsv(uri: Uri, kind: ReportKind, from: LocalDate, to: LocalDate, onDone: (Boolean) -> Unit = {}) =
        viewModelScope.launch {
            val report = reportBuilder.build(kind, from, to)
            val ok = CsvExporter.write(getApplication(), uri, CsvExporter.toCsv(report))
            _message.value = if (ok) "CSV তৈরি হয়েছে।" else "CSV তৈরি করা যায়নি।"
            onDone(ok)
        }

    fun exportPdf(uri: Uri, kind: ReportKind, from: LocalDate, to: LocalDate, onDone: (Boolean) -> Unit = {}) =
        viewModelScope.launch {
            val report = reportBuilder.build(kind, from, to)
            val ok = PdfExporter.write(getApplication(), uri, report)
            _message.value = if (ok) "PDF তৈরি হয়েছে।" else "PDF তৈরি করা যায়নি।"
            onDone(ok)
        }

    fun importCsv(uri: Uri) = viewModelScope.launch {
        val text = runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text == null) {
            _message.value = "ফাইলটি পড়া যায়নি।"
            return@launch
        }
        val rows = CsvExporter.parseTransactions(text)
        if (rows.isEmpty()) {
            _message.value = "এই ফাইলে import করার মতো কিছু পাওয়া যায়নি।"
            return@launch
        }
        val existing = container.repository.transactionsBetweenOnce(
            rows.minOf { it.date },
            rows.maxOf { it.date },
        )
        var imported = 0
        rows.forEach { row ->
            val duplicate = existing.any {
                it.date == row.date.toEpochDay() && it.title == row.title && it.amount == row.amount.poisha
            }
            if (!duplicate) {
                val type = when (row.type) {
                    "আয়" -> com.shohankhan.bokeya.domain.TxType.INCOME
                    "খরচ" -> com.shohankhan.bokeya.domain.TxType.EXPENSE
                    else -> com.shohankhan.bokeya.domain.TxType.EXPENSE
                }
                container.repository.addTransaction(type, row.amount, row.title, row.date)
                imported++
            }
        }
        _message.value = "${imported}টি entry import হয়েছে (${rows.size - imported}টি আগে থেকেই ছিল)।"
    }
}

class ReportsViewModel(
    application: Application,
    private val container: AppContainer,
) : AndroidViewModel(application) {

    private val builder = ReportBuilder(container.repository, container.database)

    private val _kind = MutableStateFlow(ReportKind.FULL)
    val kind = _kind.asStateFlow()

    private val _range = MutableStateFlow(
        Clocks.today().withDayOfMonth(1) to Clocks.today(),
    )
    val range = _range.asStateFlow()

    private val _report = MutableStateFlow<ReportData?>(null)
    val report = _report.asStateFlow()

    init { reload() }

    fun setKind(value: ReportKind) { _kind.value = value; reload() }

    fun setRange(from: LocalDate, to: LocalDate) {
        _range.value = from to to
        reload()
    }

    fun setPreset(preset: RangePreset) {
        val today = Clocks.today()
        _range.value = when (preset) {
            RangePreset.TODAY -> today to today
            RangePreset.WEEK -> today.minusDays(6) to today
            RangePreset.MONTH -> today.withDayOfMonth(1) to today
            RangePreset.YEAR -> today.withDayOfYear(1) to today
        }
        reload()
    }

    private fun reload() = viewModelScope.launch {
        val (from, to) = _range.value
        _report.value = builder.build(_kind.value, from, to)
    }
}

enum class RangePreset(val label: String) {
    TODAY("আজ"),
    WEEK("সপ্তাহ"),
    MONTH("মাস"),
    YEAR("বছর"),
}

object BokeyaViewModels {

    private fun container(extras: CreationExtras): AppContainer {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BokeyaApplication
        return app.container
    }

    val Factory = viewModelFactory {
        initializer { DashboardViewModel(container(this).repository, container(this).settings) }
        initializer { AccountsViewModel(container(this).repository) }
        initializer { CashflowViewModel(container(this).repository) }
        initializer { SearchViewModel(container(this).repository) }
        initializer { GoalsViewModel(container(this).repository) }
        initializer { RecurringViewModel(container(this).repository) }
        initializer { CalendarViewModel(container(this).repository) }
        initializer { PlannerViewModel(container(this).repository) }
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BokeyaApplication
            SettingsViewModel(app, app.container)
        }
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BokeyaApplication
            ReportsViewModel(app, app.container)
        }
    }

    fun detailFactory(container: AppContainer, accountId: Long) = viewModelFactory {
        initializer { DetailViewModel(container.repository, accountId) }
    }
}
