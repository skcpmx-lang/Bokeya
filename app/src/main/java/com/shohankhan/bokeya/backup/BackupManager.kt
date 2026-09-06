package com.shohankhan.bokeya.backup

import android.content.Context
import android.net.Uri
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.AccountEntity
import com.shohankhan.bokeya.data.db.BokeyaDatabase
import com.shohankhan.bokeya.data.db.CategoryEntity
import com.shohankhan.bokeya.data.db.GoalEntity
import com.shohankhan.bokeya.data.db.InstallmentEntity
import com.shohankhan.bokeya.data.db.PaymentEntity
import com.shohankhan.bokeya.data.db.PersonEntity
import com.shohankhan.bokeya.data.db.RecurringEntity
import com.shohankhan.bokeya.data.db.ReminderEntity
import com.shohankhan.bokeya.data.db.ShopItemEntity
import com.shohankhan.bokeya.data.db.ShopPurchaseEntity
import com.shohankhan.bokeya.data.db.TransactionEntity
import com.shohankhan.bokeya.domain.TxType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/**
 * Backup payload. [schemaVersion] lets future app versions migrate older files instead of
 * rejecting them. Everything stays on-device: the user chooses the destination via SAF.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int = SCHEMA_VERSION,
    val appVersion: String = "1.0.0",
    val createdAt: Long = 0,
    val accounts: List<AccountDto> = emptyList(),
    val people: List<PersonDto> = emptyList(),
    val purchases: List<PurchaseDto> = emptyList(),
    val items: List<ItemDto> = emptyList(),
    val installments: List<InstallmentDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val recurring: List<RecurringDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
) {
    val recordCount: Int
        get() = accounts.size + people.size + purchases.size + items.size + installments.size +
            payments.size + transactions.size + categories.size + recurring.size +
            reminders.size + goals.size

    companion object { const val SCHEMA_VERSION = 1 }
}

@Serializable data class AccountDto(
    val id: Long, val type: String, val direction: String, val title: String, val subtitle: String?,
    val principal: Long, val interestAmount: Long, val downPayment: Long, val installmentAmount: Long,
    val installmentCount: Int, val frequency: String?, val customIntervalDays: Int,
    val startDate: Long?, val endDate: Long?, val dueDate: Long?, val personId: Long?,
    val institution: String?, val brand: String?, val seller: String?, val paymentMethod: String?,
    val note: String?, val archived: Boolean, val paused: Boolean, val createdAt: Long, val updatedAt: Long,
)

@Serializable data class PersonDto(
    val id: Long, val name: String, val relationship: String, val phone: String?,
    val note: String?, val createdAt: Long, val updatedAt: Long,
)

@Serializable data class PurchaseDto(
    val id: Long, val accountId: Long, val date: Long, val timeMinutes: Int,
    val note: String?, val createdAt: Long,
)

@Serializable data class ItemDto(
    val id: Long, val purchaseId: Long, val name: String, val quantity: Double,
    val unit: String, val unitPrice: Long, val total: Long, val note: String?,
)

@Serializable data class InstallmentDto(
    val id: Long, val accountId: Long, val number: Int, val dueDate: Long,
    val expectedAmount: Long, val paidAmount: Long, val status: String, val paidAt: Long?,
)

@Serializable data class PaymentDto(
    val id: Long, val accountId: Long, val installmentId: Long?, val amount: Long,
    val date: Long, val timeMinutes: Int, val method: String, val note: String?, val createdAt: Long,
)

@Serializable data class TransactionDto(
    val id: Long, val type: String, val amount: Long, val date: Long, val timeMinutes: Int,
    val title: String, val categoryId: Long?, val accountId: Long?, val personId: Long?,
    val method: String?, val note: String?, val createdAt: Long, val updatedAt: Long,
)

@Serializable data class CategoryDto(
    val id: Long, val name: String, val kind: String, val icon: String,
    val colorArgb: Int, val builtIn: Boolean, val sortOrder: Int,
)

@Serializable data class RecurringDto(
    val id: Long, val title: String, val type: String, val amount: Long, val categoryId: Long?,
    val accountId: Long?, val frequency: String, val customIntervalDays: Int, val anchorDate: Long,
    val nextRun: Long, val autoCreate: Boolean, val active: Boolean, val note: String?, val createdAt: Long,
)

@Serializable data class ReminderDto(
    val id: Long, val accountId: Long?, val installmentId: Long?, val title: String,
    val message: String, val triggerAt: Long, val enabled: Boolean, val fired: Boolean, val createdAt: Long,
)

@Serializable data class GoalDto(
    val id: Long, val title: String, val targetAmount: Long, val savedAmount: Long, val kind: String,
    val linkedAccountId: Long?, val targetDate: Long?, val note: String?, val archived: Boolean,
    val createdAt: Long, val updatedAt: Long,
)

sealed interface BackupResult {
    data class Success(val fileName: String, val records: Int) : BackupResult
    data class Failure(val message: String) : BackupResult
}

sealed interface RestoreResult {
    data class Preview(val payload: BackupPayload) : RestoreResult
    data class Success(val records: Int) : RestoreResult
    data class Failure(val message: String) : RestoreResult
}

class BackupManager(
    private val context: Context,
    private val db: BokeyaDatabase,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun buildPayload(): BackupPayload = BackupPayload(
        createdAt = Clocks.nowMillis(),
        accounts = db.accountDao().allOnce().map { it.toDto() },
        people = db.personDao().allOnce().map { it.toDto() },
        purchases = db.shopDao().allPurchases().map { it.toDto() },
        items = db.shopDao().allItems().map { it.toDto() },
        installments = db.installmentDao().allOnce().map { it.toDto() },
        payments = db.paymentDao().allOnce().map { it.toDto() },
        transactions = db.transactionDao().allOnce().map { it.toDto() },
        categories = db.categoryDao().allOnce().map { it.toDto() },
        recurring = db.recurringDao().allOnce().map { it.toDto() },
        reminders = db.reminderDao().allOnce().map { it.toDto() },
        goals = db.goalDao().allOnce().map { it.toDto() },
    )

    suspend fun exportTo(uri: Uri): BackupResult = runCatching {
        val payload = buildPayload()
        val text = json.encodeToString(BackupPayload.serializer(), payload)
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            ?: return BackupResult.Failure("ফাইলটি তৈরি করা যায়নি।")
        db.backupMetaDao().insert(
            com.shohankhan.bokeya.data.db.BackupMetaEntity(
                schemaVersion = BackupPayload.SCHEMA_VERSION,
                createdAt = Clocks.nowMillis(),
                fileName = defaultFileName(),
                recordCount = payload.recordCount,
            ),
        )
        BackupResult.Success(defaultFileName(), payload.recordCount)
    }.getOrElse { BackupResult.Failure("Backup সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।") }

    /** Reads and validates a backup file without writing anything to the database. */
    fun preview(uri: Uri): RestoreResult = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return RestoreResult.Failure("ফাইলটি পড়া যায়নি।")
        val payload = json.decodeFromString(BackupPayload.serializer(), text)
        if (payload.schemaVersion > BackupPayload.SCHEMA_VERSION) {
            return RestoreResult.Failure("এই backup ফাইলটি নতুন version-এর। App update করুন।")
        }
        RestoreResult.Preview(payload)
    }.getOrElse { RestoreResult.Failure("Backup ফাইলটি সঠিক নয়।") }

    /** Destructive by design: replaces all current data with the backup contents. */
    suspend fun restore(payload: BackupPayload): RestoreResult = runCatching {
        db.maintenanceDao().wipeAll()
        payload.people.forEach { db.personDao().insert(it.toEntity()) }
        payload.categories.forEach { db.categoryDao().insert(it.toEntity()) }
        payload.accounts.forEach { db.accountDao().insert(it.toEntity()) }
        payload.purchases.forEach { db.shopDao().insertPurchase(it.toEntity()) }
        payload.items.forEach { db.shopDao().insertItem(it.toEntity()) }
        db.installmentDao().insertAll(payload.installments.map { it.toEntity() })
        payload.payments.forEach { db.paymentDao().insert(it.toEntity()) }
        payload.transactions.forEach { db.transactionDao().insert(it.toEntity()) }
        payload.recurring.forEach { db.recurringDao().insert(it.toEntity()) }
        payload.reminders.forEach { db.reminderDao().insert(it.toEntity()) }
        payload.goals.forEach { db.goalDao().insert(it.toEntity()) }
        RestoreResult.Success(payload.recordCount)
    }.getOrElse { RestoreResult.Failure("Restore সম্পূর্ণ করা যায়নি।") }

    fun defaultFileName(): String {
        val today = Clocks.today()
        return "bokeya-backup-${today}.json"
    }
}

// ---------------------------------------------------------------- mappers

private fun AccountEntity.toDto() = AccountDto(
    id, type.name, direction.name, title, subtitle, principal, interestAmount, downPayment,
    installmentAmount, installmentCount, frequency?.name, customIntervalDays, startDate, endDate,
    dueDate, personId, institution, brand, seller, paymentMethod?.name, note, archived, paused,
    createdAt, updatedAt,
)

private fun AccountDto.toEntity() = AccountEntity(
    id = id,
    type = com.shohankhan.bokeya.domain.AccountType.valueOf(type),
    direction = com.shohankhan.bokeya.domain.Direction.valueOf(direction),
    title = title, subtitle = subtitle, principal = principal, interestAmount = interestAmount,
    downPayment = downPayment, installmentAmount = installmentAmount, installmentCount = installmentCount,
    frequency = frequency?.let { com.shohankhan.bokeya.core.Frequency.valueOf(it) },
    customIntervalDays = customIntervalDays, startDate = startDate, endDate = endDate, dueDate = dueDate,
    personId = personId, institution = institution, brand = brand, seller = seller,
    paymentMethod = paymentMethod?.let { com.shohankhan.bokeya.domain.PaymentMethod.valueOf(it) },
    note = note, archived = archived, paused = paused, createdAt = createdAt, updatedAt = updatedAt,
)

private fun PersonEntity.toDto() = PersonDto(id, name, relationship.name, phone, note, createdAt, updatedAt)
private fun PersonDto.toEntity() = PersonEntity(
    id, name, com.shohankhan.bokeya.domain.Relationship.valueOf(relationship), phone, note, createdAt, updatedAt,
)

private fun ShopPurchaseEntity.toDto() = PurchaseDto(id, accountId, date, timeMinutes, note, createdAt)
private fun PurchaseDto.toEntity() = ShopPurchaseEntity(id, accountId, date, timeMinutes, note, createdAt)

private fun ShopItemEntity.toDto() = ItemDto(id, purchaseId, name, quantity, unit, unitPrice, total, note)
private fun ItemDto.toEntity() = ShopItemEntity(id, purchaseId, name, quantity, unit, unitPrice, total, note)

private fun InstallmentEntity.toDto() =
    InstallmentDto(id, accountId, number, dueDate, expectedAmount, paidAmount, status.name, paidAt)

private fun InstallmentDto.toEntity() = InstallmentEntity(
    id, accountId, number, dueDate, expectedAmount, paidAmount,
    com.shohankhan.bokeya.domain.InstallmentStatus.valueOf(status), paidAt,
)

private fun PaymentEntity.toDto() =
    PaymentDto(id, accountId, installmentId, amount, date, timeMinutes, method.name, note, createdAt)

private fun PaymentDto.toEntity() = PaymentEntity(
    id, accountId, installmentId, amount, date, timeMinutes,
    com.shohankhan.bokeya.domain.PaymentMethod.valueOf(method), note, createdAt,
)

private fun TransactionEntity.toDto() = TransactionDto(
    id, type.name, amount, date, timeMinutes, title, categoryId, accountId, personId,
    method?.name, note, createdAt, updatedAt,
)

private fun TransactionDto.toEntity() = TransactionEntity(
    id, TxType.valueOf(type), amount, date, timeMinutes, title, categoryId, accountId, personId,
    method?.let { com.shohankhan.bokeya.domain.PaymentMethod.valueOf(it) }, note, createdAt, updatedAt,
)

private fun CategoryEntity.toDto() = CategoryDto(id, name, kind.name, icon, colorArgb, builtIn, sortOrder)
private fun CategoryDto.toEntity() = CategoryEntity(
    id, name, com.shohankhan.bokeya.domain.CategoryKind.valueOf(kind), icon, colorArgb, builtIn, sortOrder,
)

private fun RecurringEntity.toDto() = RecurringDto(
    id, title, type.name, amount, categoryId, accountId, frequency.name, customIntervalDays,
    anchorDate, nextRun, autoCreate, active, note, createdAt,
)

private fun RecurringDto.toEntity() = RecurringEntity(
    id, title, TxType.valueOf(type), amount, categoryId, accountId,
    com.shohankhan.bokeya.core.Frequency.valueOf(frequency), customIntervalDays, anchorDate,
    nextRun, autoCreate, active, note, createdAt,
)

private fun ReminderEntity.toDto() =
    ReminderDto(id, accountId, installmentId, title, message, triggerAt, enabled, fired, createdAt)

private fun ReminderDto.toEntity() =
    ReminderEntity(id, accountId, installmentId, title, message, triggerAt, enabled, fired, createdAt)

private fun GoalEntity.toDto() = GoalDto(
    id, title, targetAmount, savedAmount, kind, linkedAccountId, targetDate, note, archived,
    createdAt, updatedAt,
)

private fun GoalDto.toEntity() = GoalEntity(
    id, title, targetAmount, savedAmount, kind, linkedAccountId, targetDate, note, archived,
    createdAt, updatedAt,
)
