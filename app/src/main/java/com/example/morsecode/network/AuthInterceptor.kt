package com.example.morsecode.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Created by Ikhiloya Imokhai on 2019-10-19.
 *
 *
 * Retrofit Interceptor to intercept and encrypt response from the server
 */
class AuthInterceptor(context: Context) : Interceptor {
    private val sessionManager = SessionManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // If token has been saved, add it to the request
        sessionManager.fetchAuthToken()?.let {
            requestBuilder.addHeader("Autho", "$it")
            Log.d("ingo", "Autho token - $it")
        }
        //requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded")
        Log.d("ingo", chain.request().toString())


        return chain.proceed(requestBuilder.build())
    }
}