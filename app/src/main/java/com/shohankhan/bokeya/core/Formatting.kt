package com.shohankhan.bokeya.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object BanglaNumbers {
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(input: String): String = buildString(input.length) {
        for (ch in input) {
            if (ch in '0'..'9') append(banglaDigits[ch - '0']) else append(ch)
        }
    }

    fun toWesternDigits(input: String): String = buildString(input.length) {
        for (ch in input) {
            val index = banglaDigits.indexOf(ch)
            if (index >= 0) append(('0' + index)) else append(ch)
        }
    }
}

/**
 * Central currency formatter. Every amount rendered in the UI goes through here so that
 * currency symbol, grouping and digit script stay consistent app-wide.
 */
object CurrencyFormatter {

    const val SYMBOL = "৳"

    fun format(
        money: Money,
        withSymbol: Boolean = true,
        banglaDigits: Boolean = true,
        showDecimals: Boolean = false,
    ): String {
        val negative = money.isNegative
        val absolute = money.abs()
        val whole = absolute.poisha / 100
        val fraction = (absolute.poisha % 100).toInt()

        val grouped = groupBangladeshi(whole)
        val body = if (showDecimals || fraction != 0) {
            grouped + "." + fraction.toString().padStart(2, '0')
        } else {
            grouped
        }

        val scripted = if (banglaDigits) BanglaNumbers.toBanglaDigits(body) else body
        val sign = if (negative) "-" else ""
        return if (withSymbol) "$sign$SYMBOL$scripted" else sign + scripted
    }

    fun formatSigned(money: Money, banglaDigits: Boolean = true): String {
        val prefix = if (money.isNegative) "-" else "+"
        return prefix + format(money.abs(), banglaDigits = banglaDigits)
    }

    /** Bangladeshi (lakh/crore) digit grouping: 1,23,45,678 */
    fun groupBangladeshi(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3)
        val tail = digits.takeLast(3)
        val chunks = mutableListOf<String>()
        var remaining = head
        while (remaining.length > 2) {
            chunks.add(0, remaining.takeLast(2))
            remaining = remaining.dropLast(2)
        }
        if (remaining.isNotEmpty()) chunks.add(0, remaining)
        return chunks.joinToString(",") + "," + tail
    }
}

object BanglaDate {

    private val months = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর",
    )

    private val weekdays = arrayOf(
        "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার", "রবিবার",
    )

    fun monthName(month: Int): String = months[month - 1]

    fun weekdayName(date: LocalDate): String = weekdays[date.dayOfWeek.value - 1]

    /** "৬ সেপ্টেম্বর ২০২৬" */
    fun full(date: LocalDate): String =
        BanglaNumbers.toBanglaDigits(date.dayOfMonth.toString()) + " " +
            monthName(date.monthValue) + " " +
            BanglaNumbers.toBanglaDigits(date.year.toString())

    /** "৬ সেপ্টেম্বর" */
    fun dayMonth(date: LocalDate): String =
        BanglaNumbers.toBanglaDigits(date.dayOfMonth.toString()) + " " + monthName(date.monthValue)

    fun monthYear(date: LocalDate): String =
        monthName(date.monthValue) + " " + BanglaNumbers.toBanglaDigits(date.year.toString())

    fun time(time: LocalTime): String {
        val hour24 = time.hour
        val suffix = when {
            hour24 < 6 -> "রাত"
            hour24 < 12 -> "সকাল"
            hour24 < 16 -> "দুপুর"
            hour24 < 19 -> "বিকেল"
            hour24 < 21 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        val text = "$hour12:" + time.minute.toString().padStart(2, '0')
        return suffix + " " + BanglaNumbers.toBanglaDigits(text)
    }

    /** Human relative phrasing used across dashboard, reminders and lists. */
    fun relative(target: LocalDate, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(today, target)
        return when {
            days == 0L -> "আজ"
            days == 1L -> "আগামীকাল"
            days == -1L -> "গতকাল"
            days in 2..6 -> BanglaNumbers.toBanglaDigits(days.toString()) + " দিন পর"
            days < -1 && days >= -6 -> BanglaNumbers.toBanglaDigits((-days).toString()) + " দিন আগে"
            days in 7..13 -> "এই সপ্তাহের পরে"
            days < 0 -> full(target)
            else -> full(target)
        }
    }

    fun greeting(now: LocalDateTime = LocalDateTime.now()): String = when (now.hour) {
        in 4..5 -> "সুপ্রভাত"
        in 6..11 -> "শুভ সকাল"
        in 12..14 -> "শুভ দুপুর"
        in 15..17 -> "শুভ বিকেল"
        in 18..20 -> "শুভ সন্ধ্যা"
        else -> "শুভ রাত্রি"
    }
}

object Clocks {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(zone())

    fun nowMillis(): Long = System.currentTimeMillis()

    fun LocalDate.toEpochDayLong(): Long = this.toEpochDay()
}
