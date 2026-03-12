package ua.com.programmer.simpleremote.http.impl

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.getBaseUrl
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.FilterItem
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
import ua.com.programmer.simpleremote.http.entity.readError
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.DocumentsResult
import ua.com.programmer.simpleremote.repository.NetworkRepository
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    connectionRepo: ConnectionSettingsRepository,
    tokenRefresh: TokenRefresh,
    private val okHttpClient: OkHttpClient,
    private val httpAuthInterceptor: HttpAuthInterceptor,
) : NetworkRepository {

    private val _activeConnection = connectionRepo.currentConnection.stateIn(
        CoroutineScope(Dispatchers.IO + SupervisorJob()),
        SharingStarted.Eagerly,
        null // initial value
    )

    private val _activeOptions = MutableStateFlow(UserOptions(isEmpty = true))
    private val _networkError = Channel<String>(Channel.BUFFERED)

    override val networkError: Flow<String> = _networkError.receiveAsFlow()

    private var apiService: HttpClientApi? = null
    private var baseUrl: String = ""
    private val tokenCounter = AtomicInteger(0)
    private val maxTokenRefresh = 3
    private val logger = FirebaseCrashlytics.getInstance()

    private fun emitError(message: String) {
        _networkError.trySend(message)
    }

    private fun extractErrorMessage(e: Exception): String {
        return when (e) {
            is HttpException -> "Server error: ${e.code()}"
            else -> e.message ?: "Connection error"
        }
    }

    private val catalogCache = object : LinkedHashMap<String, List<Catalog>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Catalog>>): Boolean {
            return size > 50
        }
    }

    init {
        tokenRefresh.setRefreshToken { runBlocking(Dispatchers.IO) { refreshToken() } }

        _activeConnection.filterNotNull().onEach { settings ->
            Log.w("RC_NetworkRepository", "connection changed: ${settings.description}")
            handleConnectionChange(settings)
        }.launchIn(CoroutineScope(Dispatchers.IO + SupervisorJob()))
    }

    override val userOptions: Flow<UserOptions> = _activeOptions

    override fun documents(type: String, filter: List<FilterItem>): Flow<DocumentsResult> = flow {
        val options = _activeOptions.value
        if (options.isEmpty) {
            emit(DocumentsResult())
            return@flow
        }

        val body = ListRequest(
            userID = options.userId,
            type = "documents",
            data = DataType(type = type),
            filter = filter,
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getDocuments(options.token, body)
            if (response != null && response.isSuccessful()) {
                val documents = response.data.filterNotNull()
                emit(DocumentsResult(
                    documents = documents,
                    filterSchema = response.filter,
                ))
            } else {
                val msg = response?.message?.ifEmpty { null } ?: "Failed to fetch documents"
                Log.e("RC_NetworkRepository", "Failed to fetch documents: $msg")
                emitError(msg)
                emit(DocumentsResult())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while fetching documents: ${e.message}")
            logger.recordException(e)
            emitError(extractErrorMessage(e))
            emit(DocumentsResult())
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
            data = DataType(type = type, guid = guid),
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getDocumentContent(options.token, body)
            if (response != null && response.isSuccessful()) {
                val content = response.data.filterNotNull()
                emit(content)
            } else {
                val msg = response?.message?.ifEmpty { null } ?: "Failed to fetch content"
                Log.e("RC_NetworkRepository", "Failed to fetch content: $msg")
                emitError(msg)
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while fetching content: ${e.message}")
            logger.recordException(e)
            emitError(extractErrorMessage(e))
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveDocument(document: Document): String {
        val options = _activeOptions.value
        if (options.isEmpty) {
            return "Connection error"
        }

        val body = ListRequest(
            userID = options.userId,
            type = "saveDocument",
            data = document,
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.saveDocument(options.token, body)
            if (response != null && response.isSuccessful()) {
                return "OK"
            } else {
                val message = response?.readError() ?: "Unknown error"
                Log.e("RC_NetworkRepository", "Failed to save document: $message")
                return message
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while saving document: ${e.message}")
            logger.recordException(e)
        }
        return "Connection error"
    }

    override suspend fun lockDocument(type: String, guid: String): String {
        val options = _activeOptions.value
        if (options.isEmpty) {
            return "Connection error"
        }

        val body = ListRequest(
            userID = options.userId,
            type = "lockDocument",
            data = DataType(type = type, guid = guid),
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.lockDocument(options.token, body)
            if (response != null && response.isSuccessful()) {
                return "OK"
            } else {
                val message = response?.readError()?.ifEmpty { null } ?: "Unknown error"
                Log.e("RC_NetworkRepository", "Failed to lock document: $message")
                return message
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while locking document: ${e.message}")
            logger.recordException(e)
        }
        return "Connection error"
    }

    override suspend fun unlockDocument(type: String, guid: String): String {
        val options = _activeOptions.value
        if (options.isEmpty) {
            return "Connection error"
        }

        val body = ListRequest(
            userID = options.userId,
            type = "unlockDocument",
            data = DataType(type = type, guid = guid),
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.unlockDocument(options.token, body)
            if (response != null && response.isSuccessful()) {
                return "OK"
            } else {
                val message = response?.readError()?.ifEmpty { null } ?: "Unknown error"
                Log.e("RC_NetworkRepository", "Failed to unlock document: $message")
                return message
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while unlocking document: ${e.message}")
            logger.recordException(e)
        }
        return "Connection error"
    }

    override fun catalog(type: String, group: String, docGuid: String, searchFilter: String): Flow<List<Catalog>> = flow {
        val options = _activeOptions.value
        if (options.isEmpty) {
            emit(emptyList())
            return@flow
        }

        val cacheKey = "$type|$group|$searchFilter"
        val cached = synchronized(catalogCache) { catalogCache[cacheKey] }
        if (cached != null) {
            emit(cached)
            return@flow
        }

        val body = ListRequest(
            userID = options.userId,
            type = "catalog",
            data = DataType(
                type = type,
                group = group,
                documentGUID = docGuid,
                searchFilter = searchFilter,
            ),
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getCatalog(options.token, body)
            if (response != null && response.isSuccessful()) {
                val items = response.data.filterNotNull()
                synchronized(catalogCache) { catalogCache[cacheKey] = items }
                emit(items)
            } else {
                val msg = response?.message?.ifEmpty { null } ?: "Failed to fetch catalog"
                Log.e("RC_NetworkRepository", "Failed to fetch catalog: $msg")
                emitError(msg)
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while fetching catalog: ${e.message}")
            logger.recordException(e)
            emitError(extractErrorMessage(e))
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
            data = DataType(type = type, guid = guid, value = value),
        )
        logger.log("request body: $body")

        try {
            val response = apiService?.getBarcode(options.token, body)
            if (response != null && response.isSuccessful()) {
                val products = response.data.filterNotNull()
                emit(products.firstOrNull() ?: Product())
            } else {
                val msg = response?.message?.ifEmpty { null } ?: "Failed to receive barcode"
                Log.e("RC_NetworkRepository", "Failed to receive barcode: $msg")
                emitError(msg)
                emit(Product())
            }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Error while receiving barcode: ${e.message}")
            logger.recordException(e)
            emitError(extractErrorMessage(e))
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

        Log.d("RC_NetworkRepository", "init connection; set credentials: ${settings.user}")
        httpAuthInterceptor.setCredentials(settings.user, settings.password)

        if (this.baseUrl == baseUrl) return

        synchronized(catalogCache) { catalogCache.clear() }

        Log.d("RC_NetworkRepository", "init connection; base url: $baseUrl")
        try {
            this.baseUrl = baseUrl
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .baseUrl(baseUrl)
                .build()
            apiService = retrofit.create(HttpClientApi::class.java)

            val updatedOptions = fetchUserOptions(settings)
            _activeOptions.value = updatedOptions ?: UserOptions(isEmpty = true)
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Failed to update connection: ${e.message}")
            apiService = null
            _activeOptions.value = UserOptions(isEmpty = true)
            emitError(extractErrorMessage(e))
            logger.recordException(e)
        }
    }

    private suspend fun fetchUserOptions(settings: ConnectionSettings): UserOptions? {
        val response = executeCheck(settings) ?: return null
        if (response.result != "ok") {
            val msg = response.message.ifEmpty { "Server error" }
            emitError(msg)
            return null
        }
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
            tokenCounter.set(0)
            throw Exception("Token refresh limit reached")
        }
        val connection = _activeConnection.value ?: throw Exception("No active connection settings")
        val response = executeCheck(connection) ?: throw Exception("$tag: Response is null")
        Log.d("RC_NetworkRepository", "new token: ${response.token}")
        tokenCounter.set(0)
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