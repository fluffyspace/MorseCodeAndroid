package com.ingokodba.morsecode.models

data class Postavke(
    var pwm_on: Long = 0,
    var pwm_off: Long = 0,
    var oneTimeUnit: Long = 0,
    var socketioIp: String = "",
    var userId: Int = -1,
    var username: String = "",
    var userHash: String = "",
    var handsFreeOnChat: Boolean = false,
    var lastLegProfile: Long = -1,
    var physicalButtons: MutableList<Int> = mutableListOf(),
    var award_interval: Int = 20,
)
