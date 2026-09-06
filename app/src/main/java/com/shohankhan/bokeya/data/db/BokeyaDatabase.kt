package com.shohankhan.bokeya.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.InstallmentStatus
import com.shohankhan.bokeya.domain.PaymentMethod
import com.shohankhan.bokeya.domain.Relationship
import com.shohankhan.bokeya.domain.TxType

class Converters {
    @TypeConverter fun accountType(value: AccountType?): String? = value?.name
    @TypeConverter fun toAccountType(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @TypeConverter fun direction(value: Direction?): String? = value?.name
    @TypeConverter fun toDirection(value: String?): Direction? = value?.let { Direction.valueOf(it) }

    @TypeConverter fun frequency(value: Frequency?): String? = value?.name
    @TypeConverter fun toFrequency(value: String?): Frequency? = value?.let { Frequency.valueOf(it) }

    @TypeConverter fun txType(value: TxType?): String? = value?.name
    @TypeConverter fun toTxType(value: String?): TxType? = value?.let { TxType.valueOf(it) }

    @TypeConverter fun method(value: PaymentMethod?): String? = value?.name
    @TypeConverter fun toMethod(value: String?): PaymentMethod? = value?.let { PaymentMethod.valueOf(it) }

    @TypeConverter fun status(value: InstallmentStatus?): String? = value?.name
    @TypeConverter fun toStatus(value: String?): InstallmentStatus? = value?.let { InstallmentStatus.valueOf(it) }

    @TypeConverter fun relationship(value: Relationship?): String? = value?.name
    @TypeConverter fun toRelationship(value: String?): Relationship? = value?.let { Relationship.valueOf(it) }

    @TypeConverter fun categoryKind(value: CategoryKind?): String? = value?.name
    @TypeConverter fun toCategoryKind(value: String?): CategoryKind? = value?.let { CategoryKind.valueOf(it) }
}

@Database(
    entities = [
        AccountEntity::class,
        PersonEntity::class,
        ShopPurchaseEntity::class,
        ShopItemEntity::class,
        InstallmentEntity::class,
        PaymentEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringEntity::class,
        ReminderEntity::class,
        GoalEntity::class,
        BackupMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class BokeyaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun personDao(): PersonDao
    abstract fun shopDao(): ShopDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringDao(): RecurringDao
    abstract fun reminderDao(): ReminderDao
    abstract fun goalDao(): GoalDao
    abstract fun backupMetaDao(): BackupMetaDao
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        const val NAME = "bokeya.db"

        /**
         * Migrations are declared explicitly; destructive fallback is intentionally not
         * enabled because it would silently destroy a user's financial history.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()

        @Volatile private var instance: BokeyaDatabase? = null

        fun get(context: Context): BokeyaDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): BokeyaDatabase =
            Room.databaseBuilder(context, BokeyaDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL("PRAGMA foreign_keys=ON")
                    }
                })
                .build()
    }
}
