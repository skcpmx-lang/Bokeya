package com.shohankhan.bokeya

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.AccountEntity
import com.shohankhan.bokeya.data.db.BokeyaDatabase
import com.shohankhan.bokeya.data.db.ShopItemEntity
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.data.repo.PaymentResult
import com.shohankhan.bokeya.domain.AccountStatus
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.PaymentMethod
import java.time.LocalDate
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class RepositoryTest {

    private lateinit var db: BokeyaDatabase
    private lateinit var repository: BokeyaRepository

    private val today: LocalDate = LocalDate.of(2026, 9, 6)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BokeyaDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = BokeyaRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newLoan(
        principal: Money = Money.ofTaka(12_000),
        interest: Money = Money.ZERO,
        installments: Int = 12,
    ): Long = repository.createAccount(
        AccountEntity(
            type = AccountType.LOAN,
            direction = Direction.I_OWE,
            title = "ব্যাংক Loan",
            principal = principal.poisha,
            interestAmount = interest.poisha,
            installmentCount = installments,
            frequency = Frequency.MONTHLY,
            startDate = today.toEpochDay(),
            institution = "সোনালী ব্যাংক",
            createdAt = 0,
            updatedAt = 0,
        ),
    )

    @Test
    fun `creating a loan generates a full installment schedule`() = runTest {
        val id = newLoan()
        val installments = db.installmentDao().forAccount(id)
        assertEquals(12, installments.size)
        assertEquals(Money.ofTaka(12_000).poisha, installments.sumOf { it.expectedAmount })
        assertEquals(1, installments.minOf { it.number })
        assertEquals(12, installments.maxOf { it.number })
    }

    @Test
    fun `partial payment reduces remaining without settling the account`() = runTest {
        val id = newLoan()
        val result = repository.recordPayment(id, Money.ofTaka(1_000), today)
        assertTrue(result is PaymentResult.Success)
        result as PaymentResult.Success
        assertEquals(Money.ofTaka(1_000), result.applied)
        assertEquals(Money.ofTaka(11_000), result.remaining)
        assertFalse(result.fullyPaid)

        val summary = repository.summaryOnce(id)!!
        assertEquals(Money.ofTaka(1_000), summary.paid)
        assertEquals(Money.ofTaka(11_000), summary.remaining)
        assertEquals(AccountStatus.PARTIALLY_PAID, summary.status)
    }

    @Test
    fun `many small payments settle the account exactly`() = runTest {
        val id = newLoan(principal = Money.ofTaka(1_000), installments = 4)
        repeat(9) { repository.recordPayment(id, Money.ofTaka(100), today) }
        val last = repository.recordPayment(id, Money.ofTaka(100), today) as PaymentResult.Success
        assertTrue(last.fullyPaid)
        assertEquals(Money.ZERO, repository.summaryOnce(id)!!.remaining)
    }

    @Test
    fun `overpayment is trimmed to the outstanding amount`() = runTest {
        val id = newLoan(principal = Money.ofTaka(500), installments = 1)
        val result = repository.recordPayment(id, Money.ofTaka(900), today) as PaymentResult.Success
        assertTrue(result.overpaymentTrimmed)
        assertEquals(Money.ofTaka(500), result.applied)
        assertEquals(Money.ZERO, result.remaining)
    }

    @Test
    fun `paying a settled account is rejected`() = runTest {
        val id = newLoan(principal = Money.ofTaka(100), installments = 1)
        repository.recordPayment(id, Money.ofTaka(100), today)
        assertEquals(PaymentResult.AlreadySettled, repository.recordPayment(id, Money.ofTaka(10), today))
    }

    @Test
    fun `zero or negative payments are rejected`() = runTest {
        val id = newLoan()
        assertEquals(PaymentResult.InvalidAmount, repository.recordPayment(id, Money.ZERO, today))
        assertEquals(PaymentResult.InvalidAmount, repository.recordPayment(id, Money.ofTaka(-5), today))
    }

    @Test
    fun `payment on a missing account reports not found`() = runTest {
        assertEquals(PaymentResult.NotFound, repository.recordPayment(9999L, Money.ofTaka(10), today))
    }

    @Test
    fun `payments fill installments oldest first`() = runTest {
        val id = newLoan(principal = Money.ofTaka(1_200), installments = 12)
        repository.recordPayment(id, Money.ofTaka(250), today)
        val installments = db.installmentDao().forAccount(id).sortedBy { it.number }
        assertEquals(Money.ofTaka(100).poisha, installments[0].paidAmount)
        assertEquals(Money.ofTaka(100).poisha, installments[1].paidAmount)
        assertEquals(Money.ofTaka(50).poisha, installments[2].paidAmount)
        assertEquals(0L, installments[3].paidAmount)
    }

    @Test
    fun `deleting a payment restores the outstanding balance`() = runTest {
        val id = newLoan(principal = Money.ofTaka(1_000), installments = 2)
        repository.recordPayment(id, Money.ofTaka(400), today)
        val payment = db.paymentDao().allOnce().first { it.accountId == id }
        repository.deletePayment(payment.id, id)
        assertEquals(Money.ofTaka(1_000), repository.summaryOnce(id)!!.remaining)
    }

    @Test
    fun `shop purchase totals roll up to the account`() = runTest {
        val shopId = repository.createAccount(
            AccountEntity(
                type = AccountType.SHOP,
                direction = Direction.I_OWE,
                title = "রহিম স্টোর",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        repository.addPurchase(
            shopId,
            listOf(
                ShopItemEntity(purchaseId = 0, name = "চাল", quantity = 5.0, unit = "কেজি", unitPrice = 6_000, total = 30_000),
                ShopItemEntity(purchaseId = 0, name = "ডাল", quantity = 2.0, unit = "কেজি", unitPrice = 12_000, total = 24_000),
            ),
            today,
            null,
        )
        val summary = repository.summaryOnce(shopId)!!
        assertEquals(Money.ofPoisha(54_000), summary.total)
        assertEquals(Money.ofPoisha(54_000), summary.remaining)
    }

    @Test
    fun `overdue detection uses the earliest unpaid installment`() = runTest {
        val id = repository.createAccount(
            AccountEntity(
                type = AccountType.EMI,
                direction = Direction.I_OWE,
                title = "ফ্রিজ EMI",
                principal = Money.ofTaka(6_000).poisha,
                installmentCount = 6,
                frequency = Frequency.MONTHLY,
                startDate = today.minusMonths(3).toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        repository.refreshInstallmentStatuses(id, today)
        val overdue = repository.overduePayments(today)
        assertTrue(overdue.any { it.accountId == id })
    }

    @Test
    fun `custom categories can be added and removed but built-ins survive`() = runTest {
        val id = repository.addCategory("টিউশন", CategoryKind.EXPENSE)
        assertNotNull(repository.category(id))
        repository.deleteCategory(id)
        assertEquals(null, repository.category(id))
    }

    @Test
    fun `wipe removes every account and payment`() = runTest {
        val id = newLoan()
        repository.recordPayment(id, Money.ofTaka(100), today, PaymentMethod.BKASH)
        repository.wipeAllData()
        assertEquals(null, repository.account(id))
        assertEquals(0L, db.paymentDao().totalFor(id))
    }
}
