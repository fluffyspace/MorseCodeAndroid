package com.example.morsecode

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Collections.max

class CustomProgressBarView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var first:Int = -1
    var second:Int = -1
    var third:Int = -1
    var first_text:String = ""
    var second_text:String = ""
    var third_text:String = ""
    var biggest_value = -1
    var for_what = -1
    var progress = 0

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomProgressBarView,
            0, 0).apply {

            try {
                first = getInteger(R.styleable.CustomProgressBarView_first, -1)
                second = getInteger(R.styleable.CustomProgressBarView_second, -1)
                third = getInteger(R.styleable.CustomProgressBarView_third, -1)
                biggest_value = max(listOf(first, second, third))

                first_text = getString(R.styleable.CustomProgressBarView_first_text).toString()
                second_text = getString(R.styleable.CustomProgressBarView_second_text).toString()
                third_text = getString(R.styleable.CustomProgressBarView_third_text).toString()

                for_what = getInteger(R.styleable.CustomProgressBarView_forwhat, 0)
                progress = getInteger(R.styleable.CustomProgressBarView_progress, 0)
            } finally {
                recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Try for a width based on our minimum
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = View.resolveSizeAndState(minw, widthMeasureSpec, 1)

        // Whatever the width ends up being, ask for a height that would let the pie
        // get as big as it can
        val minh: Int = View.MeasureSpec.getSize(w) + paddingBottom + paddingTop
        val h: Int = View.resolveSizeAndState(
            minh,
            heightMeasureSpec,
            0
        )

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            drawRect(Rect(0, 0, width, height), whitePaint)
            val unit:Float = (width.toFloat()/biggest_value)
            // Down paint
            drawRect(Rect(0, 0, (unit*progress).toInt(), height), backgroundPaint)

            if(first != -1) {
                var first_at = (unit*first).toInt()
                drawRect(Rect(first_at-10, 0, first_at+10, height), indicatorPaint)
            }
            if(second != -1) {
                var second_at = (unit*second).toInt()
                drawRect(Rect(second_at-10, 0, second_at+10, height), indicatorPaint)
            }
            var text = ""
            if(third != -1 && progress > third) {
                text = third_text

            } else if(second != -1 && progress > second) {
                text = second_text

            } else if(first != -1 && progress > first) {
                text = first_text

            }
            drawText(text, (width / 2).toFloat(), (height / 2).toFloat() + 35, textPaint)
            /*drawOval(shadowBounds, shadowPaint)

            // Draw the label text
            drawText(data[mCurrentItem].mLabel, textX, textY, textPaint)

            // Draw the pie slices
            data.forEach {
                piePaint.shader = it.mShader
                drawArc(bounds,
                    360 - it.endAngle,
                    it.endAngle - it.startAngle,
                    true, piePaint)
            }

            // Draw the pointer
            drawLine(textX, pointerY, pointerX, pointerY, textPaint)
            drawCircle(pointerX, pointerY, pointerSize, mTextPaint)*/
        }
    }

    fun updateThings(first: Int, second: Int, third: Int) {
        this.first = first
        this.second = second
        this.third = third
        biggest_value = max(listOf(first, second, third))
        invalidate()
        requestLayout()
    }

    fun setNewProgress(progress: Int){
        this.progress = progress
        invalidate()
        requestLayout()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 100f
        color = Color.BLACK
    }

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if(for_what == 0) Color.parseColor("#BB86FC") else Color.parseColor("#03DAC5")
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if(for_what == 0) Color.parseColor("#3700B3") else Color.parseColor("#018786")
    }

    private val shadowPaint = Paint(0).apply {
        color = 0x101010
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }



}
