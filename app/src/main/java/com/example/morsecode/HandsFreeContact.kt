package com.example.morsecode

import android.util.Log

class HandsFreeContact {

    private var zCounter = 0
    private var counter = 0
    private var threshold = 1.5f
    private var maxX = 0f
    private var minX = 0f


    fun follow(x: Float, z: Float){

        if(x > maxX){
            maxX = x;
            Log.e("max x", " " + maxX)
        }

        if(x < minX){
            minX = x;
            Log.e("min x", " " + minX)
        }

        var zz = z

        if (zCounter < 10) {
            zz = 0f
            zCounter++
        }
        if (counter > 0)
            counter--

        if (zz > threshold && counter == 0) {
            if (listener != null) {
                listener.onTranslation(1)
            }
            counter = 10
        } else if (zz < -threshold && counter == 0) {
            if (listener != null) {
                listener.onTranslation(2)
            }
            counter = 10
        }
        if (x < -threshold && counter == 0) {
            Log.e("max ", " x minus")
            if (listener != null) {
                listener.onTranslation(3)
            }
            counter = 10
        } else if (x < -threshold && counter == 0) {
            Log.e("max ", " x +")
            if (listener != null) {
                listener.onTranslation(4)
            }
            counter = 10
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