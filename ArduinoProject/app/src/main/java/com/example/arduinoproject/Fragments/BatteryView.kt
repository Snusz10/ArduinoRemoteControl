/**
 * Description: This class taken from stack overflow. It is used to show the battery life of the arduino
 *
 * https://stackoverflow.com/questions/40341988/how-to-create-a-battery-level-indicator-in-android
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 *
 * =================================================================================================
*/



package com.example.arduinoproject.Fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.PaintDrawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.example.arduinoproject.R

class BatteryView constructor(context: Context) : View(context) {
    private var radius: Float = 0f

    // Top
    private var topPaint =
            PaintDrawable(Color.LTGRAY) // I only want to corner top-left and top-right so I use PaintDrawable instead of Paint
    private var topRect = Rect()
    private var topPaintWidthPercent = 50
    private var topPaintHeightPercent = 8

    // Border
    private var borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
    }
    private var borderRect = RectF()
    private var borderStrokeWidthPercent = 15
    private var borderStroke: Float = 0f

    // Percent
    private var percentPaint = Paint()
    private var percentRect = RectF()
    private var percentRectTopMin = 0f
    private var percent: Int = 0

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measureWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val measureHeight = (measureWidth * 1.8f).toInt()
        setMeasuredDimension(measureWidth, measureHeight)

        radius = borderStroke / 2
        borderStroke = (borderStrokeWidthPercent * measureWidth).toFloat() / 100

        // Top
        val topLeft = measureWidth * ((100 - topPaintWidthPercent) / 2) / 100
        val topRight = measureWidth - topLeft
        val topBottom = topPaintHeightPercent * measureHeight / 100
        topRect = Rect(topLeft, 0, topRight, topBottom)

        // Border
        val borderLeft = borderStroke / 2
        val borderTop = topBottom.toFloat() + borderStroke / 2
        val borderRight = measureWidth - borderStroke / 2
        val borderBottom = measureHeight - borderStroke / 2
        borderRect = RectF(borderLeft, borderTop, borderRight, borderBottom)

        // Progress
        val progressLeft = borderStroke
        percentRectTopMin = topBottom + borderStroke
        val progressRight = measureWidth - borderStroke
        val progressBottom = measureHeight - borderStroke
        percentRect = RectF(progressLeft, percentRectTopMin, progressRight, progressBottom)

        // Charging Image
        val chargingLeft = borderStroke
        var chargingTop = topBottom + borderStroke
        val chargingRight = measureWidth - borderStroke
        var chargingBottom = measureHeight - borderStroke
        val diff = ((chargingBottom - chargingTop) - (chargingRight - chargingLeft))
        chargingTop += diff / 2
        chargingBottom -= diff / 2
    }

    override fun onDraw(canvas: Canvas) {
        drawTop(canvas)
        drawBody(canvas)
        drawProgress(canvas, percent)
    }

    private fun drawTop(canvas: Canvas) {
        topPaint.bounds = topRect
        topPaint.setCornerRadii(floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f))
        topPaint.draw(canvas)
    }

    private fun drawBody(canvas: Canvas) {
        borderPaint.strokeWidth = borderStroke
        canvas.drawRoundRect(borderRect, radius, radius, borderPaint)
    }

    private fun drawProgress(canvas: Canvas, percent: Int) {
        percentPaint.color = getPercentColor(percent)
        percentRect.top = percentRectTopMin + (percentRect.bottom - percentRectTopMin) * (100 - percent) / 100
        canvas.drawRect(percentRect, percentPaint)
    }

    private fun getPercentColor(percent: Int): Int {
        // 100, 80
        if (percent >= 80) {
            return Color.GREEN
        }
        // 60
        if (percent >= 60) {
            return Color.rgb(145, 242, 17)
        }
        // 40
        if (percent >= 40) {
            return Color.YELLOW
        }
        // 20
        return Color.RED
    }

    private fun getBitmap(drawableId: Int, desireWidth: Int? = null, desireHeight: Int? = null): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
                desireWidth ?: drawable.intrinsicWidth,
                desireHeight ?: drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun setPercent(percent: Int) {
        if (percent > 100 || percent < 0) {
            return
        }
        this.percent = percent
        invalidate()
    }
}