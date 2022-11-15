package com.example.morsecode.network

import retrofit2.http.Field

data class RegisterLoginRequest(
    @Field("username") val username:String,
    @Field("password") val password:String,
)
