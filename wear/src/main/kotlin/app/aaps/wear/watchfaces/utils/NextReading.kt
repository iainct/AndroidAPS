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
 * The mark for the next glucose reading, or null when there is no reading at all (a timestamp of 0).
 *
 * The mark is on from the moment a reading arrives: it points at the second, five minutes on, when
 * the next one is expected. Once that moment has passed without a reading it stays on the same
 * second and says "missed", until a reading arrives and moves it on.
 *
 * @param lastReadingMillis when the last reading was taken, epoch millis, 0 for none
 * @param nowMillis the instant being drawn
 * @param zone the zone the watch shows its time in
 */
fun nextReadingMark(lastReadingMillis: Long, nowMillis: Long, zone: ZoneId): NextReadingMark? {
    if (lastReadingMillis <= 0L) return null
    val expected = lastReadingMillis + READING_INTERVAL_MILLIS
    val second = Instant.ofEpochMilli(expected).atZone(zone).toLocalTime().second
    return NextReadingMark(second = second, missed = nowMillis > expected)
}

/**
 * Where the watch is in the five-minute wait for the next reading, for the countdown pie.
 *
 * @param remaining the share of the current five-minute cycle still to go, 1.0 just after a reading
 *   and falling to 0.0 as the next one becomes due
 * @param missed false in the first cycle after a reading, true in every cycle after it - the pie
 *   keeps counting five minutes at a time, in the missed colour, until a reading arrives
 */
data class ReadingCycle(val remaining: Float, val missed: Boolean)

/**
 * The current five-minute cycle since the last reading, or null when there is no reading at all.
 *
 * @param lastReadingMillis when the last reading was taken, epoch millis, 0 for none
 * @param nowMillis the instant being drawn
 */
fun readingCycle(lastReadingMillis: Long, nowMillis: Long): ReadingCycle? {
    if (lastReadingMillis <= 0L) return null
    val age = (nowMillis - lastReadingMillis).coerceAtLeast(0L)
    val intoCycle = age % READING_INTERVAL_MILLIS
    return ReadingCycle(
        remaining = 1f - intoCycle.toFloat() / READING_INTERVAL_MILLIS,
        missed = age > READING_INTERVAL_MILLIS
    )
}
