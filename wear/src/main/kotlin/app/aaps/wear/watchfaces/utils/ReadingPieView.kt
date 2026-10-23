package app.aaps.wear.watchfaces.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * A small pie that counts the five minutes to the next glucose reading down.
 *
 * The `reading_pie` view of a custom watch face. Full just after a reading, it loses a wedge
 * clockwise from the top as the minutes pass and is empty when the next reading is due. It waits
 * empty for a minute, the grace a reading normally needs to reach the watch. If still no
 * reading comes it fills again and counts the next five minutes down in [missedColor], cycle after
 * cycle, until one arrives. A thin outline marks the full circle so an empty pie is still seen.
 *
 * It only draws what it is given; `readingCycle()` works the numbers out and is tested on its own.
 */
class ReadingPieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val bounds = RectF()

    /** The cycle to show, or null to draw only the outline. */
    var cycle: ReadingCycle? = null
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** Fill colour while the reading is still to come. */
    var color: Int = Color.LTGRAY
        set(value) {
            field = value
            invalidate()
        }

    /** Fill colour once the reading is missed. */
    var missedColor: Int = Color.RED
        set(value) {
            field = value
            invalidate()
        }

    /** Colour of the outline. Transparent leaves it out. */
    var outlineColor: Int
        get() = outlinePaint.color
        set(value) {
            outlinePaint.color = value
            invalidate()
        }

    /** Outline stroke in pixels, scaled by the watch face with everything else. */
    var outlineWidthPx: Float
        get() = outlinePaint.strokeWidth
        set(value) {
            outlinePaint.strokeWidth = value.coerceAtLeast(1f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val inset = outlinePaint.strokeWidth / 2f
        val left = (width - size) / 2f + inset
        val top = (height - size) / 2f + inset
        bounds.set(left, top, left + size - 2 * inset, top + size - 2 * inset)
        cycle?.let { cycle ->
            val remaining = cycle.remaining.coerceIn(0f, 1f)
            fillPaint.color = if (cycle.missed) missedColor else color
            when {
                remaining >= 1f -> canvas.drawOval(bounds, fillPaint)
                remaining > 0f  -> {
                    // What is left sits at the end of the circle, so the wedge disappears clockwise
                    // from the top and the last of it is just before twelve o'clock
                    val sweep = remaining * 360f
                    canvas.drawArc(bounds, -90f + (360f - sweep), sweep, true, fillPaint)
                }
            }
        }
        if (outlinePaint.color != Color.TRANSPARENT) canvas.drawOval(bounds, outlinePaint)
    }
}
