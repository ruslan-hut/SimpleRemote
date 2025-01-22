package ua.com.programmer.simpleremote.http.impl

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.getBaseUrl
import ua.com.programmer.simpleremote.entity.UserOptions
import ua.com.programmer.simpleremote.http.entity.CheckRequest
import ua.com.programmer.simpleremote.http.entity.CheckResponse
import ua.com.programmer.simpleremote.http.client.HttpAuthInterceptor
import ua.com.programmer.simpleremote.http.client.HttpClientApi
import ua.com.programmer.simpleremote.http.client.TokenRefresh
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.NetworkRepository
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository,
    private val retrofitBuilder: Retrofit.Builder,
    private val httpAuthInterceptor: HttpAuthInterceptor,
    tokenRefresh: TokenRefresh
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

    init {
        tokenRefresh.setRefreshToken { runBlocking { refreshToken() } }

        _activeConnection.filterNotNull().onEach { settings ->
            handleConnectionChange(settings)
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    override val userOptions: Flow<UserOptions> = _activeOptions

    private suspend fun handleConnectionChange(settings: ConnectionSettings) {
        val baseUrl = settings.getBaseUrl()
        if (baseUrl.isBlank()) return

        httpAuthInterceptor.setCredentials(settings.user, settings.password)

        Log.d("RC_NetworkRepository", "init connection; base url: $baseUrl")
        try {
            val retrofit = retrofitBuilder.baseUrl(baseUrl).build()
            apiService = retrofit.create(HttpClientApi::class.java)

            val updatedOptions = fetchUserOptions(settings)
            updatedOptions?.let { _activeOptions.value = it }
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "Failed to update connection: ${e.message}")
            apiService = null
            _activeOptions.value = UserOptions(isEmpty = true)
        }
    }

    private suspend fun fetchUserOptions(settings: ConnectionSettings): UserOptions? {
        val response = executeCheck(settings) ?: return null
        return if (response.data.isNotEmpty()) {
            val userOptions = response.data[0]
            userOptions?.let {
                _activeOptions.value = it
                connectionRepo.save(settings.copy(userOptions = gson.toJson(it)))
            }
            userOptions
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
        return response.token
    }

    private suspend fun executeCheck(settings: ConnectionSettings): CheckResponse? {
        val body = CheckRequest(userID = settings.guid, type = "check")
        return try {
            apiService?.check(settings.guid, body)
        } catch (e: Exception) {
            Log.e("RC_NetworkRepository", "check call failed: ${e.message}")
            null
        }
    }

}