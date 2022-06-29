package com.example.morsecode

import android.util.Log
import kotlin.math.abs
import kotlin.math.min

class HandsFree {

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
            Log.e("SStjepan ", " counter = $zCounter")
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
                        listener.onTranslation(1)
                    }
                    goreDole = !goreDole

                } else if (x < -threshold && !goreDole && !leftRight) {
                    //Log.e("max manji od thresholda", " $x")
                    if (listener != null) {
                        listener.onTranslation(2)
                    }
                    goreDole = !goreDole
                } else if (z > thresholdX && counter < 0) {
                    Log.e("z ", " plius  $z")
                    if (leftRight){
                        if (listener != null) {
                            leftRight = false
                            Log.e("Stjepan ", " return to morse")
                            listener.onTranslation(5)
                        }
                    }else {
                        Log.e("Stjepan ", " back")
                        if (listener != null) {
                            listener.onTranslation(4)
                        }
                    }
                    counter = 3
                } else if (z < -thresholdX && counter<0) {
                    Log.e("z ", " minus $z")
                    Log.e("Stjepan ", " read message")
                    if (listener != null) {
                        listener.onTranslation(3)
                        leftRight = true
                    }
                    counter = 3
                }

            } else if (abs(lastXGsum) > (abs(lastZGsum))) {
               if (z > threshold && goreDole && !leftRight) {
                    //Log.e("x je veci od threshoda ", " $x")
                    if (listener != null) {
                        listener.onTranslation(1)
                    }
                    goreDole = !goreDole

                } else if (z < -threshold && !goreDole && !leftRight) {
                    //Log.e("max manji od thresholda", " $x")
                    if (listener != null) {
                        listener.onTranslation(2)
                    }
                    goreDole = !goreDole
                } else if (x < -thresholdX && counter < 0) {
                    Log.e("z ", " plius  $z")
                    if (leftRight){
                        if (listener != null) {
                            leftRight = false
                            Log.e("Stjepan ", " return to morse")
                            listener.onTranslation(5)
                        }
                    }else {
                        Log.e("Stjepan ", " back")
                        if (listener != null) {
                            listener.onTranslation(4)
                        }
                    }
                    counter = 3
                } else if (x > thresholdX && counter<0) {
                    Log.e("z ", " minus $z")
                    Log.e("Stjepan ", " read message")
                    if (listener != null) {
                        listener.onTranslation(3)
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