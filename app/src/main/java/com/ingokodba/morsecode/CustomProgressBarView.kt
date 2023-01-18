package com.ingokodba.morsecode

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.Collections.max

class CustomProgressBarView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var firstThreshold:Int = -1
    var secondThreshold:Int = -1
    var thirdThreshold:Int = -1
    var firstText:String = ""
    var secondText:String = ""
    var thirdText:String = ""
    var biggestThresholdValue = -1
    var barColor = -1
    var progress = 0

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomProgressBarView,
            0, 0).apply {

            try {
                firstThreshold = getInteger(R.styleable.CustomProgressBarView_first_threshold, -1)
                secondThreshold = getInteger(R.styleable.CustomProgressBarView_second_threshold, -1)
                thirdThreshold = getInteger(R.styleable.CustomProgressBarView_third_threshold, -1)
                biggestThresholdValue = max(listOf(firstThreshold, secondThreshold, thirdThreshold))

                firstText = getString(R.styleable.CustomProgressBarView_first_text).toString()
                secondText = getString(R.styleable.CustomProgressBarView_second_text).toString()
                thirdText = getString(R.styleable.CustomProgressBarView_third_text).toString()

                barColor = getInteger(R.styleable.CustomProgressBarView_bar_color, 0)
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
            val unit:Float = (width.toFloat()/biggestThresholdValue)
            // Down paint
            drawRect(Rect(0, 0, (unit*progress).toInt(), height), backgroundPaint)

            if(firstThreshold != -1) {
                var first_at = (unit*firstThreshold).toInt()
                drawRect(Rect(first_at-10, 0, first_at+10, height), indicatorPaint)
            }
            if(secondThreshold != -1) {
                var second_at = (unit*secondThreshold).toInt()
                drawRect(Rect(second_at-10, 0, second_at+10, height), indicatorPaint)
            }
            var text = ""
            if(thirdThreshold != -1 && progress > thirdThreshold) {
                text = thirdText
            } else if(secondThreshold != -1 && progress > secondThreshold) {
                text = secondText
            } else if(firstThreshold != -1 && progress > firstThreshold) {
                text = firstText
            }
            drawText(text, (width / 2).toFloat(), (height / 2).toFloat() + 15, textPaint)
        }
    }

    fun updateThings(first: Int, second: Int, third: Int) {
        this.firstThreshold = first
        this.secondThreshold = second
        this.thirdThreshold = third
        biggestThresholdValue = max(listOf(first, second, third))
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
        textSize = 40f
        color = Color.BLACK
    }

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if(barColor == 0) Color.parseColor("#BB86FC") else Color.parseColor("#03DAC5")
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if(barColor == 0) Color.parseColor("#3700B3") else Color.parseColor("#018786")
    }

    private val shadowPaint = Paint(0).apply {
        color = 0x101010
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
}
