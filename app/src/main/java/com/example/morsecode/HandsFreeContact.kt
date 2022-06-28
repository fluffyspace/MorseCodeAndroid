package com.example.morsecode

import android.util.Log
import kotlin.math.abs

class HandsFreeContact {

    private var zCounter = 0
    private var counter: Long = 0
    private var threshold = 1.5f
    private var lowThreshold = 1.2f

    private var xGsum = 0f
    private var zGsum = 0f
    private var lastXGsum = 0f
    private var lastZGsum = 0f

    private var gCounter = 9


    fun follow(x: Float, y: Float, z: Float, xG: Float, yG: Float, zG: Float) {
        if (zCounter < 10) {
            zCounter++
            return
        }

        xGsum = (xGsum + xG) / 2
        zGsum = (zGsum + zG) / 2

        if (gCounter == 0) {
            lastXGsum = (lastXGsum + xGsum) / 2
            lastZGsum = (lastZGsum + zGsum) / 2
            gCounter = 9
        } else {
            gCounter--
        }

        //Log.e("g sum", " $xGsum x $zGsum")

        if (abs(x) > threshold || abs(z) > threshold) {
            //Log.e("abs x ili aabz je veci od thresholda", " abs(x) ${abs(x)} abs z${abs(z)}")

            //flat phone
            if (abs(lastXGsum) < (abs(lastZGsum))) {
                //Log.e(" ravni mob ", " ${abs(lastXGsum)} ${abs(lastZGsum)} x $x counter $counter")

                if (x > threshold && counter < 0) {
                    //Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(3)
                    }
                    counter = 10
                } else if (x < -threshold && counter < 0) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(4)
                    }
                    counter = 10
                } else if (z > threshold && counter < 0) {
                   // Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    counter = 10
                } else if (z < -threshold && counter < 0) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    counter = 10
                }
                //sideways phone
            } else if (abs(lastXGsum) > (abs(lastZGsum))) {
                //Log.e( " obrnuti mob  "," ${abs(lastXGsum)} ${abs(lastZGsum)} x $x counter $counter")

                if (z > threshold && counter < 0) {
                    //Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(3)
                    }
                    counter = 10
                } else if (z < -threshold && counter < 0) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(4)
                    }
                    counter = 10
                } else if (x > lowThreshold && counter < 0) {
                    //Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    counter = 10
                } else if (x < -lowThreshold && counter < 0) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    counter = 10
                }
            }
        }
        counter--
    }

    interface Listener {
        fun onTranslation(tap: Int)
    }

    private lateinit var listener: Listener


    fun setListener(l: Listener) {
        listener = l
    }
}