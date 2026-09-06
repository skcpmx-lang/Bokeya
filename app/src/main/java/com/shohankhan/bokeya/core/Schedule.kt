package com.shohankhan.bokeya.core

import java.time.LocalDate

enum class Frequency(val label: String) {
    DAILY("প্রতিদিন"),
    WEEKLY("সাপ্তাহিক"),
    BIWEEKLY("পাক্ষিক"),
    MONTHLY("মাসিক"),
    QUARTERLY("ত্রৈমাসিক"),
    YEARLY("বাৎসরিক"),
    CUSTOM("কাস্টম"),
}

/**
 * Date engine for installment schedules. Handles month-end clamping: a schedule anchored on
 * the 31st falls back to the last valid day of shorter months without drifting the anchor.
 */
object ScheduleEngine {

    fun nextDate(start: LocalDate, frequency: Frequency, index: Int, customDays: Int = 0): LocalDate {
        require(index >= 0) { "index must be >= 0" }
        return when (frequency) {
            Frequency.DAILY -> start.plusDays(index.toLong())
            Frequency.WEEKLY -> start.plusWeeks(index.toLong())
            Frequency.BIWEEKLY -> start.plusWeeks(2L * index)
            Frequency.MONTHLY -> addMonthsAnchored(start, index.toLong())
            Frequency.QUARTERLY -> addMonthsAnchored(start, 3L * index)
            Frequency.YEARLY -> addMonthsAnchored(start, 12L * index)
            Frequency.CUSTOM -> start.plusDays(customDays.coerceAtLeast(1).toLong() * index)
        }
    }

    /**
     * Adds months while preserving the original day-of-month anchor. February 31 becomes
     * February 28/29, but the following March still lands on the 31st.
     */
    fun addMonthsAnchored(anchor: LocalDate, months: Long): LocalDate {
        val shifted = anchor.withDayOfMonth(1).plusMonths(months)
        val day = minOf(anchor.dayOfMonth, shifted.lengthOfMonth())
        return shifted.withDayOfMonth(day)
    }

    /** Generates [count] due dates starting at [start]. */
    fun generateDueDates(
        start: LocalDate,
        frequency: Frequency,
        count: Int,
        customDays: Int = 0,
    ): List<LocalDate> = (0 until count).map { nextDate(start, frequency, it, customDays) }

    /**
     * Builds an amortization-style plan: equal installments with the rounding remainder
     * folded into the final installment so the sum always equals [total] exactly.
     */
    fun installmentPlan(
        total: Money,
        count: Int,
        start: LocalDate,
        frequency: Frequency,
        customDays: Int = 0,
    ): List<PlannedInstallment> {
        if (count <= 0 || total.isZero) return emptyList()
        val amounts = total.splitInto(count)
        val dates = generateDueDates(start, frequency, count, customDays)
        var remaining = total
        return amounts.mapIndexed { index, amount ->
            remaining -= amount
            PlannedInstallment(
                number = index + 1,
                dueDate = dates[index],
                amount = amount,
                remainingAfter = remaining.clampAtZero(),
            )
        }
    }

    /** Next occurrence strictly after [from] for a recurring rule. */
    fun nextOccurrenceAfter(
        anchor: LocalDate,
        frequency: Frequency,
        from: LocalDate,
        customDays: Int = 0,
    ): LocalDate {
        var index = 0
        var candidate = nextDate(anchor, frequency, index, customDays)
        while (!candidate.isAfter(from) && index < 5000) {
            index++
            candidate = nextDate(anchor, frequency, index, customDays)
        }
        return candidate
    }
}

data class PlannedInstallment(
    val number: Int,
    val dueDate: LocalDate,
    val amount: Money,
    val remainingAfter: Money,
)
