package com.ingokodba.morsecode

import android.content.Context
import android.util.Log
import com.ingokodba.morsecode.baza.AppDatabase
import com.ingokodba.morsecode.models.LegProfile
import java.util.*
import kotlin.math.abs
import kotlin.math.min

class HandsFree {

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

    private var lastAccelerometerSampleTime: Long = 0

    //accelerometer orientation
    private var xGsum = 0f
    private var zGsum = 0f
    private var lastXGsum = 0f
    private var lastZGsum = 0f
    private var gCounter = 9

    private var goreDole = true
    private var leftRight = false

    var greska: Float = 5f
    var downPosition: Float = 0f
    var upPosition: Float = 10f
    var calibration: Boolean = false
    var lastXRot: Float = 0f
    var lastYRot: Float = 0f
    var lastZRot: Float = 0f

    var profile: LegProfile? = null

    // gleda kako sile magnetizma zemlje utječu
    fun followAccelerometer(x: Float, y: Float, z: Float, xG: Float, yG: Float, zG: Float) {
        // zG - pomicanje mobitela gore dole
        // xG - rotiranje mobitela gore dole
        //Log.d("ingo", "$x, $y, $z, $xG, $yG, $zG")
        lastXRot = xG
        lastYRot = yG
        lastZRot = zG
        if(!calibration) {
            if(profile != null){
                val xDiff = abs(profile!!.downX - profile!!.upX)
                val yDiff = abs(profile!!.downY - profile!!.upY)
                val zDiff = abs(profile!!.downZ - profile!!.upZ)
                val maxDiff = Collections.max(listOf(xDiff, yDiff, zDiff))
                if(maxDiff == xDiff){
                    var xUpThreshold = if(profile!!.downX < profile!!.upX){
                        profile!!.downX + xDiff*profile!!.threshold/100
                    } else {
                        profile!!.downX - xDiff*profile!!.threshold/100
                    }
                    var xDownThreshold = if(profile!!.downX < profile!!.upX){
                        profile!!.upX - xDiff*profile!!.threshold/100
                    } else {
                        profile!!.upX + xDiff*profile!!.threshold/100
                    }
                    if(xG > xUpThreshold){
                        listener.onTranslation(UP)
                    } else if(xG < xDownThreshold){
                        listener.onTranslation(DOWN)
                    }
                } else if(maxDiff == yDiff){
                    var yUpThreshold = if(profile!!.downY < profile!!.upY){
                        profile!!.downY + yDiff*profile!!.threshold/100
                    } else {
                        profile!!.downY - yDiff*profile!!.threshold/100
                    }
                    var yDownThreshold = if(profile!!.downY < profile!!.upY){
                        profile!!.upY - yDiff*profile!!.threshold/100
                    } else {
                        profile!!.upY + yDiff*profile!!.threshold/100
                    }
                    if(yG > yUpThreshold){
                        listener.onTranslation(UP)
                    } else if(yG < yDownThreshold){
                        listener.onTranslation(DOWN)
                    }
                } else if(maxDiff == zDiff){
                    var zUpThreshold = if(profile!!.downZ < profile!!.upZ){
                        profile!!.downZ + zDiff*profile!!.threshold/100
                    } else {
                        profile!!.downZ - zDiff*profile!!.threshold/100
                    }
                    var zDownThreshold = if(profile!!.downZ < profile!!.upZ){
                        profile!!.upZ - zDiff*profile!!.threshold/100
                    } else {
                        profile!!.upZ + zDiff*profile!!.threshold/100
                    }
                    if(zG > zUpThreshold){
                        listener.onTranslation(UP)
                    } else if(zG < zDownThreshold){
                        listener.onTranslation(DOWN)
                    }
                }
            } else {
                Log.d("ingo", "HandsFree - no profile selected")
            }
        }
        listener.onNewData(xG, yG, zG)
    }

    // gleda promjene sile sa sekundu na sekundu
    fun followGyroscope(x: Float, y: Float, z: Float) {
        return
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
                            Log.e("Stjepan ", "return to morse")
                            listener.onTranslation(RETURN_MORSE)
                        }
                    }else {
                        Log.e("Stjepan ", "back")
                        if (listener != null) {
                            listener.onTranslation(BACK)
                        }
                    }
                    counter = 3
                } else if (z < -thresholdX && counter<0) {
                    Log.e("z ", " minus $z")
                    Log.e("Stjepan ", "read message")
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
                            Log.e("Stjepan ", "return to morse")
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
        fun onNewData(x: Float, y: Float, z: Float)
    }

    private lateinit var listener: Listener


    fun setListener(l: Listener) {
        listener = l
    }
}