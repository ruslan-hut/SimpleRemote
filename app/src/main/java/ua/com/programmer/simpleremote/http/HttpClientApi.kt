package ua.com.programmer.simpleremote.http

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HttpClientApi {

    @POST("pst/{id}")
    suspend fun check(@Path("id") userId: String, @Body data: CheckRequest): CheckResponse

    @POST("pst/{token}")
    suspend fun post(@Path("token") token: String, @Body data: JsonObject): CheckResponse

}