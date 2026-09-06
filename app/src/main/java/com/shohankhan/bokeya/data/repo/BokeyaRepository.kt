package com.shohankhan.bokeya.data.repo

import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.core.ScheduleEngine
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
import com.shohankhan.bokeya.domain.AccountStatus
import com.shohankhan.bokeya.domain.AccountSummary
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.InstallmentStatus
import com.shohankhan.bokeya.domain.PaymentMethod
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.domain.UpcomingPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * Single source of truth for all financial state. All obligation maths (remaining balance,
 * partial payments, installment status, schedule generation) lives here rather than in the UI.
 */
class BokeyaRepository(private val db: BokeyaDatabase) {

    private val accountDao = db.accountDao()
    private val personDao = db.personDao()
    private val shopDao = db.shopDao()
    private val installmentDao = db.installmentDao()
    private val paymentDao = db.paymentDao()
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val recurringDao = db.recurringDao()
    private val reminderDao = db.reminderDao()
    private val goalDao = db.goalDao()

    // ------------------------------------------------------------------ accounts

    val accounts: Flow<List<AccountEntity>> = accountDao.observeAll()
    val people: Flow<List<PersonEntity>> = personDao.observeAll()
    val goals: Flow<List<GoalEntity>> = goalDao.observeActive()
    val recurring: Flow<List<RecurringEntity>> = recurringDao.observeAll()
    val reminders: Flow<List<ReminderEntity>> = reminderDao.observeAll()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.observeAll()
    val allInstallments: Flow<List<InstallmentEntity>> = installmentDao.observeAll()

    fun expenseCategories(): Flow<List<CategoryEntity>> = categoryDao.observeByKind(CategoryKind.EXPENSE)
    fun incomeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeByKind(CategoryKind.INCOME)
    fun allCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeAccount(id: Long): Flow<AccountEntity?> = accountDao.observeById(id)
    fun observeInstallments(id: Long): Flow<List<InstallmentEntity>> = installmentDao.observeFor(id)
    fun observePayments(id: Long): Flow<List<PaymentEntity>> = paymentDao.observeFor(id)
    fun observePurchases(id: Long): Flow<List<ShopPurchaseEntity>> = shopDao.observePurchases(id)

    suspend fun purchaseItems(purchaseId: Long): List<ShopItemEntity> = shopDao.items(purchaseId)

    /**
     * Summaries combine account rows with their payment totals and next-due installment.
     * Shop totals are derived from item lines so the two can never disagree.
     */
    val summaries: Flow<List<AccountSummary>> = combine(
        accountDao.observeAll(),
        paymentDao.observeTotals(),
        installmentDao.observeAll(),
    ) { accounts, totals, installments ->
        val paidByAccount = totals.associate { it.accountId to it.paid }
        val installmentsByAccount = installments.groupBy { it.accountId }
        val today = Clocks.today()
        accounts.map { account ->
            summarize(account, paidByAccount[account.id] ?: 0L, installmentsByAccount[account.id].orEmpty(), today)
        }
    }

    fun summarize(
        account: AccountEntity,
        paidPoisha: Long,
        installments: List<InstallmentEntity>,
        today: LocalDate,
    ): AccountSummary {
        val total = Money(account.principal + account.interestAmount)
        val paid = Money(paidPoisha)
        val remaining = (total - paid).clampAtZero()
        val nextInstallment = installments
            .filter { it.status != InstallmentStatus.PAID }
            .minByOrNull { it.dueDate }
        val nextDate = nextInstallment?.let { LocalDate.ofEpochDay(it.dueDate) }
            ?: account.dueDate?.let { LocalDate.ofEpochDay(it) }?.takeIf { !remaining.isZero }
        val nextAmount = nextInstallment
            ?.let { Money(it.expectedAmount - it.paidAmount).clampAtZero() }
            ?: remaining.takeIf { !it.isZero }

        val status = when {
            account.archived -> AccountStatus.ARCHIVED
            remaining.isZero && total.isPositive -> AccountStatus.PAID
            account.paused -> AccountStatus.PAUSED
            nextDate != null && nextDate.isBefore(today) -> AccountStatus.OVERDUE
            nextDate != null && nextDate == today -> AccountStatus.DUE_TODAY
            nextDate != null && !nextDate.isAfter(today.plusDays(7)) -> AccountStatus.DUE_SOON
            paid.isPositive -> AccountStatus.PARTIALLY_PAID
            else -> AccountStatus.ACTIVE
        }

        return AccountSummary(
            id = account.id,
            type = account.type,
            direction = account.direction,
            title = account.title,
            subtitle = account.subtitle ?: account.institution ?: account.brand,
            total = total,
            paid = paid,
            remaining = remaining,
            nextDueDate = nextDate,
            nextDueAmount = nextAmount,
            status = status,
            archived = account.archived,
        )
    }

    suspend fun summaryOnce(accountId: Long): AccountSummary? {
        val account = accountDao.byId(accountId) ?: return null
        return summarize(
            account,
            paymentDao.totalFor(accountId),
            installmentDao.forAccount(accountId),
            Clocks.today(),
        )
    }

    suspend fun account(id: Long): AccountEntity? = accountDao.byId(id)

    suspend fun createAccount(account: AccountEntity): Long {
        val now = Clocks.nowMillis()
        val id = accountDao.insert(account.copy(createdAt = now, updatedAt = now))
        if (account.installmentCount > 0 && account.frequency != null && account.startDate != null) {
            regenerateSchedule(id)
        }
        return id
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.update(account.copy(updatedAt = Clocks.nowMillis()))
    }

    suspend fun deleteAccount(id: Long) = accountDao.deleteById(id)

    suspend fun setArchived(id: Long, archived: Boolean) =
        accountDao.setArchived(id, archived, Clocks.nowMillis())

    suspend fun setPaused(id: Long, paused: Boolean) =
        accountDao.setPaused(id, paused, Clocks.nowMillis())

    /** (Re)builds the installment plan for a loan/EMI from its own schedule fields. */
    suspend fun regenerateSchedule(accountId: Long) {
        val account = accountDao.byId(accountId) ?: return
        val start = account.startDate?.let { LocalDate.ofEpochDay(it) } ?: return
        val frequency = account.frequency ?: return
        val count = account.installmentCount
        if (count <= 0) return

        val financed = Money(account.principal + account.interestAmount)
        val plan = if (account.installmentAmount > 0) {
            ScheduleEngine.generateDueDates(start, frequency, count, account.customIntervalDays)
                .mapIndexed { index, date ->
                    val amount = if (index == count - 1) {
                        Money(financed.poisha - account.installmentAmount * (count - 1)).clampAtZero()
                            .takeIf { it.isPositive } ?: Money(account.installmentAmount)
                    } else {
                        Money(account.installmentAmount)
                    }
                    InstallmentEntity(
                        accountId = accountId,
                        number = index + 1,
                        dueDate = date.toEpochDay(),
                        expectedAmount = amount.poisha,
                    )
                }
        } else {
            ScheduleEngine.installmentPlan(financed, count, start, frequency, account.customIntervalDays)
                .map {
                    InstallmentEntity(
                        accountId = accountId,
                        number = it.number,
                        dueDate = it.dueDate.toEpochDay(),
                        expectedAmount = it.amount.poisha,
                    )
                }
        }

        installmentDao.deleteForAccount(accountId)
        installmentDao.insertAll(plan)
        applyPaymentsToSchedule(accountId)
        refreshInstallmentStatuses(accountId)
    }

    // ------------------------------------------------------------------ payments

    /**
     * Records a payment and cascades it across the installment schedule oldest-first.
     * Overpayment is capped at the outstanding balance so remaining never goes negative.
     */
    suspend fun recordPayment(
        accountId: Long,
        amount: Money,
        date: LocalDate = Clocks.today(),
        method: PaymentMethod = PaymentMethod.CASH,
        note: String? = null,
    ): PaymentResult {
        val account = accountDao.byId(accountId) ?: return PaymentResult.NotFound
        if (!amount.isPositive) return PaymentResult.InvalidAmount

        val total = Money(account.principal + account.interestAmount)
        val alreadyPaid = Money(paymentDao.totalFor(accountId))
        val outstanding = (total - alreadyPaid).clampAtZero()
        if (outstanding.isZero) return PaymentResult.AlreadySettled

        val applied = if (amount > outstanding) outstanding else amount
        val now = Clocks.nowMillis()
        val time = LocalTime.now()

        paymentDao.insert(
            PaymentEntity(
                accountId = accountId,
                amount = applied.poisha,
                date = date.toEpochDay(),
                timeMinutes = time.hour * 60 + time.minute,
                method = method,
                note = note,
                createdAt = now,
            ),
        )

        transactionDao.insert(
            TransactionEntity(
                type = when (account.type) {
                    AccountType.LOAN -> TxType.LOAN_PAYMENT
                    AccountType.EMI -> TxType.EMI_PAYMENT
                    else -> if (account.direction == Direction.I_OWE) TxType.PAYMENT else TxType.RECEIVED
                },
                amount = applied.poisha,
                date = date.toEpochDay(),
                timeMinutes = time.hour * 60 + time.minute,
                title = account.title,
                accountId = accountId,
                personId = account.personId,
                method = method,
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )

        applyPaymentsToSchedule(accountId)
        refreshInstallmentStatuses(accountId)

        val remaining = (outstanding - applied).clampAtZero()
        if (remaining.isZero) {
            accountDao.setArchived(accountId, false, now)
        }
        accountDao.update(account.copy(updatedAt = now))

        return PaymentResult.Success(
            applied = applied,
            remaining = remaining,
            fullyPaid = remaining.isZero,
            overpaymentTrimmed = amount > outstanding,
        )
    }

    suspend fun deletePayment(paymentId: Long, accountId: Long) {
        paymentDao.deleteById(paymentId)
        applyPaymentsToSchedule(accountId)
        refreshInstallmentStatuses(accountId)
    }

    /** Distributes the account's total paid amount across installments oldest-first. */
    private suspend fun applyPaymentsToSchedule(accountId: Long) {
        val installments = installmentDao.forAccount(accountId).sortedBy { it.number }
        if (installments.isEmpty()) return
        var pool = paymentDao.totalFor(accountId)
        for (installment in installments) {
            val applied = minOf(pool, installment.expectedAmount).coerceAtLeast(0L)
            pool -= applied
            if (installment.paidAmount != applied) {
                installmentDao.update(installment.copy(paidAmount = applied))
            }
        }
    }

    suspend fun refreshInstallmentStatuses(accountId: Long, today: LocalDate = Clocks.today()) {
        installmentDao.forAccount(accountId).forEach { installment ->
            val status = installmentStatus(installment, today)
            if (installment.status != status) {
                installmentDao.update(installment.copy(status = status))
            }
        }
    }

    fun installmentStatus(installment: InstallmentEntity, today: LocalDate): InstallmentStatus {
        val due = LocalDate.ofEpochDay(installment.dueDate)
        return when {
            installment.paidAmount >= installment.expectedAmount -> InstallmentStatus.PAID
            due.isBefore(today) -> InstallmentStatus.OVERDUE
            installment.paidAmount > 0 -> InstallmentStatus.PARTIALLY_PAID
            due == today -> InstallmentStatus.DUE_TODAY
            else -> InstallmentStatus.UPCOMING
        }
    }

    // ------------------------------------------------------------------ shops

    suspend fun addPurchase(
        accountId: Long,
        items: List<ShopItemEntity>,
        date: LocalDate = Clocks.today(),
        note: String? = null,
    ): Long {
        val now = Clocks.nowMillis()
        val time = LocalTime.now()
        val purchaseId = shopDao.insertPurchase(
            ShopPurchaseEntity(
                accountId = accountId,
                date = date.toEpochDay(),
                timeMinutes = time.hour * 60 + time.minute,
                note = note,
                createdAt = now,
            ),
        )
        shopDao.insertItems(items.map { it.copy(purchaseId = purchaseId) })
        syncShopTotal(accountId)

        val purchaseTotal = items.sumOf { it.total }
        transactionDao.insert(
            TransactionEntity(
                type = TxType.SHOP_PURCHASE,
                amount = purchaseTotal,
                date = date.toEpochDay(),
                timeMinutes = time.hour * 60 + time.minute,
                title = accountDao.byId(accountId)?.title ?: "দোকান",
                accountId = accountId,
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return purchaseId
    }

    suspend fun deletePurchase(purchaseId: Long, accountId: Long) {
        shopDao.deletePurchase(purchaseId)
        syncShopTotal(accountId)
    }

    /** Shop principal is always the sum of its item lines. */
    private suspend fun syncShopTotal(accountId: Long) {
        val account = accountDao.byId(accountId) ?: return
        val total = shopDao.purchaseTotal(accountId)
        accountDao.update(account.copy(principal = total, updatedAt = Clocks.nowMillis()))
    }

    // ------------------------------------------------------------------ people

    suspend fun upsertPerson(person: PersonEntity): Long {
        val now = Clocks.nowMillis()
        return if (person.id == 0L) {
            personDao.insert(person.copy(createdAt = now, updatedAt = now))
        } else {
            personDao.update(person.copy(updatedAt = now))
            person.id
        }
    }

    suspend fun person(id: Long): PersonEntity? = personDao.byId(id)
    suspend fun deletePerson(id: Long) = personDao.deleteById(id)

    // ------------------------------------------------------------------ transactions

    suspend fun addTransaction(
        type: TxType,
        amount: Money,
        title: String,
        date: LocalDate = Clocks.today(),
        categoryId: Long? = null,
        method: PaymentMethod? = null,
        note: String? = null,
        accountId: Long? = null,
        personId: Long? = null,
    ): Long {
        val now = Clocks.nowMillis()
        val time = LocalTime.now()
        return transactionDao.insert(
            TransactionEntity(
                type = type,
                amount = amount.poisha,
                date = date.toEpochDay(),
                timeMinutes = time.hour * 60 + time.minute,
                title = title,
                categoryId = categoryId,
                accountId = accountId,
                personId = personId,
                method = method,
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateTransaction(tx: TransactionEntity) =
        transactionDao.update(tx.copy(updatedAt = Clocks.nowMillis()))

    suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)
    suspend fun deleteTransactions(ids: List<Long>) = transactionDao.deleteByIds(ids)
    suspend fun transaction(id: Long): TransactionEntity? = transactionDao.byId(id)

    fun recentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> =
        transactionDao.observeRecent(limit)

    fun transactionsBetween(from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(from.toEpochDay(), to.toEpochDay())

    suspend fun transactionsBetweenOnce(from: LocalDate, to: LocalDate): List<TransactionEntity> =
        transactionDao.between(from.toEpochDay(), to.toEpochDay())

    suspend fun categoryTotals(type: TxType, from: LocalDate, to: LocalDate) =
        transactionDao.categoryTotals(type.name, from.toEpochDay(), to.toEpochDay())

    suspend fun suggestCategory(title: String): Long? =
        title.takeIf { it.length >= 2 }?.let { transactionDao.suggestCategory(it) }

    // ------------------------------------------------------------------ categories

    suspend fun addCategory(name: String, kind: CategoryKind): Long =
        categoryDao.insert(CategoryEntity(name = name, kind = kind, builtIn = false, sortOrder = 999))

    suspend fun deleteCategory(id: Long) = categoryDao.deleteCustom(id)
    suspend fun category(id: Long): CategoryEntity? = categoryDao.byId(id)

    // ------------------------------------------------------------------ goals

    suspend fun upsertGoal(goal: GoalEntity): Long {
        val now = Clocks.nowMillis()
        return if (goal.id == 0L) {
            goalDao.insert(goal.copy(createdAt = now, updatedAt = now))
        } else {
            goalDao.update(goal.copy(updatedAt = now))
            goal.id
        }
    }

    suspend fun addToGoal(goalId: Long, amount: Money) {
        val goal = goalDao.byId(goalId) ?: return
        goalDao.update(
            goal.copy(
                savedAmount = (goal.savedAmount + amount.poisha).coerceAtLeast(0L),
                updatedAt = Clocks.nowMillis(),
            ),
        )
    }

    suspend fun deleteGoal(id: Long) = goalDao.deleteById(id)

    // ------------------------------------------------------------------ recurring

    suspend fun upsertRecurring(item: RecurringEntity): Long =
        if (item.id == 0L) recurringDao.insert(item.copy(createdAt = Clocks.nowMillis()))
        else { recurringDao.update(item); item.id }

    suspend fun deleteRecurring(id: Long) = recurringDao.deleteById(id)

    /** Materializes any recurring rules whose next run date has arrived. */
    suspend fun runDueRecurring(today: LocalDate = Clocks.today()): Int {
        val due = recurringDao.dueBy(today.toEpochDay())
        var created = 0
        for (rule in due) {
            var next = LocalDate.ofEpochDay(rule.nextRun)
            while (!next.isAfter(today)) {
                if (rule.autoCreate) {
                    addTransaction(
                        type = rule.type,
                        amount = Money(rule.amount),
                        title = rule.title,
                        date = next,
                        categoryId = rule.categoryId,
                        accountId = rule.accountId,
                        note = rule.note,
                    )
                    created++
                }
                next = ScheduleEngine.nextOccurrenceAfter(
                    LocalDate.ofEpochDay(rule.anchorDate),
                    rule.frequency,
                    next,
                    rule.customIntervalDays,
                )
            }
            recurringDao.update(rule.copy(nextRun = next.toEpochDay()))
        }
        return created
    }

    // ------------------------------------------------------------------ reminders

    suspend fun upsertReminder(reminder: ReminderEntity): Long =
        if (reminder.id == 0L) reminderDao.insert(reminder.copy(createdAt = Clocks.nowMillis()))
        else { reminderDao.update(reminder); reminder.id }

    suspend fun pendingReminders(now: Long = Clocks.nowMillis()) = reminderDao.pending(now)
    suspend fun markReminderFired(id: Long) = reminderDao.markFired(id)
    suspend fun snoozeReminder(id: Long, at: Long) = reminderDao.snooze(id, at)
    suspend fun deleteReminder(id: Long) = reminderDao.deleteById(id)

    // ------------------------------------------------------------------ schedules & search

    /** All unpaid obligations due within [from]..[to], sorted by urgency then amount. */
    suspend fun upcomingPayments(from: LocalDate, to: LocalDate): List<UpcomingPayment> {
        val accounts = accountDao.allOnce().filter { !it.archived && !it.paused }.associateBy { it.id }
        val result = mutableListOf<UpcomingPayment>()

        installmentDao.dueBetween(from.toEpochDay(), to.toEpochDay()).forEach { installment ->
            val account = accounts[installment.accountId] ?: return@forEach
            if (account.direction != Direction.I_OWE) return@forEach
            val remaining = Money(installment.expectedAmount - installment.paidAmount).clampAtZero()
            if (remaining.isZero) return@forEach
            result += UpcomingPayment(
                accountId = account.id,
                type = account.type,
                title = account.title,
                amount = remaining,
                dueDate = LocalDate.ofEpochDay(installment.dueDate),
                installmentId = installment.id,
            )
        }

        // Accounts without a generated schedule (shops, simple personal debt)
        accounts.values.forEach { account ->
            if (account.direction != Direction.I_OWE) return@forEach
            if (installmentDao.forAccount(account.id).isNotEmpty()) return@forEach
            val dueEpoch = account.dueDate ?: return@forEach
            if (dueEpoch < from.toEpochDay() || dueEpoch > to.toEpochDay()) return@forEach
            val remaining = Money(
                account.principal + account.interestAmount - paymentDao.totalFor(account.id),
            ).clampAtZero()
            if (remaining.isZero) return@forEach
            result += UpcomingPayment(
                accountId = account.id,
                type = account.type,
                title = account.title,
                amount = remaining,
                dueDate = LocalDate.ofEpochDay(dueEpoch),
                installmentId = null,
            )
        }

        return result.sortedWith(compareBy({ it.dueDate }, { -it.amount.poisha }))
    }

    suspend fun overduePayments(today: LocalDate = Clocks.today()): List<UpcomingPayment> =
        upcomingPayments(LocalDate.ofEpochDay(0), today.minusDays(1))

    suspend fun search(query: String): SearchResults {
        val q = query.trim()
        if (q.isBlank()) return SearchResults()
        return SearchResults(
            accounts = accountDao.search(q),
            people = personDao.search(q),
            transactions = transactionDao.search(q),
        )
    }

    suspend fun wipeAllData() = db.maintenanceDao().wipeAll()
}

sealed interface PaymentResult {
    data class Success(
        val applied: Money,
        val remaining: Money,
        val fullyPaid: Boolean,
        val overpaymentTrimmed: Boolean,
    ) : PaymentResult

    data object NotFound : PaymentResult
    data object InvalidAmount : PaymentResult
    data object AlreadySettled : PaymentResult
}

data class SearchResults(
    val accounts: List<AccountEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
) {
    val isEmpty: Boolean get() = accounts.isEmpty() && people.isEmpty() && transactions.isEmpty()
}
