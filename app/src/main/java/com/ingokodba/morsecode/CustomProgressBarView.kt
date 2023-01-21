package com.ingokodba.morsecode

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util.Collections.max

class CustomProgressBarView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var firstThreshold:Int = -1
    var secondThreshold:Int = -1
    var thirdThreshold:Int = -1

    var firstThresholdInput:Int = -1
    var secondThresholdInput:Int = -1
    var thirdThresholdInput:Int = -1

    var firstText:String = ""
    var secondText:String = ""
    var thirdText:String = ""

    var oneTimeUnit: Int = 0
    var barColor = -1
    var progress: Float = 0f
    var TEXT_SIZE: Float = 80f

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomProgressBarView,
            0, 0).apply {

            try {
                firstThresholdInput = getInteger(R.styleable.CustomProgressBarView_first_threshold, -1)
                secondThresholdInput = getInteger(R.styleable.CustomProgressBarView_second_threshold, -1)
                thirdThresholdInput = getInteger(R.styleable.CustomProgressBarView_third_threshold, -1)

                firstText = getString(R.styleable.CustomProgressBarView_first_text).toString()
                secondText = getString(R.styleable.CustomProgressBarView_second_text).toString()
                thirdText = getString(R.styleable.CustomProgressBarView_third_text).toString()

                barColor = getInteger(R.styleable.CustomProgressBarView_bar_color, 0)
                progress = getFloat(R.styleable.CustomProgressBarView_progress, 0f)
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
        if(firstThresholdInput != -1) firstThreshold = (firstThresholdInput*0.01*w).toInt()
        if(secondThresholdInput != -1) secondThreshold = (secondThresholdInput*0.01*w).toInt()
        if(thirdThresholdInput != -1) thirdThreshold = (thirdThresholdInput*0.01*w).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            drawRect(Rect(0, 0, width, height), whitePaint)

            drawRect(Rect(0, 0, (progress*width).toInt(), height), backgroundPaint)

            if(firstThreshold != -1) {
                drawRect(Rect(firstThreshold-10, 0, firstThreshold+10, height), indicatorPaint)
            }
            if(secondThreshold != -1) {
                drawRect(Rect(secondThreshold-10, 0, secondThreshold+10, height), indicatorPaint)
            }
            if(thirdThreshold != -1) {
                drawRect(Rect(thirdThreshold-10, 0, thirdThreshold+10, height), indicatorPaint)
            }
            var text = ""
            if(thirdThreshold != -1 && progress*100 > thirdThresholdInput) {
                text = thirdText
            } else if(secondThreshold != -1 && progress*100 > secondThresholdInput) {
                text = secondText
            } else if(firstThreshold != -1 && progress*100 > firstThresholdInput) {
                text = firstText
            }
            drawText(text, (width / 2).toFloat(), (height / 2).toFloat() + 25, shadowPaint)
            drawText(text, (width / 2).toFloat(), (height / 2).toFloat() + 25, textPaint)
        }
    }

    fun updateThings(first: Int, second: Int, third: Int) {
        this.firstThresholdInput = first
        this.secondThresholdInput = second
        this.thirdThresholdInput = third
        invalidate()
        requestLayout()
    }

    fun setNewProgress(progress: Float){
        this.progress = progress
        invalidate()
        requestLayout()

    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = TEXT_SIZE
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
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = TEXT_SIZE
        color = Color.WHITE
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
}
