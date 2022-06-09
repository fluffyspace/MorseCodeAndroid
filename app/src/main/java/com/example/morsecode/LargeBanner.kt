package com.example.morsecode

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.BaseTransientBottomBar

typealias AnimationContentViewCallback = com.google.android.material.snackbar.ContentViewCallback

class LargeBanner private constructor(
    parent: ViewGroup,
    content: View,
    callback: AnimationContentViewCallback
) : BaseTransientBottomBar<LargeBanner>(parent, content, callback) {

    lateinit var visual_feedback_container:VisualFeedbackFragment

    private val text: TextView by lazy(LazyThreadSafetyMode.NONE) {
        view.findViewById<TextView>(R.id.textBaner)
    }

    fun setText(text: CharSequence): LargeBanner {
        this.text.text = text
        return this
    }

    private val bannerAction: Button by lazy(LazyThreadSafetyMode.NONE) {
        view.findViewById<Button>(R.id.tap)
    }

    fun setAction(): LargeBanner {

        bannerAction.visibility = View.VISIBLE


        bannerAction.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    visual_feedback_container.down()
                }
                MotionEvent.ACTION_UP -> {
                    visual_feedback_container.up()
                    v.performClick()
                }
            }
            true
        }

        return this
    }

    companion object {

        const val LENGTH_SHORT = BaseTransientBottomBar.LENGTH_SHORT
        const val LENGTH_LONG = BaseTransientBottomBar.LENGTH_LONG
        const val LENGTH_INDEFINITE = BaseTransientBottomBar.LENGTH_INDEFINITE

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(LENGTH_SHORT, LENGTH_LONG, LENGTH_INDEFINITE)
        annotation class BannerDuration

        private val animationCallback =
            object : AnimationContentViewCallback {
                override fun animateContentIn(delay: Int, duration: Int) {
                    /* no animations */
                }

                override fun animateContentOut(delay: Int, duration: Int) {
                    /* no animations */
                }
            }

        @JvmStatic
        fun make(view: View, text: CharSequence, @BannerDuration duration: Int): LargeBanner {
            val parent = findSuitableParent(view)
            if (parent == null) {
                throw IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view."
                )
            }

            val inflater = LayoutInflater.from(view.context)
            val content = inflater.inflate(R.layout.view_fullscreen_banner1, parent, false)
            val viewCallback = animationCallback
            val bottomBanner = LargeBanner(parent, content, viewCallback)
            //bottomBanner.setText(text)
            bottomBanner.getView().setPadding(0, 0, 0, 0)
            bottomBanner.duration = duration
            return bottomBanner
        }

        internal fun findSuitableParent(view: View): ViewGroup? {
            var currentView: View? = view
            var fallback: ViewGroup? = null
            do {
                when (currentView) {
                    is CoordinatorLayout -> return currentView
                    is FrameLayout -> if (currentView.id == android.R.id.content) {
                        return currentView
                    } else {
                        fallback = currentView
                    }
                }
                if (currentView != null) {
                    val parent = currentView.parent
                    currentView = if (parent is View) parent else null
                }
            } while (currentView != null)
            return fallback
        }
    }
}
