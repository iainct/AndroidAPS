package app.aaps.wear.watchfaces.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * A ring that fills up clockwise from the top as the seconds of the minute pass.
 *
 * The `second_ring` view of a custom watch face. It is drawn here rather than loaded as an image
 * because an image can only be rotated, which gives a hand, not a growing arc. The ring sits inside
 * the view's own bounds, so a 400 x 400 view at the origin puts it on the rim of the face.
 *
 * It can also carry one mark: the second at which the next glucose reading is expected, set by the
 * watch face from the time of the last reading - see [nextReadingSecond] and `nextReadingSecond()`.
 *
 * Shown and hidden together with the other seconds views, so it is never drawn in always-on, where
 * it could not move anyway.
 */
class SecondRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = Color.WHITE
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.TRANSPARENT
    }

    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val arcBounds = RectF()

    /** The second of the minute shown, 0 to 59. 0 draws an empty ring, 59 one that is nearly full. */
    var seconds: Int = 0
        set(value) {
            val clamped = value.coerceIn(0, 59)
            if (field == clamped) return
            field = clamped
            invalidate()
        }

    /**
     * The mark for the next glucose reading, or null for none.
     *
     * The view only draws it. Deciding whether the reading is close enough to point at, and whether
     * it is late, is the watch face's job through `nextReadingMark()`, so that rule can be tested on
     * its own. A late reading keeps the mark on its second in [missedReadingColor].
     */
    var nextReading: NextReadingMark? = null
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** Colour of the part of the ring that has passed. */
    var ringColor: Int
        get() = ringPaint.color
        set(value) {
            ringPaint.color = value
            invalidate()
        }

    /** Colour of the part of the ring still to come. Transparent, the default, leaves it out. */
    var trackColor: Int
        get() = trackPaint.color
        set(value) {
            trackPaint.color = value
            invalidate()
        }

    /** Colour of the next-reading mark while the reading is still to come. */
    var nextReadingColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    /** Colour of the next-reading mark once the reading is overdue. */
    var missedReadingColor: Int = Color.RED
        set(value) {
            field = value
            invalidate()
        }

    /** Stroke width in pixels. Below one pixel the ring would not be seen, so that is the floor. */
    var ringWidthPx: Float
        get() = ringPaint.strokeWidth
        set(value) {
            val width = value.coerceAtLeast(1f)
            ringPaint.strokeWidth = width
            trackPaint.strokeWidth = width
            // The mark has to stand out from a thin ring, so it is wider and never hairline
            markPaint.strokeWidth = (width * MARK_WIDTH_FACTOR).coerceAtLeast(MARK_MIN_WIDTH_PX)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = ringPaint.strokeWidth / 2f
        // Inset by half the stroke so the whole stroke stays inside the view, and centred in the
        // view when it is not square.
        val size = minOf(width, height).toFloat()
        val left = (width - size) / 2f + inset
        val top = (height - size) / 2f + inset
        arcBounds.set(left, top, left + size - 2 * inset, top + size - 2 * inset)
        if (trackPaint.color != Color.TRANSPARENT) canvas.drawOval(arcBounds, trackPaint)
        val sweep = seconds * DEGREES_PER_SECOND
        if (sweep > 0f) canvas.drawArc(arcBounds, -90f, sweep, false, ringPaint)
        nextReading?.let { mark ->
            // A short line from the rim inwards at that second, like a notch on a dial
            markPaint.color = if (mark.missed) missedReadingColor else nextReadingColor
            val angle = Math.toRadians(mark.second * DEGREES_PER_SECOND - 90.0)
            val cx = width / 2f
            val cy = height / 2f
            val outer = size / 2f - markPaint.strokeWidth / 2f
            val inner = outer - size * MARK_LENGTH_FRACTION
            canvas.drawLine(
                cx + outer * cos(angle).toFloat(), cy + outer * sin(angle).toFloat(),
                cx + inner * cos(angle).toFloat(), cy + inner * sin(angle).toFloat(),
                markPaint
            )
        }
    }

    private companion object {

        const val DEGREES_PER_SECOND = 6f

        /** Length of the next-reading mark as a share of the view's size: 20 px on a 400 px face. */
        const val MARK_LENGTH_FRACTION = 0.05f
        const val MARK_WIDTH_FACTOR = 2.5f
        const val MARK_MIN_WIDTH_PX = 3f
    }
}
