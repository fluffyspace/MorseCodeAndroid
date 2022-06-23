package com.example.morsecode

class HandsFree {

    private var counter = 0
    private var threshold = 1.5f
    private var xM = 0f
    private var yM = 0f
    private var zM = 0f

    fun follow(x: Float, z: Float) {

        var zz = z

        if (counter < 10) {
            zz = 0f
            counter++
        }

        zM += zz * 0.5f
        xM += x * 0.5f

        if (zM > threshold) {
            if (listener != null) {
                listener.onTranslation(1)
            }

        } else if (zM < -threshold) {
            if (listener != null) {
                listener.onTranslation(2)
            }
        } else if (xM < -threshold) {
            if (listener != null) {
                listener.onTranslation(3)
            }
        } else if (xM < -threshold) {
            if (listener != null) {
                listener.onTranslation(4)
            }
        }

    }

    interface Listener {
        fun onTranslation(tap: Int)
    }

    private lateinit var listener: Listener


    fun setListener(l: Listener) {
        listener = l
    }
}