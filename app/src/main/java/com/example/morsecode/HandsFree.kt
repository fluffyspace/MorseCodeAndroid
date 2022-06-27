package com.example.morsecode

import android.util.Log
import kotlin.math.min

class HandsFree {

    private var counter = 0
    private var threshold = 1.5f

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

    fun followGyroscope(x: Float, y: Float, zG: Float){

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

    }

    interface Listener {
        fun onTranslation(tap: Int)
    }

    private lateinit var listener: Listener


    fun setListener(l: Listener) {
        listener = l
    }
}