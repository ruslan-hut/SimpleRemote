package ua.com.programmer.simpleremote.http.client

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import ua.com.programmer.simpleremote.http.entity.CatalogListResponse
import ua.com.programmer.simpleremote.http.entity.CheckRequest
import ua.com.programmer.simpleremote.http.entity.CheckResponse
import ua.com.programmer.simpleremote.http.entity.DocumentContentResponse
import ua.com.programmer.simpleremote.http.entity.ListRequest
import ua.com.programmer.simpleremote.http.entity.DocumentListResponse
import ua.com.programmer.simpleremote.http.entity.ProductResponse

interface HttpClientApi {

    @POST("pst/{id}")
    suspend fun check(@Path("id") userId: String, @Body data: CheckRequest): CheckResponse

    @POST("pst/{token}")
    suspend fun getDocuments(@Path("token") token: String, @Body data: ListRequest): DocumentListResponse

    @POST("pst/{token}")
    suspend fun getDocumentContent(@Path("token") token: String, @Body data: ListRequest): DocumentContentResponse

    @POST("pst/{token}")
    suspend fun getBarcode(@Path("token") token: String, @Body data: ListRequest): ProductResponse

    @POST("pst/{token}")
    suspend fun getCatalog(@Path("token") token: String, @Body data: ListRequest): CatalogListResponse

}