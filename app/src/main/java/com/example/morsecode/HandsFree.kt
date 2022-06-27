package com.example.morsecode

import android.util.Log
import kotlin.math.abs
import kotlin.math.min

class HandsFree {

    private var counter = 0
    private var threshold = 0.3f

    private var xG = 0f

    //gyroscope min max values
    private var minX = 0f
    private var maxX = 0f
    private var minZ = 0f
    private var maxZ = 0f

    private var zCounter = 0

    //accelerometer orientation
    private var xGsum = 0f
    private var zGsum = 0f
    private var lastXGsum = 0f
    private var lastZGsum = 0f
    private var gCounter = 9


    private var goreDole = true

    fun followAccelerometer(x: Float, y: Float, z: Float, xG: Float, yG: Float, zG: Float) {

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
    }

    fun followGyroscope(x: Float, y: Float, z: Float) {

        if (abs(x) > threshold || abs(z) > threshold) {

            if (abs(lastXGsum) < (abs(lastZGsum))) {

                if (x > threshold && goreDole) {
                    Log.e("max ", " x minus")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    goreDole = !goreDole

                } else if (x < -threshold && !goreDole) {
                    Log.e("max ", " x +")
                    if (listener != null) {
                        listener.onTranslation(2)
                    }
                    goreDole = !goreDole
                } else if (z > threshold) {
                     Log.e("z ", " plus")
                    if (listener != null) {
                        //listener.onTranslation(3)
                    }

                } else if (z < -threshold ) {
                    Log.e("z ", " minus")
                    if (listener != null) {
                        listener.onTranslation(4)
                    }

                }


            } else if (abs(lastXGsum) > (abs(lastZGsum))) {
                if (z > threshold && goreDole) {
                    Log.e("max ", " z minus")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    goreDole = !goreDole

                } else if (z < -threshold && !goreDole) {
                    Log.e("max ", " z +")
                    if (listener != null) {
                        listener.onTranslation(2)
                    }
                    goreDole = !goreDole
                } else if (x > threshold && counter < 0) {
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
                }

            }
        }
/*


        }
        if(x < minX){
            minX = x
            Log.e("min x" , minX.toString())
        }

        if (x > maxX){
            maxX = x
            Log.e("max x" , maxX.toString())
        }

        if(x < minZ){
            minZ = x
            Log.e("min z" , minZ.toString())
        }

        if (x > maxZ){
            maxZ = x
            Log.e("max z" , maxZ.toString())
        }
*/
    }

    interface Listener {
        fun onTranslation(tap: Int)
    }

    private lateinit var listener: Listener


    fun setListener(l: Listener) {
        listener = l
    }
}