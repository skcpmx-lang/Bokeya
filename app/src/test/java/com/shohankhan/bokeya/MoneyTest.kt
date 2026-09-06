package com.shohankhan.bokeya

import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `subtraction is exact with no floating point drift`() {
        val due = Money.ofTaka(10_000)
        val afterFirst = due - Money.ofTaka(2_500)
        assertEquals(Money.ofTaka(7_500), afterFirst)
        val afterSecond = afterFirst - Money.ofTaka(3_000)
        assertEquals(Money.ofTaka(4_500), afterSecond)
        val final = afterSecond - Money.ofTaka(4_500)
        assertEquals(Money.ZERO, final)
        assertTrue(final.isZero)
    }

    @Test
    fun `repeated decimal arithmetic never accumulates error`() {
        var balance = Money.ofTaka(100)
        repeat(300) { balance -= Money.ofPoisha(33) }
        assertEquals(10_000L - 300 * 33, balance.poisha)
    }

    @Test
    fun `clampAtZero prevents negative remaining`() {
        val remaining = Money.ofTaka(500) - Money.ofTaka(800)
        assertTrue(remaining.isNegative)
        assertEquals(Money.ZERO, remaining.clampAtZero())
    }

    @Test
    fun `splitInto distributes remainder into last installment`() {
        val parts = Money.ofPoisha(1000).splitInto(3)
        assertEquals(3, parts.size)
        assertEquals(1000L, parts.sumOf { it.poisha })
        assertEquals(333L, parts[0].poisha)
        assertEquals(334L, parts[2].poisha)
    }

    @Test
    fun `splitInto always sums back to the original for many sizes`() {
        for (count in 1..37) {
            val parts = Money.ofPoisha(999_983).splitInto(count)
            assertEquals(999_983L, parts.sumOf { it.poisha })
        }
    }

    @Test
    fun `parse handles separators symbol and bangla digits`() {
        assertEquals(Money.ofTaka(1_250), Money.parseOrNull("1,250"))
        assertEquals(Money.ofPoisha(125_050), Money.parseOrNull("1250.50"))
        assertEquals(Money.ofTaka(125), Money.parseOrNull("৳১২৫"))
        assertEquals(Money.ofTaka(500), Money.parseOrNull(" 500 "))
    }

    @Test
    fun `parse rejects invalid and negative input`() {
        assertNull(Money.parseOrNull(""))
        assertNull(Money.parseOrNull("abc"))
        assertNull(Money.parseOrNull("-100"))
    }

    @Test
    fun `parse rounds half up at two decimals`() {
        assertEquals(Money.ofPoisha(1006), Money.parseOrNull("10.055"))
    }

    @Test
    fun `bangladeshi grouping uses lakh and crore`() {
        assertEquals("500", CurrencyFormatter.groupBangladeshi(500))
        assertEquals("1,000", CurrencyFormatter.groupBangladeshi(1_000))
        assertEquals("1,00,000", CurrencyFormatter.groupBangladeshi(100_000))
        assertEquals("1,23,45,678", CurrencyFormatter.groupBangladeshi(12_345_678))
    }

    @Test
    fun `formatter renders symbol and bangla digits`() {
        val text = CurrencyFormatter.format(Money.ofTaka(1_500))
        assertTrue(text.startsWith("৳"))
        assertEquals("৳১,৫০০", text)
        assertEquals("৳1,500", CurrencyFormatter.format(Money.ofTaka(1_500), banglaDigits = false))
    }

    @Test
    fun `zero and very large amounts format safely`() {
        assertEquals("৳০", CurrencyFormatter.format(Money.ZERO))
        val large = Money.ofTaka(99_99_99_999L)
        assertTrue(CurrencyFormatter.format(large, banglaDigits = false).contains(","))
    }

    @Test
    fun `bangla digit conversion round trips`() {
        assertEquals("১২৩৪৫৬৭৮৯০", BanglaNumbers.toBanglaDigits("1234567890"))
        assertEquals("1234567890", BanglaNumbers.toWesternDigits("১২৩৪৫৬৭৮৯০"))
    }
}
