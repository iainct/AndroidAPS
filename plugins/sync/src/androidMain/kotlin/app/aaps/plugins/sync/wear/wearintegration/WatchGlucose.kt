package app.aaps.plugins.sync.wear.wearintegration

import app.aaps.core.data.iob.InMemoryGlucoseValue
import app.aaps.core.data.model.GV
import app.aaps.core.objects.extensions.fromGv
import kotlin.math.abs

/**
 * How far the newest bucket may sit from the newest reading and still be taken as that reading.
 * The same 90 seconds the five-minute bucketing allows a reading to be off its grid slot.
 */
private const val SAME_READING_TOLERANCE_MILLIS = 90_000L

/**
 * The glucose value the watch shows as the current one.
 *
 * A bucket's time is a point on a five-minute grid, never the time of a reading. While the CGM
 * sends every five minutes the newest bucket holds the newest reading, smoothed and calibrated as
 * the phone's own overview shows it, but snapped to the grid by up to 90 seconds. After a sensor
 * start the readings come irregularly for a while, the bucketed data is recalculated on a grid
 * anchored earlier, and the newest bucket can be a point minutes behind the newest reading, holding
 * an interpolation of older readings.
 *
 * The watch used to get the newest bucket as it was. It counts the reading's age from that time,
 * and the next-reading mark and the countdown pie with it, so it ran minutes late after a sensor
 * start and marked readings as missed while the phone had them, and showed the interpolated number.
 *
 * So the time sent is always the newest reading's own. The value is the bucket's when the bucket is
 * that reading, within the tolerance above, and the reading's raw value when it is not, because no
 * smoothed value exists for it yet.
 *
 * Pure, so the rule can be tested without the data store. The bucket itself is never changed: it
 * belongs to the store.
 *
 * @param newestReading the newest glucose reading as received, or null when there is none
 * @param newestBucket the newest bucketed value, or null when there is none yet
 */
fun glucoseForWatch(newestReading: GV?, newestBucket: InMemoryGlucoseValue?): InMemoryGlucoseValue? = when {
    newestReading == null                                                                      -> newestBucket
    newestBucket == null                                                                       -> InMemoryGlucoseValue.fromGv(newestReading)
    abs(newestBucket.timestamp - newestReading.timestamp) <= SAME_READING_TOLERANCE_MILLIS -> newestBucket.copy(timestamp = newestReading.timestamp)
    else                                                                                       -> InMemoryGlucoseValue.fromGv(newestReading)
}
