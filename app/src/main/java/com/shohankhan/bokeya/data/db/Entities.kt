package com.shohankhan.bokeya.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.InstallmentStatus
import com.shohankhan.bokeya.domain.PaymentMethod
import com.shohankhan.bokeya.domain.Relationship
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.core.Frequency

/**
 * Single obligation table backing shops, loans, EMIs and personal debt.
 * Type-specific fields are nullable columns rather than separate tables, which keeps
 * dashboard/report queries to a single scan without sacrificing normalization of the
 * child tables (installments, items, payments).
 */
@Entity(
    tableName = "accounts",
    indices = [
        Index("type"), Index("direction"), Index("archived"), Index("personId"),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AccountType,
    val direction: Direction,
    val title: String,
    val subtitle: String? = null,
    /** Total obligation in poisha. For shops this is recomputed from purchases. */
    val principal: Long = 0,
    val interestAmount: Long = 0,
    val downPayment: Long = 0,
    val installmentAmount: Long = 0,
    val installmentCount: Int = 0,
    val frequency: Frequency? = null,
    val customIntervalDays: Int = 0,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val dueDate: Long? = null,
    val personId: Long? = null,
    val institution: String? = null,
    val brand: String? = null,
    val seller: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val note: String? = null,
    val archived: Boolean = false,
    val paused: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "people", indices = [Index(value = ["name"])])
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: Relationship = Relationship.OTHER,
    val phone: String? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "shop_purchases",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index("date")],
)
data class ShopPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val date: Long,
    val timeMinutes: Int = 0,
    val note: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "shop_items",
    foreignKeys = [
        ForeignKey(
            entity = ShopPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("purchaseId")],
)
data class ShopItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "পিস",
    val unitPrice: Long = 0,
    val total: Long = 0,
    val note: String? = null,
)

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index("dueDate"), Index("status")],
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val number: Int,
    val dueDate: Long,
    val expectedAmount: Long,
    val paidAmount: Long = 0,
    val status: InstallmentStatus = InstallmentStatus.UPCOMING,
    val paidAt: Long? = null,
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId"), Index("date"), Index("installmentId")],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val installmentId: Long? = null,
    val amount: Long,
    val date: Long,
    val timeMinutes: Int = 0,
    val method: PaymentMethod = PaymentMethod.CASH,
    val note: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("type"), Index("categoryId"), Index("accountId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val date: Long,
    val timeMinutes: Int = 0,
    val title: String,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val personId: Long? = null,
    val method: PaymentMethod? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "categories", indices = [Index(value = ["name", "kind"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val icon: String = "wallet",
    val colorArgb: Int = 0,
    val builtIn: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "recurring", indices = [Index("nextRun"), Index("active")])
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TxType,
    val amount: Long,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val frequency: Frequency,
    val customIntervalDays: Int = 0,
    val anchorDate: Long,
    val nextRun: Long,
    val autoCreate: Boolean = false,
    val active: Boolean = true,
    val note: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "reminders", indices = [Index("triggerAt"), Index("accountId")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long? = null,
    val installmentId: Long? = null,
    val title: String,
    val message: String,
    val triggerAt: Long,
    val enabled: Boolean = true,
    val fired: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Long,
    val savedAmount: Long = 0,
    val kind: String = "SAVING",
    val linkedAccountId: Long? = null,
    val targetDate: Long? = null,
    val note: String? = null,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "backup_meta")
data class BackupMetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schemaVersion: Int,
    val createdAt: Long,
    val fileName: String,
    val recordCount: Int,
)

/** Denormalized row used by the unified timeline / calendar queries. */
data class TimelineRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "timeMinutes") val timeMinutes: Int,
    @ColumnInfo(name = "note") val note: String?,
)

data class AccountAggregate(
    @ColumnInfo(name = "accountId") val accountId: Long,
    @ColumnInfo(name = "paid") val paid: Long,
)

data class CategoryTotal(
    @ColumnInfo(name = "categoryId") val categoryId: Long?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "total") val total: Long,
)
