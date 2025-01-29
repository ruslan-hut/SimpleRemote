package ua.com.programmer.simpleremote.http.impl

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.getBaseUrl
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.UserOptions
import ua.com.programmer.simpleremote.http.entity.CheckRequest
import ua.com.programmer.simpleremote.http.entity.CheckResponse
import ua.com.programmer.simpleremote.http.client.HttpAuthInterceptor
import ua.com.programmer.simpleremote.http.client.HttpClientApi
import ua.com.programmer.simpleremote.http.client.TokenRefresh
import ua.com.programmer.simpleremote.http.entity.DataType
import ua.com.programmer.simpleremote.http.entity.ListRequest
import ua.com.programmer.simpleremote.http.entity.isSuccessful
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.NetworkRepository
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    connectionRepo: ConnectionSettingsRepository,
    tokenRefresh: TokenRefresh,
    private val retrofitBuilder: Retrofit.Builder,
    private val httpAuthInterceptor: HttpAuthInterceptor,
) : NetworkRepository {

    private val _activeConnection = connectionRepo.currentConnection.stateIn(
        CoroutineScope(Dispatchers.IO),
        SharingStarted.Companion.Eagerly,
        null // initial value
    )
    private val _activeOptions = MutableStateFlow(UserOptions(isEmpty = true))

    private var apiService: HttpClientApi? = null
    private val tokenCounter = AtomicInteger(0)
    private val maxTokenRefresh = 3
    private val gson = Gson()
    private val logger = FirebaseCrashlytics.getInstance()

    init {
        tokenRefresh.setRefreshToken { runBlocking { refreshToken() } }

        _activeConnection.filterNotNull().onEach { settings ->
            handleConnectionChange(settings)
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    override val userOptions: Flow<UserOptions> = _activeOptions

    override fun documents(type: String): Flow<List<Document>> = flow {
        val options = _activeOptions.value
        if (options.isEmpty) {
            emit(emptyList())
            return@flow
        }

        val body = ListRequest(
            userID = options.userId,
            type = "documents",
            data = gson.toJson(DataType(type = type)).toString()
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getDocuments(options.token, body)
            if (response != null && response.isSuccessful()) {
                val documents = response.data.filterNotNull()
                emit(documents)
            } else {
                Log.e("RC_NetworkRepository", "Failed to fetch documents: ${response?.message}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while fetching documents: ${e.message}")
            logger.recordException(e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun documentContent(type: String, guid: String): Flow<List<Content>> = flow {
        val options = _activeOptions.value
        if (options.isEmpty) {
            emit(emptyList())
            return@flow
        }

        val body = ListRequest(
            userID = options.userId,
            type = "documentContent",
            data = gson.toJson(DataType(type = type, guid = guid)).toString()
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getDocumentContent(options.token, body)
            if (response != null && response.isSuccessful()) {
                val content = response.data.filterNotNull()
                emit(content)
            } else {
                Log.e("RC_NetworkRepository", "Failed to fetch content: ${response?.message}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while fetching content: ${e.message}")
            logger.recordException(e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun catalog(type: String, group: String, docGuid: String): Flow<List<Catalog>> = flow {
        val options = _activeOptions.value
        if (options.isEmpty) {
            emit(emptyList())
            return@flow
        }

        val body = ListRequest(
            userID = options.userId,
            type = "catalog",
            data = gson.toJson(
                DataType(
                    type = type,
                    group = group,
                    documentGUID = docGuid
                )
            ).toString()
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getCatalog(options.token, body)
            if (response != null && response.isSuccessful()) {
                val documents = response.data.filterNotNull()
                emit(documents)
            } else {
                Log.e("RC_NetworkRepository", "Failed to fetch catalog: ${response?.message}")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while fetching catalog: ${e.message}")
            logger.recordException(e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun barcode(type: String, guid: String, value: String): Flow<Product> = flow {
        val options = _activeOptions.value
        if (options.isEmpty) {
            emit(Product())
            return@flow
        }

        val body = ListRequest(
            userID = options.userId,
            type = "barcode",
            data = gson.toJson(DataType(type = type, guid = guid, value = value)).toString()
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getBarcode(options.token, body)
            if (response != null && response.isSuccessful()) {
                val products = response.data.filterNotNull()
                emit(products.firstOrNull() ?: Product())
            } else {
                Log.e("RC_NetworkRepository", "Failed to receive barcode: ${response?.message}")
                emit(Product())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while receiving barcode: ${e.message}")
            logger.recordException(e)
            emit(Product())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun reconnect() {
        _activeOptions.value = UserOptions(isEmpty = false)
        val settings = _activeConnection.value
        if (settings == null) {
            _activeOptions.value = UserOptions(isEmpty = true)
            return
        }
        if (apiService == null) {
            handleConnectionChange(settings)
            return
        }
        apiService?.let {
            val updatedOptions = fetchUserOptions(settings)
            _activeOptions.value = updatedOptions ?: UserOptions(isEmpty = true)
        }
    }

    private suspend fun handleConnectionChange(settings: ConnectionSettings) {
        val baseUrl = settings.getBaseUrl()
        if (baseUrl.isBlank()) return

        httpAuthInterceptor.setCredentials(settings.user, settings.password)

        Log.d("RC_NetworkRepository", "init connection; base url: $baseUrl")
        try {
            val retrofit = retrofitBuilder.baseUrl(baseUrl).build()
            apiService = retrofit.create(HttpClientApi::class.java)

            val updatedOptions = fetchUserOptions(settings)
            _activeOptions.value = updatedOptions ?: UserOptions(isEmpty = true)
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Failed to update connection: ${e.message}")
            apiService = null
            _activeOptions.value = UserOptions(isEmpty = true)
            logger.recordException(e)
        }
    }

    private suspend fun fetchUserOptions(settings: ConnectionSettings): UserOptions? {
        val response = executeCheck(settings) ?: return null
        return if (response.data.isNotEmpty()) {
            val userOptions = response.data[0]
            val options = userOptions?.copy(
                isEmpty = false,
                userId = settings.guid,
                token = response.token,
            )
            options
        } else {
            null
        }
    }

    private suspend fun refreshToken(tag: String = "interceptor"): String {
        if (tokenCounter.incrementAndGet() > maxTokenRefresh) {
            throw Exception("Token refresh limit reached")
        }
        val connection = _activeConnection.value ?: throw Exception("No active connection settings")
        val response = executeCheck(connection) ?: throw Exception("$tag: Response is null")
        Log.d("RC_NetworkRepository", "new token: ${response.token}")
        _activeOptions.apply {
            value = value.copy(isEmpty = false, userId = connection.guid, token = response.token)
        }
        return response.token
    }

    private suspend fun executeCheck(settings: ConnectionSettings): CheckResponse? {
        val body = CheckRequest(userID = settings.guid, type = "check")
        logger.log("request body: $body")
        return try {
            apiService?.check(settings.guid, body)
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "check call failed: ${e.message}")
            logger.recordException(e)
            null
        }
    }

}