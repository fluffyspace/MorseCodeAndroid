package com.ingokodba.morsecode.network

import android.content.Context
import com.ingokodba.morsecode.models.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

private const val BASE_URL =
    "https://kodba.eu/morse2/"

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

private lateinit var apiMessagesService: MessagesApiService
private lateinit var apiContactsService: ContactsApiService

private fun okhttpClient(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context))
        .build()
}

fun getMessagesApiService(context: Context): MessagesApiService {
    if (!::apiMessagesService.isInitialized) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(BASE_URL)
            .client(okhttpClient(context)) // Add our Okhttp client
            .build()
        apiMessagesService = retrofit.create(MessagesApiService::class.java)
    }
    return apiMessagesService
}

fun getContactsApiService(context: Context): ContactsApiService {
    if (!::apiContactsService.isInitialized) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(BASE_URL)
            .client(okhttpClient(context)) // Add our Okhttp client
            .build()
        apiContactsService = retrofit.create(ContactsApiService::class.java)
    }
    return apiContactsService
}

interface MessagesApiService {
    @FormUrlEncoded
    @POST("sendMessage.php")
    suspend fun sendMessage(@Field("to") to: Int, @Field("message") message: String): Long

    @FormUrlEncoded
    @POST("getMessages.php")
    suspend fun getMessages(@Field("contactId") contactId: Long): List<Message>

    @POST("getContactsWithMessages.php")
    suspend fun getMessageContact(): List<ContactListResponse>

    @POST("getNewMessages.php")
    suspend fun getNewMessages(): List<Message>

    @FormUrlEncoded
    @POST("deleteMessages.php")
    suspend fun deleteMessages(@Field("contactId") contactId: Long): Boolean
}

interface ContactsApiService {
    @POST("kontakt.php")
    suspend fun getAllContacts(): List<Contact>

    @FormUrlEncoded
    @POST("kontakt.php")
    suspend fun getContact(@Field("username") username: String): RegisterResponse

    @FormUrlEncoded
    @POST("register.php")
    suspend fun registerContact(@Field("username") username: String, @Field("password") password: String): RegisterResponse

    @FormUrlEncoded
    @POST("addFriend.php")
    suspend fun addFriend(@Field("contactId") contactId: Long): Boolean

    @FormUrlEncoded
    @POST("removeFriend.php")
    suspend fun removeFriend(@Field("contactId") contactId: Long): Boolean

    @FormUrlEncoded
    @POST("login.php")
    suspend fun logInUser(@Field("username") username: String, @Field("password") password: String, @Field("hash") hash: String): LogInResponse

    @FormUrlEncoded
    @POST("check_google_login.php")
    suspend fun googleCheckUser(@Field("id_token") id_token: String): GoogleLoginResponse

    @FormUrlEncoded
    @POST("getUserByUsername.php")
    suspend fun getUserByUsername(@Field("username") username: String): GetIdResponse

    @POST("getMyFriends.php")
    suspend fun getMyFriends(): List<Contact>
}