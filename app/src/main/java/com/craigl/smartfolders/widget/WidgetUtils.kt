package com.craigl.smartfolders.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.craigl.smartfolders.R

object WidgetUtils {
    /**
     * Renders text to a Bitmap using a custom font to bypass RemoteViews limitations.
     */
    fun createTextBitmap(
        context: Context,
        text: String,
        fontSizeSp: Float,
        textColor: Int = Color.WHITE,
        fontRes: Int = R.font.inter_medium,
        maxWidthDp: Int = 72
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val fontSizePx = fontSizeSp * density
        val maxWidthPx = maxWidthDp * density
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = fontSizePx
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, Color.parseColor("#80000000"))
            try {
                typeface = ResourcesCompat.getFont(context, fontRes)
            } catch (e: Exception) {
                typeface = Typeface.SANS_SERIF
            }
        }

        var displayEx = text
        if (paint.measureText(displayEx) > maxWidthPx) {
            val ellipsis = "..."
            val ellipsisWidth = paint.measureText(ellipsis)
            while (displayEx.isNotEmpty() && (paint.measureText(displayEx) + ellipsisWidth) > maxWidthPx) {
                displayEx = displayEx.substring(0, displayEx.length - 1)
            }
            displayEx = displayEx.trim() + ellipsis
        }

        val fontMetrics = paint.fontMetrics
        val baseline = -fontMetrics.ascent
        val width = (paint.measureText(displayEx) + 8).toInt()
        val height = (fontMetrics.descent - fontMetrics.ascent + 8).toInt()

        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(bitmap)
        canvas.drawText(displayEx, width / 2f, baseline, paint)
        
        return bitmap
    }
}
