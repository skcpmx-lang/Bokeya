package com.shohankhan.bokeya.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.InstallmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert suspend fun insert(account: AccountEntity): Long
    @Update suspend fun update(account: AccountEntity)
    @Delete suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE type = :type AND archived = 0 ORDER BY updatedAt DESC")
    fun observeByType(type: AccountType): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun allOnce(): List<AccountEntity>

    @Query("UPDATE accounts SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    @Query("UPDATE accounts SET paused = :paused, updatedAt = :now WHERE id = :id")
    suspend fun setPaused(id: Long, paused: Boolean, now: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM accounts WHERE archived = 0 AND title LIKE '%' || :q || '%'")
    suspend fun search(q: String): List<AccountEntity>
}

@Dao
interface PersonDao {
    @Insert suspend fun insert(person: PersonEntity): Long
    @Update suspend fun update(person: PersonEntity)

    @Query("SELECT * FROM people ORDER BY name")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people")
    suspend fun allOnce(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun byId(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): PersonEntity?

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM people WHERE name LIKE '%' || :q || '%'")
    suspend fun search(q: String): List<PersonEntity>
}

@Dao
interface ShopDao {
    @Insert suspend fun insertPurchase(purchase: ShopPurchaseEntity): Long
    @Insert suspend fun insertItems(items: List<ShopItemEntity>)
    @Insert suspend fun insertItem(item: ShopItemEntity): Long

    @Query("SELECT * FROM shop_purchases WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun observePurchases(accountId: Long): Flow<List<ShopPurchaseEntity>>

    @Query("SELECT * FROM shop_purchases")
    suspend fun allPurchases(): List<ShopPurchaseEntity>

    @Query("SELECT * FROM shop_items WHERE purchaseId = :purchaseId")
    suspend fun items(purchaseId: Long): List<ShopItemEntity>

    @Query("SELECT * FROM shop_items WHERE purchaseId IN (:ids)")
    fun observeItemsFor(ids: List<Long>): Flow<List<ShopItemEntity>>

    @Query("SELECT * FROM shop_items")
    suspend fun allItems(): List<ShopItemEntity>

    @Query(
        "SELECT COALESCE(SUM(i.total), 0) FROM shop_items i " +
            "INNER JOIN shop_purchases p ON p.id = i.purchaseId WHERE p.accountId = :accountId",
    )
    suspend fun purchaseTotal(accountId: Long): Long

    @Query("DELETE FROM shop_purchases WHERE id = :id")
    suspend fun deletePurchase(id: Long)
}

@Dao
interface InstallmentDao {
    @Insert suspend fun insertAll(items: List<InstallmentEntity>)
    @Insert suspend fun insert(item: InstallmentEntity): Long
    @Update suspend fun update(item: InstallmentEntity)

    @Query("SELECT * FROM installments WHERE accountId = :accountId ORDER BY number")
    fun observeFor(accountId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE accountId = :accountId ORDER BY number")
    suspend fun forAccount(accountId: Long): List<InstallmentEntity>

    @Query("SELECT * FROM installments")
    fun observeAll(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments")
    suspend fun allOnce(): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun byId(id: Long): InstallmentEntity?

    @Query(
        "SELECT * FROM installments WHERE status != 'PAID' AND dueDate BETWEEN :from AND :to " +
            "ORDER BY dueDate",
    )
    suspend fun dueBetween(from: Long, to: Long): List<InstallmentEntity>

    @Query("DELETE FROM installments WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: Long)

    @Query("UPDATE installments SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: InstallmentStatus)
}

@Dao
interface PaymentDao {
    @Insert suspend fun insert(payment: PaymentEntity): Long
    @Delete suspend fun delete(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun observeFor(accountId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments")
    suspend fun allOnce(): List<PaymentEntity>

    @Query("SELECT accountId AS accountId, COALESCE(SUM(amount),0) AS paid FROM payments GROUP BY accountId")
    fun observeTotals(): Flow<List<AccountAggregate>>

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments WHERE accountId = :accountId")
    suspend fun totalFor(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments WHERE date BETWEEN :from AND :to")
    fun observeTotalBetween(from: Long, to: Long): Flow<Long>

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TransactionDao {
    @Insert suspend fun insert(tx: TransactionEntity): Long
    @Update suspend fun update(tx: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC, timeMinutes DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, timeMinutes DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun allOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :from AND :to")
    suspend fun between(from: Long, to: Long): List<TransactionEntity>

    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type = :type AND date BETWEEN :from AND :to")
    suspend fun sumByType(type: String, from: Long, to: Long): Long

    @Query(
        "SELECT t.categoryId AS categoryId, c.name AS name, COALESCE(SUM(t.amount),0) AS total " +
            "FROM transactions t LEFT JOIN categories c ON c.id = t.categoryId " +
            "WHERE t.type = :type AND t.date BETWEEN :from AND :to GROUP BY t.categoryId ORDER BY total DESC",
    )
    suspend fun categoryTotals(type: String, from: Long, to: Long): List<CategoryTotal>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query(
        "SELECT * FROM transactions WHERE title LIKE '%' || :q || '%' OR note LIKE '%' || :q || '%' " +
            "ORDER BY date DESC LIMIT 100",
    )
    suspend fun search(q: String): List<TransactionEntity>

    @Query(
        "SELECT categoryId FROM transactions WHERE title LIKE '%' || :q || '%' AND categoryId IS NOT NULL " +
            "GROUP BY categoryId ORDER BY COUNT(*) DESC LIMIT 1",
    )
    suspend fun suggestCategory(q: String): Long?
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY sortOrder, name")
    fun observeByKind(kind: CategoryKind): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY kind, sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun allOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("DELETE FROM categories WHERE id = :id AND builtIn = 0")
    suspend fun deleteCustom(id: Long)
}

@Dao
interface RecurringDao {
    @Insert suspend fun insert(item: RecurringEntity): Long
    @Update suspend fun update(item: RecurringEntity)

    @Query("SELECT * FROM recurring ORDER BY nextRun")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE active = 1 AND nextRun <= :date")
    suspend fun dueBy(date: Long): List<RecurringEntity>

    @Query("SELECT * FROM recurring")
    suspend fun allOnce(): List<RecurringEntity>

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReminderDao {
    @Insert suspend fun insert(item: ReminderEntity): Long
    @Update suspend fun update(item: ReminderEntity)

    @Query("SELECT * FROM reminders ORDER BY triggerAt")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND fired = 0 AND triggerAt <= :now")
    suspend fun pending(now: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun allOnce(): List<ReminderEntity>

    @Query("UPDATE reminders SET fired = 1 WHERE id = :id")
    suspend fun markFired(id: Long)

    @Query("UPDATE reminders SET triggerAt = :at, fired = 0 WHERE id = :id")
    suspend fun snooze(id: Long, at: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface GoalDao {
    @Insert suspend fun insert(goal: GoalEntity): Long
    @Update suspend fun update(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun allOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun byId(id: Long): GoalEntity?

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BackupMetaDao {
    @Insert suspend fun insert(meta: BackupMetaEntity): Long

    @Query("SELECT * FROM backup_meta ORDER BY createdAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<BackupMetaEntity>>
}

@Dao
interface MaintenanceDao {
    @Transaction
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM payments") suspend fun clearPayments()
    @Query("DELETE FROM installments") suspend fun clearInstallments()
    @Query("DELETE FROM shop_items") suspend fun clearShopItems()
    @Query("DELETE FROM shop_purchases") suspend fun clearShopPurchases()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM people") suspend fun clearPeople()
    @Query("DELETE FROM recurring") suspend fun clearRecurring()
    @Query("DELETE FROM reminders") suspend fun clearReminders()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM categories WHERE builtIn = 0") suspend fun clearCustomCategories()

    @Transaction
    suspend fun wipeAll() {
        clearTransactions()
        clearPayments()
        clearInstallments()
        clearShopItems()
        clearShopPurchases()
        clearAccounts()
        clearPeople()
        clearRecurring()
        clearReminders()
        clearGoals()
    }
}
