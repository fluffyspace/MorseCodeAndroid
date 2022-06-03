package com.example.morsecode.network

import com.example.morsecode.models.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL =
    "https://kodba.eu/morse/"

/**
 * Build the Moshi object that Retrofit will be using, making sure to add the Kotlin adapter for
 * full Kotlin compatibility.
 */
private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/**
 * Use the Retrofit builder to build a retrofit object using a Moshi converter with our Moshi
 * object.
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

/**
 * A public interface that exposes the [getPhotos] method
 */
interface MessagesApiService {
    //@Headers("Accept: text/html")
    //@Headers("Content-Type: application/json")
    @GET("api")
    suspend fun sendMessage(@Query("poruka") poruka: String, @Query("token") token: String): Poruka

    @GET("api")
    suspend fun getAllMessages(@Query("poruka") poruka: String, @Query("token") token: String): List<Zadatak>
}

interface ContactsApiService {
    //@Headers("Accept: text/html")
    //@Headers("Content-Type: application/json")
    @GET("api/kontakt.php")
    suspend fun getAllContacts(): List<EntitetKontakt>

    @GET("api/kontakt.php")
    suspend fun getContact(@Query("username") username: String): RegisterResponse

    @GET("api/register.php")
    suspend fun registerContact(@Query("username") username: String, @Query("password") password: String): RegisterResponse

    @GET("api/login.php")
    suspend fun logInUser(@Query("username") username: String, @Query("password") password: String): LogInResponse
}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object MessagesApi {
    val retrofitService: MessagesApiService by lazy { retrofit.create(MessagesApiService::class.java) }
}

object ContactsApi {
    val retrofitService: ContactsApiService by lazy { retrofit.create(ContactsApiService::class.java) }
}