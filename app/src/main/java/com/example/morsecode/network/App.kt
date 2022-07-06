package com.example.morsecode.network

import android.app.Application
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit


class App : Application() {
    /*private var apiService: VibrationMessagesApi? = null
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        val httpBuilder = OkHttpClient.Builder()

        //Gson Builder
        val gsonBuilder = GsonBuilder()
        val gson = gsonBuilder.create()

        /**
         * injection of interceptors to handle encryption and decryption
         */

        //Encryption Interceptor
        val encryptionInterceptor = AuthentificationInterceptor(1, "20f4a2d91ebc229e6e6081ac9a060894")

        // OkHttpClient. Be conscious with the order
        val okHttpClient: OkHttpClient = OkHttpClient()
            .newBuilder() //httpLogging interceptor for logging network requests
            .addInterceptor(encryptionInterceptor) // interceptor for decryption of request data
            .build()

        //Retrofit
        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL) // for serialization
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        //ApiService
        apiService = retrofit.create(ApiService::class.java)
    }

    val bookService: ApiService?
        get() = apiService

    companion object {
        private val TAG = App::class.java.simpleName
        private const val BASE_URL = "https://66d252d7-61d1-44e0-be70-1f77477ac86c.mock.pstmn.io"
        private var INSTANCE: App? = null
        fun get(): App? {
            return INSTANCE
        }
    }*/
}