package com.example.morsecode

class HandsFree {

    private var counter = 0
    private var threshold = 1.5f

    fun follow(z: Float){

        var zz = z

        if (counter < 10) {
            zz = 0f
            counter++
        }

        if (zz > threshold){
            if (listener != null){
                listener.onTranslation(1)
            }

        }else if( zz < -threshold){
            if (listener != null){
                listener.onTranslation(2)
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