package com.example.morsecode.moodel

import com.squareup.moshi.Json

data class Poruka(
    @Json(name = "poruka") val poruka:String?,
    @Json(name = "vibrate") val vibrate:String?,
    @Json(name = "sss") val sss:Long? = 1,
    @Json(name = "aaa") val aaa:Long? = 10,
)
