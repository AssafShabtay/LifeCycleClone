package com.yourcompany.lifecycleclone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class Donut24hChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Segment(
        val startAngle: Float,
        val sweepAngle: Float,
        val color: Int
    )

    private val segments = mutableListOf<Segment>()
    private val arcBounds = RectF()
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = Color.parseColor("#E0E0E0")
    }
    private var strokeWidthPx = 0f

    fun setSegments(data: List<Segment>) {
        segments.clear()
        segments.addAll(data)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val diameter = min(w - paddingLeft - paddingRight, h - paddingTop - paddingBottom)
        strokeWidthPx = (diameter * 0.22f).coerceAtLeast(8f)
        segmentPaint.strokeWidth = strokeWidthPx
        backgroundPaint.strokeWidth = strokeWidthPx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        if (strokeWidthPx == 0f) {
            val diameter = min(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom)
            strokeWidthPx = (diameter * 0.22f).coerceAtLeast(8f)
            segmentPaint.strokeWidth = strokeWidthPx
            backgroundPaint.strokeWidth = strokeWidthPx
        }
        val halfStroke = strokeWidthPx / 2f
        arcBounds.set(
            paddingLeft + halfStroke,
            paddingTop + halfStroke,
            width - paddingRight - halfStroke,
            height - paddingBottom - halfStroke
        )
        canvas.drawArc(arcBounds, 0f, 360f, false, backgroundPaint)
        segments.forEach { segment ->
            segmentPaint.color = segment.color
            canvas.drawArc(arcBounds, -90f + segment.startAngle, segment.sweepAngle, false, segmentPaint)
        }
    }
}
