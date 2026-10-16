package app.aaps.wear.watchfaces.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneId

/**
 * When the ring carries the mark for the next glucose reading, and where.
 */
class NextReadingTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    // 10:04:37 UTC on 2026-10-16, so the next reading is expected at 10:09:37
    private val lastReading = 1_792_145_077_000L
    private val expected = lastReading + READING_INTERVAL_MILLIS

    @Test
    fun `no mark without a reading`() {
        assertThat(nextReadingSecond(lastReadingMillis = 0L, nowMillis = lastReading, zone = utc)).isNull()
    }

    @Test
    fun `no mark while the reading is more than a minute away`() {
        // Two minutes after the last reading: three minutes to go
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = lastReading + 2 * 60_000L, zone = utc)).isNull()
        // Just over a minute to go
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = expected - 60_001L, zone = utc)).isNull()
    }

    @Test
    fun `the mark sits at the second the reading is due, through the final minute`() {
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = expected - 60_000L, zone = utc)).isEqualTo(37)
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = expected - 30_000L, zone = utc)).isEqualTo(37)
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = expected, zone = utc)).isEqualTo(37)
    }

    @Test
    fun `no mark once the reading is overdue`() {
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = expected + 1L, zone = utc)).isNull()
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = expected + 7 * 60_000L, zone = utc)).isNull()
    }

    @Test
    fun `the second is taken in the given zone`() {
        // A whole-hour zone offset does not move the second; a 30-second one would, and none exists,
        // so the zone can only matter through daylight saving - checked here with a half-hour zone
        // to be sure the zone is used at all
        val halfHour = ZoneId.of("Asia/Kolkata")
        assertThat(nextReadingSecond(lastReadingMillis = lastReading, nowMillis = expected, zone = halfHour)).isEqualTo(37)
    }
}
