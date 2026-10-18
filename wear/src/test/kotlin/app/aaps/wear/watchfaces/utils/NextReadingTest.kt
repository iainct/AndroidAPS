package app.aaps.wear.watchfaces.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneId

/**
 * When the ring carries the mark for the next glucose reading, where, and in which state.
 */
class NextReadingTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    // 10:04:37 UTC on 2026-10-16, so the next reading is expected at 10:09:37
    private val lastReading = 1_792_145_077_000L
    private val expected = lastReading + READING_INTERVAL_MILLIS

    @Test
    fun `no mark without a reading`() {
        assertThat(nextReadingMark(lastReadingMillis = 0L, nowMillis = lastReading, zone = utc)).isNull()
    }

    @Test
    fun `no mark while the reading is more than a minute away`() {
        // Two minutes after the last reading: three minutes to go
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = lastReading + 2 * 60_000L, zone = utc)).isNull()
        // Just over a minute to go
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected - 60_001L, zone = utc)).isNull()
    }

    @Test
    fun `the mark sits at the second the reading is due, through the final minute`() {
        val coming = NextReadingMark(second = 37, missed = false)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected - 60_000L, zone = utc)).isEqualTo(coming)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected - 30_000L, zone = utc)).isEqualTo(coming)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected, zone = utc)).isEqualTo(coming)
    }

    @Test
    fun `the mark stays on its second and turns to missed once the reading is overdue`() {
        val missed = NextReadingMark(second = 37, missed = true)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected + 1L, zone = utc)).isEqualTo(missed)
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected + 7 * 60_000L, zone = utc)).isEqualTo(missed)
        // However late, until a reading arrives
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected + 3 * 60 * 60_000L, zone = utc)).isEqualTo(missed)
    }

    @Test
    fun `a new reading takes the mark away until its own final minute`() {
        val newReading = lastReading + READING_INTERVAL_MILLIS + 20_000L   // 20 s late, at 10:09:57

        assertThat(nextReadingMark(lastReadingMillis = newReading, nowMillis = newReading + 1_000L, zone = utc)).isNull()
        assertThat(nextReadingMark(lastReadingMillis = newReading, nowMillis = newReading + READING_INTERVAL_MILLIS - 10_000L, zone = utc))
            .isEqualTo(NextReadingMark(second = 57, missed = false))
    }

    @Test
    fun `the second is taken in the given zone`() {
        // No zone has an offset in seconds, so the second cannot move with the zone; this only checks
        // that the zone is used at all without an error
        val halfHour = ZoneId.of("Asia/Kolkata")
        assertThat(nextReadingMark(lastReadingMillis = lastReading, nowMillis = expected, zone = halfHour)?.second).isEqualTo(37)
    }
}
