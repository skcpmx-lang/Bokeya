package com.shohankhan.bokeya

import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.HealthLevel
import com.shohankhan.bokeya.domain.InsightEngine
import com.shohankhan.bokeya.domain.MonthSnapshot
import com.shohankhan.bokeya.domain.UpcomingPayment
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightEngineTest {

    private val today = LocalDate.of(2026, 9, 6)

    private fun input(
        overdueCount: Int = 0,
        overdueAmount: Money = Money.ZERO,
        totalIOwe: Money = Money.ofTaka(10_000),
        monthIncome: Money = Money.ofTaka(30_000),
        monthExpense: Money = Money.ofTaka(20_000),
        previousMonthExpense: Money = Money.ofTaka(20_000),
    ) = InsightEngine.Input(
        today = today,
        totalIOwe = totalIOwe,
        totalTheyOwe = Money.ofTaka(2_000),
        breakdown = mapOf(AccountType.SHOP to Money.ofTaka(4_000)),
        overdueCount = overdueCount,
        overdueAmount = overdueAmount,
        next7DaysAmount = Money.ofTaka(3_000),
        next7DaysCount = 2,
        monthIncome = monthIncome,
        monthExpense = monthExpense,
        previousMonthExpense = previousMonthExpense,
        topExpenseCategory = "বাজার",
        topExpenseAmount = Money.ofTaka(6_000),
        nearestDueDays = 3,
        totalPaidAllTime = Money.ofTaka(15_000),
    )

    @Test
    fun `overdue produces a warning insight`() {
        val insights = InsightEngine.generate(input(overdueCount = 2, overdueAmount = Money.ofTaka(5_000)))
        assertTrue(insights.any { it.id == "overdue" })
    }

    @Test
    fun `no overdue means no overdue insight`() {
        val insights = InsightEngine.generate(input())
        assertFalse(insights.any { it.id == "overdue" })
    }

    @Test
    fun `insights are unique and non empty text`() {
        val insights = InsightEngine.generate(input(overdueCount = 1, overdueAmount = Money.ofTaka(900)))
        assertEquals(insights.map { it.id }.distinct().size, insights.size)
        assertTrue(insights.all { it.text.isNotBlank() })
    }

    @Test
    fun `health flags overdue accounts first`() {
        val (level, _) = InsightEngine.health(
            Money.ofTaka(1_000), 2, Money.ZERO, Money.ofTaka(10_000), Money.ofTaka(1_000),
        )
        assertEquals(HealthLevel.HIGH_LOAD, level)
    }

    @Test
    fun `health is good with no debt and no overdue`() {
        val (level, message) = InsightEngine.health(
            Money.ZERO, 0, Money.ZERO, Money.ofTaka(10_000), Money.ofTaka(2_000),
        )
        assertEquals(HealthLevel.GOOD, level)
        assertTrue(message.isNotBlank())
    }

    @Test
    fun `health warns when expense exceeds income`() {
        val (level, _) = InsightEngine.health(
            Money.ofTaka(500), 0, Money.ZERO, Money.ofTaka(10_000), Money.ofTaka(12_000),
        )
        assertEquals(HealthLevel.ATTENTION, level)
    }

    @Test
    fun `prioritize puts overdue before today before future`() {
        val payments = listOf(
            UpcomingPayment(1, AccountType.LOAN, "ভবিষ্যৎ", Money.ofTaka(100), today.plusDays(5), null),
            UpcomingPayment(2, AccountType.EMI, "আজ", Money.ofTaka(100), today, null),
            UpcomingPayment(3, AccountType.SHOP, "পেরিয়েছে", Money.ofTaka(100), today.minusDays(3), null),
        )
        val ordered = InsightEngine.prioritize(payments, today)
        assertEquals(listOf(3L, 2L, 1L), ordered.map { it.accountId })
    }

    @Test
    fun `prioritize breaks ties by larger amount first`() {
        val payments = listOf(
            UpcomingPayment(1, AccountType.LOAN, "ছোট", Money.ofTaka(100), today, null),
            UpcomingPayment(2, AccountType.LOAN, "বড়", Money.ofTaka(900), today, null),
        )
        assertEquals(listOf(2L, 1L), InsightEngine.prioritize(payments, today).map { it.accountId })
    }

    @Test
    fun `monthly summary mentions income and expense`() {
        val text = InsightEngine.monthlySummaryText(
            MonthSnapshot(
                income = Money.ofTaka(30_000),
                expense = Money.ofTaka(18_000),
                debtPayments = Money.ofTaka(5_000),
                newDebt = Money.ZERO,
                topExpenseCategory = "বাজার",
            ),
            today,
        )
        assertTrue(text.contains("সেপ্টেম্বর"))
        assertTrue(text.contains("বাজার"))
    }
}
