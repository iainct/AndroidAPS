package app.aaps.wear.watchfaces.utils

import java.time.Instant
import java.time.ZoneId

/** How often a CGM sends a reading, which is also how far ahead the next one is expected. */
const val READING_INTERVAL_MILLIS = 5 * 60_000L

/**
 * The mark on the seconds ring for the next glucose reading.
 *
 * @param second the second of the minute at which the reading is, or was, expected
 * @param missed false while the reading is still to come, true once that moment has passed without a
 *   new reading - the mark stays where it was and changes colour, so the wearer sees the reading is
 *   late without reading the age field
 */
data class NextReadingMark(val second: Int, val missed: Boolean)

/**
 * The mark for the next glucose reading, or null when there is nothing to point at yet.
 *
 * Nothing is shown until the reading is at most a minute away; earlier it would only add clutter to
 * the ring. In that final minute the mark says "coming". Once the expected moment has passed it stays
 * on the same second and says "missed", until a new reading arrives and moves the expected time five
 * minutes on, which takes the mark away again until the next final minute.
 *
 * No reading at all (a timestamp of 0) gives null.
 *
 * @param lastReadingMillis when the last reading was taken, epoch millis, 0 for none
 * @param nowMillis the instant being drawn
 * @param zone the zone the watch shows its time in
 */
fun nextReadingMark(lastReadingMillis: Long, nowMillis: Long, zone: ZoneId): NextReadingMark? {
    if (lastReadingMillis <= 0L) return null
    val expected = lastReadingMillis + READING_INTERVAL_MILLIS
    val untilExpected = expected - nowMillis
    if (untilExpected > 60_000L) return null
    val second = Instant.ofEpochMilli(expected).atZone(zone).toLocalTime().second
    return NextReadingMark(second = second, missed = untilExpected < 0L)
}
