package app.aaps.plugins.sync.wear.wearintegration

import app.aaps.core.data.iob.InMemoryGlucoseValue
import app.aaps.core.data.model.GV
import app.aaps.core.data.model.SourceSensor
import app.aaps.core.data.model.TrendArrow
import app.aaps.core.data.time.T
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WatchGlucoseTest {

    private val readingTime = T.mins(100).msecs()

    private fun reading(timestamp: Long = readingTime, value: Double = 120.0) = GV(
        raw = 0.0,
        noise = 0.0,
        value = value,
        timestamp = timestamp,
        sourceSensor = SourceSensor.UNKNOWN,
        trendArrow = TrendArrow.FLAT
    )

    @Test
    fun nothingWhenThereIsNoData() {
        assertThat(glucoseForWatch(newestReading = null, newestBucket = null)).isNull()
    }

    @Test
    fun theBucketValueAtTheReadingTimeWhenTheBucketIsTheReadingOnTheGrid() {
        // Regular five-minute data: the newest bucket holds the newest reading, smoothed, but its time
        // is the grid slot, here 40 seconds after the reading
        val reading = reading()
        val bucket = InMemoryGlucoseValue(timestamp = readingTime + T.secs(40).msecs(), value = 120.0, smoothed = 118.0)

        val forWatch = glucoseForWatch(newestReading = reading, newestBucket = bucket)

        assertThat(forWatch?.timestamp).isEqualTo(readingTime)
        assertThat(forWatch?.recalculated).isEqualTo(118.0)
        // The store's own bucket keeps its grid time
        assertThat(bucket.timestamp).isEqualTo(readingTime + T.secs(40).msecs())
    }

    @Test
    fun theBucketValueUpToNinetySecondsEitherSide() {
        val reading = reading()
        val behind = InMemoryGlucoseValue(timestamp = readingTime - T.secs(90).msecs(), value = 120.0, smoothed = 118.0)
        val ahead = InMemoryGlucoseValue(timestamp = readingTime + T.secs(90).msecs(), value = 120.0, smoothed = 118.0)

        assertThat(glucoseForWatch(reading, behind)?.recalculated).isEqualTo(118.0)
        assertThat(glucoseForWatch(reading, ahead)?.recalculated).isEqualTo(118.0)
    }

    @Test
    fun theRawReadingWhenTheBucketIsFurtherAway() {
        // Irregular data after a sensor start: the newest bucket is a grid point minutes behind the
        // newest reading and holds an interpolation of older readings
        val reading = reading(value = 120.0)
        val bucket = InMemoryGlucoseValue(timestamp = readingTime - T.secs(91).msecs(), value = 104.0, smoothed = 103.0)

        val forWatch = glucoseForWatch(newestReading = reading, newestBucket = bucket)

        assertThat(forWatch?.timestamp).isEqualTo(readingTime)
        assertThat(forWatch?.recalculated).isEqualTo(120.0)
        assertThat(forWatch?.smoothed).isNull()
    }

    @Test
    fun theRawReadingWhenThereIsNoBucketYet() {
        // Fewer than three readings give no bucketed data at all
        val forWatch = glucoseForWatch(newestReading = reading(), newestBucket = null)

        assertThat(forWatch?.timestamp).isEqualTo(readingTime)
        assertThat(forWatch?.value).isEqualTo(120.0)
    }

    @Test
    fun theBucketWhenThereIsNoReading() {
        val bucket = InMemoryGlucoseValue(timestamp = readingTime, value = 120.0)

        assertThat(glucoseForWatch(newestReading = null, newestBucket = bucket)).isSameInstanceAs(bucket)
    }
}
