package org.autojs.autojs.ui.edit.editor

import android.graphics.Canvas
import android.graphics.Rect
import android.text.Layout

/**
 * 布局辅助工具类
 * Created by Stardust on 2018/2/13.
 */
object LayoutHelper {

    private val tempRect = Rect()

    @JvmStatic
    fun getLineRangeForDraw(layout: Layout, canvas: Canvas): Long {
        val dtop: Int
        val dbottom: Int
        synchronized(tempRect) {
            if (!canvas.getClipBounds(tempRect)) {
                // Negative range end used as a special flag
                return packRangeInLong(0, -1)
            }
            dtop = tempRect.top
            dbottom = tempRect.bottom
        }

        val top = maxOf(dtop, 0)
        val bottom = minOf(layout.getLineTop(layout.lineCount), dbottom)

        if (top >= bottom) return packRangeInLong(0, -1)
        return packRangeInLong(layout.getLineForVertical(top), layout.getLineForVertical(bottom))
    }

    /**
     * Pack 2 int values into a long, useful as a return value for a range
     */
    @JvmStatic
    fun packRangeInLong(start: Int, end: Int): Long {
        return (start.toLong() shl 32) or end.toLong()
    }

    /**
     * Get the start value from a range packed in a long by [packRangeInLong]
     */
    @JvmStatic
    fun unpackRangeStartFromLong(range: Long): Int {
        return (range ushr 32).toInt()
    }

    @JvmStatic
    fun unpackRangeEndFromLong(range: Long): Int {
        return (range and 0x00000000FFFFFFFFL).toInt()
    }

    @JvmStatic
    fun getLineOfChar(layout: Layout, charIndex: Int): Int {
        var low = 0
        var high = layout.lineCount - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            val midVal = layout.getLineEnd(mid)

            if (charIndex > midVal) {
                low = mid + 1
            } else if (charIndex < midVal) {
                high = mid
            } else {
                return minOf(layout.lineCount - 1, mid + 1)
            }
        }
        return low
    }

    @JvmStatic
    fun getVisibleLineAt(layout: Layout?, x: Float, y: Float): Int {
        if (layout == null) {
            return -1
        }
        return 0
    }
}
