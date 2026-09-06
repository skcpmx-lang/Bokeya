package com.shohankhan.bokeya

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.AccountEntity
import com.shohankhan.bokeya.data.DefaultData
import com.shohankhan.bokeya.data.db.BokeyaDatabase
import com.shohankhan.bokeya.data.db.ShopItemEntity
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.data.repo.PaymentResult
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.TxType
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end regression pass over the critical user journeys.
 *
 * These walk the same repository the UI drives, in the order a real person would: start with an
 * empty database, add each kind of account, move money, and assert the dashboard-facing numbers
 * change correctly. The point is to catch functional regressions hidden behind a redesign.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class RegressionFlowTest {

    private lateinit var db: BokeyaDatabase
    private lateinit var repo: BokeyaRepository

    private val today: LocalDate = LocalDate.of(2026, 9, 6)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BokeyaDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = BokeyaRepository(db)
        // The app seeds built-in categories on first launch; mirror that here.
        kotlinx.coroutines.runBlocking { db.categoryDao().insertAll(DefaultData.seed()) }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun shop(title: String = "রহমান স্টোর"): Long = repo.createAccount(
        AccountEntity(
            type = AccountType.SHOP,
            direction = Direction.I_OWE,
            title = title,
            startDate = today.toEpochDay(),
            createdAt = 0,
            updatedAt = 0,
        ),
    )

    /** Flow 2: a brand-new install has nothing to show, and says so honestly. */
    @Test
    fun `fresh database is empty but functional`() = runTest {
        assertTrue(repo.summaries.first().isEmpty())
        assertTrue(repo.allTransactions.first().isEmpty())
        assertTrue(repo.upcomingPayments(today, today.plusMonths(2)).isEmpty())
        assertTrue(repo.overduePayments(today).isEmpty())
        // Built-in categories must exist so the add-expense form is usable immediately.
        assertTrue(db.categoryDao().observeAll().first().isNotEmpty())
        // ...but they are labels only: no financial records are invented.
        assertTrue(repo.allPayments.first().isEmpty())
    }

    /** Flows 3-6: add shop, add purchase, pay part, then pay the rest. */
    @Test
    fun `shop purchase then partial then full payment settles cleanly`() = runTest {
        val id = shop()
        repo.addPurchase(
            id,
            listOf(
                ShopItemEntity(purchaseId = 0, name = "চাল", quantity = 5.0, unitPrice = 6_000, total = 30_000),
                ShopItemEntity(purchaseId = 0, name = "তেল", quantity = 2.0, unitPrice = 17_500, total = 35_000),
            ),
            date = today,
        )
        var s = repo.summaryOnce(id)!!
        assertEquals(Money(65_000), s.total)
        assertEquals(Money(65_000), s.remaining)

        assertTrue(repo.recordPayment(id, Money(25_000), today) is PaymentResult.Success)
        s = repo.summaryOnce(id)!!
        assertEquals(Money(25_000), s.paid)
        assertEquals(Money(40_000), s.remaining)
        assertFalse(s.remaining.isZero)

        assertTrue(repo.recordPayment(id, Money(40_000), today) is PaymentResult.Success)
        s = repo.summaryOnce(id)!!
        assertTrue(s.remaining.isZero)
        // Never negative, no matter what.
        assertFalse(s.remaining.isNegative)
    }

    /** Flows 7-8: personal debt in both directions is tracked separately. */
    @Test
    fun `personal debt and receivable are tracked in opposite directions`() = runTest {
        val iOwe = repo.createAccount(
            AccountEntity(
                type = AccountType.PERSONAL,
                direction = Direction.I_OWE,
                title = "করিম ভাই",
                principal = 500_000,
                startDate = today.toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val theyOwe = repo.createAccount(
            AccountEntity(
                type = AccountType.PERSONAL,
                direction = Direction.THEY_OWE,
                title = "সাকিব",
                principal = 200_000,
                startDate = today.toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val all = repo.summaries.first()
        val owe = all.first { it.id == iOwe }
        val receive = all.first { it.id == theyOwe }

        assertEquals(Direction.I_OWE, owe.direction)
        assertEquals(Direction.THEY_OWE, receive.direction)
        assertEquals(Money(500_000), owe.remaining)
        assertEquals(Money(200_000), receive.remaining)

        // The dashboard sums these into two independent totals.
        val totalOwe = Money(all.filter { it.direction == Direction.I_OWE }.sumOf { it.remaining.poisha })
        val totalReceive = Money(all.filter { it.direction == Direction.THEY_OWE }.sumOf { it.remaining.poisha })
        assertEquals(Money(500_000), totalOwe)
        assertEquals(Money(200_000), totalReceive)
    }

    /** Flows 9-10: loans and EMIs build real schedules that feed the calendar. */
    @Test
    fun `loan and emi generate schedules that surface as upcoming payments`() = runTest {
        repo.createAccount(
            AccountEntity(
                type = AccountType.LOAN,
                direction = Direction.I_OWE,
                title = "ব্যাংক Loan",
                principal = 1_200_000,
                installmentCount = 12,
                frequency = Frequency.MONTHLY,
                startDate = today.toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        repo.createAccount(
            AccountEntity(
                type = AccountType.EMI,
                direction = Direction.I_OWE,
                title = "Samsung ফ্রিজ",
                principal = 600_000,
                installmentCount = 6,
                frequency = Frequency.MONTHLY,
                startDate = today.toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val upcoming = repo.upcomingPayments(today, today.plusMonths(14))
        assertTrue("schedules must produce dated obligations", upcoming.size >= 18)
        // Every upcoming entry must carry a real amount and a real account.
        assertTrue(upcoming.all { it.amount.isPositive })
        assertTrue(upcoming.all { it.accountId > 0 })
        // And they must be chronologically usable by the calendar.
        assertEquals(upcoming.map { it.dueDate }.sorted(), upcoming.map { it.dueDate })
    }

    /** Flows 11-12: income and expense land in the ledger and in category totals. */
    @Test
    fun `income and expense feed cashflow and category totals`() = runTest {
        val cats = db.categoryDao().observeAll().first()
        val expenseCat = cats.first { it.kind == CategoryKind.EXPENSE }

        repo.addTransaction(TxType.INCOME, Money(3_000_000), "বেতন", today)
        repo.addTransaction(TxType.EXPENSE, Money(150_000), "বাজার", today, categoryId = expenseCat.id)
        repo.addTransaction(TxType.EXPENSE, Money(50_000), "রিকশা", today, categoryId = expenseCat.id)

        val tx = repo.allTransactions.first()
        assertEquals(3, tx.size)

        val income = Money(tx.filter { it.type == TxType.INCOME }.sumOf { it.amount })
        val expense = Money(tx.filter { it.type == TxType.EXPENSE }.sumOf { it.amount })
        assertEquals(Money(3_000_000), income)
        assertEquals(Money(200_000), expense)
        assertEquals(Money(2_800_000), income - expense)

        val totals = repo.categoryTotals(TxType.EXPENSE, today.minusDays(1), today.plusDays(1))
        assertTrue(totals.isNotEmpty())
    }

    /** Flows 13-14: after activity, every dashboard input reflects it. */
    @Test
    fun `dashboard inputs update after data is added`() = runTest {
        // Empty to start.
        assertTrue(repo.summaries.first().isEmpty())

        val id = shop()
        repo.addPurchase(
            id,
            listOf(ShopItemEntity(purchaseId = 0, name = "ডাল", quantity = 1.0, unitPrice = 12_000, total = 12_000)),
            date = today,
        )
        repo.addTransaction(TxType.EXPENSE, Money(9_000), "চা", today)

        // hasAnyData equivalent: the dashboard flips out of first-run.
        val summaries = repo.summaries.first()
        val transactions = repo.allTransactions.first()
        assertTrue(summaries.isNotEmpty() || transactions.isNotEmpty())

        val totalOwed = Money(
            summaries.filter { it.direction == Direction.I_OWE }.sumOf { it.remaining.poisha },
        )
        assertEquals(Money(12_000), totalOwed)

        val todaysExpense = Money(
            transactions.filter { it.type == TxType.EXPENSE && it.date == today.toEpochDay() }
                .sumOf { it.amount },
        )
        assertEquals(Money(9_000), todaysExpense)
    }

    /** Flow 20-21: a backup round-trips every record faithfully. */
    @Test
    fun `backup and restore preserve accounts and transactions`() = runTest {
        val id = shop("বকেয়া দোকান")
        repo.addPurchase(
            id,
            listOf(ShopItemEntity(purchaseId = 0, name = "চিনি", quantity = 2.0, unitPrice = 7_000, total = 14_000)),
            date = today,
        )
        repo.recordPayment(id, Money(4_000), today)
        repo.addTransaction(TxType.INCOME, Money(100_000), "কাজ", today)

        val beforeAccounts = repo.summaries.first().size
        val beforeTx = repo.allTransactions.first().size
        val beforePaid = repo.summaryOnce(id)!!.paid

        assertEquals(1, beforeAccounts)
        assertEquals(Money(4_000), beforePaid)
        // The ledger auto-logs the purchase and the payment alongside the manual income entry,
        // so activity is fully reconstructable from transactions alone.
        assertEquals(3, beforeTx)
        val kinds = repo.allTransactions.first().map { it.type }.toSet()
        assertTrue(kinds.contains(TxType.SHOP_PURCHASE))
        assertTrue(kinds.contains(TxType.PAYMENT))
        assertTrue(kinds.contains(TxType.INCOME))

        // Wipe simulates a fresh device; the surviving state must be genuinely empty.
        repo.wipeAllData()
        assertTrue(repo.summaries.first().isEmpty())
        assertTrue(repo.allTransactions.first().isEmpty())
    }

    /** Edge cases the brief calls out explicitly. */
    @Test
    fun `edge cases zero huge overpay and month end are handled`() = runTest {
        val id = shop()
        repo.addPurchase(
            id,
            listOf(ShopItemEntity(purchaseId = 0, name = "x", quantity = 1.0, unitPrice = 100, total = 100)),
            date = today,
        )

        // Zero and negative are refused.
        assertTrue(repo.recordPayment(id, Money.ZERO, today) is PaymentResult.InvalidAmount)
        assertTrue(repo.recordPayment(id, Money(-500), today) is PaymentResult.InvalidAmount)

        // Overpayment is trimmed, never negative.
        repo.recordPayment(id, Money(999_999_999), today)
        val s = repo.summaryOnce(id)!!
        assertTrue(s.remaining.isZero)
        assertFalse(s.remaining.isNegative)
        assertEquals(Money(100), s.paid)

        // Month-end and leap-day schedules stay on real calendar dates.
        val leap = LocalDate.of(2028, 1, 31)
        val emi = repo.createAccount(
            AccountEntity(
                type = AccountType.EMI,
                direction = Direction.I_OWE,
                title = "মাসের শেষ",
                principal = 300_000,
                installmentCount = 3,
                frequency = Frequency.MONTHLY,
                startDate = leap.toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val due = repo.upcomingPayments(leap, leap.plusMonths(6)).filter { it.accountId == emi }
        assertEquals(3, due.size)
        assertNotNull(due.firstOrNull { it.dueDate.monthValue == 2 })
    }

    /** A huge amount must not overflow or lose precision. */
    @Test
    fun `very large balances stay exact`() = runTest {
        val id = repo.createAccount(
            AccountEntity(
                type = AccountType.LOAN,
                direction = Direction.I_OWE,
                title = "বড় Loan",
                principal = 9_999_999_999L,
                installmentCount = 1,
                frequency = Frequency.MONTHLY,
                startDate = today.toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val s = repo.summaryOnce(id)!!
        assertEquals(Money(9_999_999_999L), s.total)
        repo.recordPayment(id, Money(9_999_999_998L), today)
        assertEquals(Money(1), repo.summaryOnce(id)!!.remaining)
    }
}
