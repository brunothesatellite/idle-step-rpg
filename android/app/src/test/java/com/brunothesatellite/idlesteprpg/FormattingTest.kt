package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.ui.formatCompact
import com.brunothesatellite.idlesteprpg.ui.formatFloatNumber
import com.brunothesatellite.idlesteprpg.ui.formatLevelShort
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    private fun plain(value: String): String =
        value.replace('\u202F', ' ').replace('\u00A0', ' ')

    @Test
    fun compactKeepsSmallValuesExact() {
        assertEquals("0", formatCompact(0))
        assertEquals("999", formatCompact(999))
        assertEquals("9 999", plain(formatCompact(9_999)))
        assertEquals("-9 999", plain(formatCompact(-9_999)))
    }

    @Test
    fun compactUsesKiloBetweenTenThousandAndOneMillion() {
        assertEquals("10 k", plain(formatCompact(10_000)))
        assertEquals("12,3 k", plain(formatCompact(12_345)))
        assertEquals("99,9 k", plain(formatCompact(99_949)))
        assertEquals("100 k", plain(formatCompact(100_000)))
        assertEquals("999 k", plain(formatCompact(999_499)))
    }

    @Test
    fun compactUsesMegaAndMilliard() {
        assertEquals("1 M", plain(formatCompact(1_000_000)))
        assertEquals("1,2 M", plain(formatCompact(1_234_567)))
        assertEquals("12,3 M", plain(formatCompact(12_345_678)))
        assertEquals("100 M", plain(formatCompact(100_000_000)))
        assertEquals("1 Md", plain(formatCompact(1_000_000_000)))
        assertEquals("-1,2 Md", plain(formatCompact(-1_234_567_890)))
    }

    @Test
    fun floatNumbersStayShortAndUnspaced() {
        assertEquals("0", formatFloatNumber(0))
        assertEquals("999", formatFloatNumber(999))
        assertEquals("9999", formatFloatNumber(9_999))
        assertEquals("12k", formatFloatNumber(12_345))
        assertEquals("123k", formatFloatNumber(123_456))
        assertEquals("1M", formatFloatNumber(1_234_567))
        assertEquals("123M", formatFloatNumber(123_456_789))
        assertEquals("1M", formatFloatNumber(999_500))
    }

    @Test
    fun levelShortStaysCompact() {
        assertEquals("999", formatLevelShort(999))
        assertEquals("1k", formatLevelShort(1_000))
        assertEquals("3k", formatLevelShort(3_000))
        assertEquals("12k", formatLevelShort(12_345))
        assertEquals("999k", formatLevelShort(999_999))
        assertEquals("1M", formatLevelShort(1_000_000))
        assertEquals("1Md", formatLevelShort(1_000_000_000))
    }
}
