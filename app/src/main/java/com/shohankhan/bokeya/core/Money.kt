package com.shohankhan.bokeya.core

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.absoluteValue

/**
 * Money is stored as an integral number of poisha (1 BDT = 100 poisha) so that no
 * financial arithmetic in the app ever touches a floating point type.
 */
@JvmInline
value class Money(val poisha: Long) : Comparable<Money> {

    val isZero: Boolean get() = poisha == 0L
    val isPositive: Boolean get() = poisha > 0L
    val isNegative: Boolean get() = poisha < 0L

    operator fun plus(other: Money) = Money(poisha + other.poisha)
    operator fun minus(other: Money) = Money(poisha - other.poisha)
    operator fun times(factor: Int) = Money(poisha * factor)
    operator fun unaryMinus() = Money(-poisha)

    /** Never returns a negative value; used for "remaining due" style calculations. */
    fun clampAtZero(): Money = if (poisha < 0L) ZERO else this

    fun abs(): Money = Money(poisha.absoluteValue)

    /** Splits into [parts] installments, pushing the rounding remainder into the last one. */
    fun splitInto(parts: Int): List<Money> {
        require(parts > 0) { "parts must be > 0" }
        val base = poisha / parts
        val remainder = poisha - base * parts
        return List(parts) { index ->
            if (index == parts - 1) Money(base + remainder) else Money(base)
        }
    }

    fun toTakaDouble(): Double = poisha / 100.0

    override fun compareTo(other: Money): Int = poisha.compareTo(other.poisha)

    override fun toString(): String = BigDecimal(poisha).movePointLeft(2).toPlainString()

    companion object {
        val ZERO = Money(0L)

        fun ofTaka(taka: Long): Money = Money(taka * 100)

        fun ofPoisha(poisha: Long): Money = Money(poisha)

        /** Parses user input such as "1,250.50" or "১২৫" (Bengali digits) safely. */
        fun parseOrNull(raw: String): Money? {
            val normalized = BanglaNumbers.toWesternDigits(raw)
                .replace(",", "")
                .replace("৳", "")
                .replace(" ", "")
                .trim()
            if (normalized.isEmpty()) return null
            val decimal = normalized.toBigDecimalOrNull() ?: return null
            if (decimal.signum() < 0) return null
            return Money(decimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong())
        }

        fun sum(values: Iterable<Money>): Money = Money(values.sumOf { it.poisha })
    }
}

fun Iterable<Money>.sumMoney(): Money = Money.sum(this)
