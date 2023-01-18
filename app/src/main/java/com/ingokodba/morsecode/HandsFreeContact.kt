package com.ingokodba.morsecode

import android.util.Log
import kotlin.math.abs

class HandsFreeContact {

    private var zCounter = 0
    private var counter = 0
    private var threshold = 1.5f
    private var lowThreshold = 1.0f

    private var holdCounter = 0

    private var xGsum = 0f
    private var zGsum = 0f
    private var lastXGsum = 0f
    private var lastZGsum = 0f

    private var gCounter = 9


    fun follow(x: Float, y: Float, z: Float, xG: Float, yG: Float, zG: Float) {
        if (zCounter < 5) {
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

                if (x > threshold) {
                    //Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(3)
                    }
                    zCounter = holdCounter
                } else if (x < -threshold) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(4)
                    }
                    zCounter = holdCounter
                } else if (z > lowThreshold && counter < 0) {
                   // Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    zCounter = holdCounter
                } else if (z < -lowThreshold) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    zCounter = holdCounter
                }
                //sideways phone
            } else if (abs(lastXGsum) > (abs(lastZGsum))) {
                //Log.e( " obrnuti mob  "," ${abs(lastXGsum)} ${abs(lastZGsum)} x $x counter $counter")

                if (z > threshold) {
                    //Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(3)
                    }
                    zCounter = holdCounter
                } else if (z < -threshold) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(4)
                    }
                    zCounter = holdCounter
                } else if (x > lowThreshold) {
                    //Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    zCounter = holdCounter
                } else if (x < -lowThreshold) {
                    //Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    zCounter = holdCounter
                }
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