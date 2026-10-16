package app.aaps.wear.watchfaces.utils

import java.time.Instant
import java.time.ZoneId

/** How often a CGM sends a reading, which is also how far ahead the next one is expected. */
const val READING_INTERVAL_MILLIS = 5 * 60_000L

/**
 * The second of the minute at which the next glucose reading is expected, or null when there is
 * nothing to point at yet.
 *
 * The mark is meant for the final minute only: it appears when the next reading is at most a minute
 * away and disappears once that moment has passed. Earlier it would only add clutter to the ring,
 * and later the reading is overdue, which the age field already says.
 *
 * No reading at all (a timestamp of 0) gives null as well.
 *
 * @param lastReadingMillis when the last reading was taken, epoch millis, 0 for none
 * @param nowMillis the instant being drawn
 * @param zone the zone the watch shows its time in
 */
fun nextReadingSecond(lastReadingMillis: Long, nowMillis: Long, zone: ZoneId): Int? {
    if (lastReadingMillis <= 0L) return null
    val expected = lastReadingMillis + READING_INTERVAL_MILLIS
    val untilExpected = expected - nowMillis
    if (untilExpected < 0L || untilExpected > 60_000L) return null
    return Instant.ofEpochMilli(expected).atZone(zone).toLocalTime().second
}
