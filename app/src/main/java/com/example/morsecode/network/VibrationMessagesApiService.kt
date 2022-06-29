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
interface VibrationMessagesApiService {
    //@Headers("Accept: text/html")
    //@Headers("Content-Type: application/json")
    @GET("api")
    suspend fun sendMessage(@Query("poruka") poruka: String): VibrationMessage

    @GET("api")
    suspend fun getAllMessages(@Query("poruka") poruka: String): List<Zadatak>
}

interface MessagesApiService {
    //@Headers("Accept: text/html")
    //@Headers("Content-Type: application/json")
    @GET("api/sendMessage.php")
    suspend fun sendMessage(@Query("id") senderId: Long, @Query("hash") password: String?, @Query("to") receiverId: Int, @Query("message") message: String): Long

    @GET("api/getMessages.php")
    suspend fun getMessages(@Query("id") senderId: Long, @Query("hash") password: String?, @Query("contactId") receiverId: Int): List<Message>

    @GET("api/getContactsWithMessages.php")
    suspend fun getMessageContact(@Query("id") id: Int, @Query("hash") hash: String?): List<ContactListResponse>

    @GET("api/getNewMessages.php")
    suspend fun getNewMessages(@Query("id") id: Int, @Query("hash") hash: String?): List<Message>
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

    @GET("api/addFriend.php")
    suspend fun addFriend(@Query("id") id: Int, @Query("hash") hash: String, @Query("friendId") friendId: Long?): Boolean

    @GET("api/login.php")
    suspend fun logInUser(@Query("username") username: String, @Query("password") password: String): LogInResponse

    @GET("api/getUserByUsername.php")
    suspend fun getUserByUsername(@Query("id") id: Int, @Query("hash") hash: String,@Query("username") username: String): GetIdResponse

    @GET("api/getMyFriends.php")
    suspend fun getMyFriends(@Query("id") id: Int, @Query("hash") hash: String): List<EntitetKontakt>

    @GET("api/removeFriend.php")
    suspend fun removeFriend(@Query("id") id: Int, @Query("hash") hash: String, @Query("friendId") friendId: Int): Boolean
}



/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object VibrationMessagesApi {
    val retrofitService: VibrationMessagesApiService by lazy { retrofit.create(VibrationMessagesApiService::class.java) }
}

object MessagesApi {
    val retrofitService: MessagesApiService by lazy { retrofit.create(MessagesApiService::class.java) }
}

object ContactsApi {
    val retrofitService: ContactsApiService by lazy { retrofit.create(ContactsApiService::class.java) }
}