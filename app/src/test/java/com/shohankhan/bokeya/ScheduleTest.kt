package com.shohankhan.bokeya

import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.core.ScheduleEngine
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {

    @Test
    fun `monthly schedule clamps month ends without drifting the anchor`() {
        val start = LocalDate.of(2026, 1, 31)
        val dates = ScheduleEngine.generateDueDates(start, Frequency.MONTHLY, 4)
        assertEquals(LocalDate.of(2026, 1, 31), dates[0])
        assertEquals(LocalDate.of(2026, 2, 28), dates[1])
        assertEquals(LocalDate.of(2026, 3, 31), dates[2])
        assertEquals(LocalDate.of(2026, 4, 30), dates[3])
    }

    @Test
    fun `leap year february is handled`() {
        val dates = ScheduleEngine.generateDueDates(LocalDate.of(2028, 1, 30), Frequency.MONTHLY, 2)
        assertEquals(LocalDate.of(2028, 2, 29), dates[1])
    }

    @Test
    fun `weekly and biweekly frequencies advance correctly`() {
        val start = LocalDate.of(2026, 9, 6)
        assertEquals(
            LocalDate.of(2026, 9, 20),
            ScheduleEngine.nextDate(start, Frequency.WEEKLY, 2),
        )
        assertEquals(
            LocalDate.of(2026, 10, 4),
            ScheduleEngine.nextDate(start, Frequency.BIWEEKLY, 2),
        )
    }

    @Test
    fun `installment plan sums exactly to the total`() {
        val total = Money.ofPoisha(1_000_001)
        val plan = ScheduleEngine.installmentPlan(total, 7, LocalDate.of(2026, 3, 10), Frequency.MONTHLY)
        assertEquals(7, plan.size)
        assertEquals(total.poisha, plan.sumOf { it.amount.poisha })
        assertEquals(1, plan.first().number)
        assertEquals(7, plan.last().number)
        assertTrue(plan.last().remainingAfter.isZero)
    }

    @Test
    fun `installment plan remaining decreases monotonically`() {
        val plan = ScheduleEngine.installmentPlan(
            Money.ofTaka(50_000), 12, LocalDate.of(2026, 1, 15), Frequency.MONTHLY,
        )
        var previous = Long.MAX_VALUE
        plan.forEach {
            assertTrue(it.remainingAfter.poisha < previous)
            previous = it.remainingAfter.poisha
        }
    }

    @Test
    fun `empty plan for zero count or zero amount`() {
        assertTrue(
            ScheduleEngine.installmentPlan(Money.ofTaka(100), 0, LocalDate.now(), Frequency.MONTHLY)
                .isEmpty(),
        )
        assertTrue(
            ScheduleEngine.installmentPlan(Money.ZERO, 5, LocalDate.now(), Frequency.MONTHLY)
                .isEmpty(),
        )
    }

    @Test
    fun `next occurrence is strictly after the reference date`() {
        val anchor = LocalDate.of(2026, 1, 10)
        val next = ScheduleEngine.nextOccurrenceAfter(anchor, Frequency.MONTHLY, LocalDate.of(2026, 3, 10))
        assertEquals(LocalDate.of(2026, 4, 10), next)
    }

    @Test
    fun `custom frequency respects the interval`() {
        val start = LocalDate.of(2026, 5, 1)
        assertEquals(
            LocalDate.of(2026, 5, 31),
            ScheduleEngine.nextDate(start, Frequency.CUSTOM, 3, customDays = 10),
        )
    }
}
