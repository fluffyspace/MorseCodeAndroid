package com.ingokodba.morsecode

import android.util.Log
import kotlin.math.abs
import kotlin.math.min

class HandsFreeFile {

    companion object {
        const val UP = 1
        const val DOWN = 2
        const val BACK = 4
        const val READ_LAST = 3
        const val RETURN_MORSE = 5
    }

    private var counter = 0
    private var threshold = 0.3f
    private var thresholdX = 0.5f

    private var zCounter = 0

    //accelerometer orientation
    private var xGsum = 0f
    private var zGsum = 0f
    private var lastXGsum = 0f
    private var lastZGsum = 0f
    private var gCounter = 9

    private var goreDole = true
    private var leftRight = false

    fun followAccelerometer(x: Float, y: Float, z: Float, xG: Float, yG: Float, zG: Float) {

        if (zCounter < 3) {
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

        if (counter >= 0)
            counter--

        if (abs(x) > threshold || abs(z) > threshold) {
            if (abs(lastXGsum) < (abs(lastZGsum))) {

                if (x > threshold && goreDole && !leftRight) {
                    //Log.e("x je veci od threshoda ", " $x")
                    if (listener != null) {
                        listener.onTranslation(UP)
                    }
                    goreDole = !goreDole

                } else if (x < -threshold && !goreDole && !leftRight) {
                    //Log.e("max manji od thresholda", " $x")
                    if (listener != null) {
                        listener.onTranslation(DOWN)
                    }
                    goreDole = !goreDole
                } else if (z > thresholdX && counter < 0) {
                    Log.e("z ", " plius  $z")
                    if (leftRight){
                        if (listener != null) {
                            leftRight = false
                            Log.e("Stjepan ", " return to morse")
                            listener.onTranslation(RETURN_MORSE)
                        }
                    }else {
                        Log.e("Stjepan ", " back")
                        if (listener != null) {
                            listener.onTranslation(BACK)
                        }
                    }
                    counter = 3
                } else if (z < -thresholdX && counter<0) {
                    Log.e("z ", " minus $z")
                    Log.e("Stjepan ", " read message")
                    if (listener != null) {
                        listener.onTranslation(READ_LAST)
                        leftRight = true
                    }
                    counter = 3
                }

            } else if (abs(lastXGsum) > (abs(lastZGsum))) {
               if (z > threshold && goreDole && !leftRight) {
                    //Log.e("x je veci od threshoda ", " $x")
                    if (listener != null) {
                        listener.onTranslation(UP)
                    }
                    goreDole = !goreDole

                } else if (z < -threshold && !goreDole && !leftRight) {
                    //Log.e("max manji od thresholda", " $x")
                    if (listener != null) {
                        listener.onTranslation(DOWN)
                    }
                    goreDole = !goreDole
                } else if (x < -thresholdX && counter < 0) {
                    Log.e("z ", " plius  $z")
                    if (leftRight){
                        if (listener != null) {
                            leftRight = false
                            Log.e("Stjepan ", " return to morse")
                            listener.onTranslation(RETURN_MORSE)
                        }
                    }else {
                        Log.d("Stjepan ", " back")
                        if (listener != null) {
                            listener.onTranslation(BACK)
                        }
                    }
                    counter = 3
                } else if (x > thresholdX && counter<0) {
                    Log.d("Stjepan ", " read message")
                    if (listener != null) {
                        listener.onTranslation(READ_LAST)
                        leftRight = true
                    }
                    counter = 3
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