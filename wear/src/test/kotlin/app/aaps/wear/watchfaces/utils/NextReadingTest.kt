package app.aaps.wear.watchfaces.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneId

/**
 * The mark for the next glucose reading on the ring, and the five-minute cycle behind the pie.
 */
class NextReadingTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    // 10:04:37 UTC on 2026-10-16, so the next reading is expected at 10:09:37
    private val lastReading = 1_792_145_077_000L
    private val expected = lastReading + READING_INTERVAL_MILLIS

    // ---- the mark on the ring ----------------------------------------------------------------

    @Test
    fun `no mark without a reading`() {
        assertThat(nextReadingMark(lastReadingMillis = 0L, nowMillis = lastReading, zone = utc)).isNull()
    }

    @Test
    fun `the mark is on from the moment a reading arrives, at the second the next one is due`() {
        val coming = NextReadingMark(second = 37, missed = false)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = lastReading + 1_000L, zone = utc)).isEqualTo(coming)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = lastReading + 2 * 60_000L, zone = utc)).isEqualTo(coming)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected, zone = utc)).isEqualTo(coming)
    }

    @Test
    fun `the mark stays on its second and turns to missed once the reading is overdue`() {
        val missed = NextReadingMark(second = 37, missed = true)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected + 1L, zone = utc)).isEqualTo(missed)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected + 7 * 60_000L, zone = utc)).isEqualTo(missed)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected + 3 * 60 * 60_000L, zone = utc)).isEqualTo(missed)
    }

    @Test
    fun `a new reading moves the mark on`() {
        val newReading = lastReading + READING_INTERVAL_MILLIS + 20_000L   // 20 s late, at 10:09:57

        assertThat(nextReadingMark(lastReadingMillis = newReading, nowMillis = newReading + 1_000L, zone = utc))
            .isEqualTo(NextReadingMark(second = 57, missed = false))
    }

    @Test
    fun `the second is taken in the given zone`() {
        // No zone has an offset in seconds, so the second cannot move with the zone; this only checks
        // that the zone is used at all without an error
        val halfHour = ZoneId.of("Asia/Kolkata")
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected, zone = halfHour)?.second).isEqualTo(37)
    }

    // ---- the five-minute cycle behind the pie ------------------------------------------------

    @Test
    fun `no cycle without a reading`() {
        assertThat(readingCycle(lastReadingMillis = 0L, nowMillis = lastReading)).isNull()
    }

    @Test
    fun `the pie is full just after a reading and empties towards the next one`() {
        assertThat(readingCycle(lastReadingMillis = lastReading, nowMillis = lastReading)).isEqualTo(ReadingCycle(remaining = 1f, missed = false))
        assertThat(readingCycle(lastReadingMillis = lastReading, nowMillis = lastReading + 60_000L)).isEqualTo(ReadingCycle(remaining = 0.8f, missed = false))
        assertThat(readingCycle(lastReadingMillis = lastReading, nowMillis = lastReading + 4 * 60_000L)).isEqualTo(ReadingCycle(remaining = 0.2f, missed = false))
    }

    @Test
    fun `once missed, the pie counts the next five minutes down again in the missed state`() {
        // One minute into the second cycle: 80 % of it left, and missed
        assertThat(readingCycle(lastReadingMillis = lastReading, nowMillis = expected + 60_000L)).isEqualTo(ReadingCycle(remaining = 0.8f, missed = true))
        // Still counting in the fourth cycle
        val cycle4 = readingCycle(lastReadingMillis = lastReading, nowMillis = lastReading + 3 * READING_INTERVAL_MILLIS + 2 * 60_000L)
        assertThat(cycle4).isEqualTo(ReadingCycle(remaining = 0.6f, missed = true))
    }

    @Test
    fun `a reading from the future counts as just arrived`() {
        // Clocks between phone and watch can differ by a little; a negative age must not break the pie
        assertThat(readingCycle(lastReadingMillis = lastReading, nowMillis = lastReading - 5_000L)).isEqualTo(ReadingCycle(remaining = 1f, missed = false))
    }
}
