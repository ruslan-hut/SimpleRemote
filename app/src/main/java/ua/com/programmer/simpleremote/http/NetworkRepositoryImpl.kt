package ua.com.programmer.simpleremote.http

import android.util.Log
import android.webkit.URLUtil.isValidUrl
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.getBaseUrl
import ua.com.programmer.simpleremote.dao.entity.isDifferent
import ua.com.programmer.simpleremote.entity.UserOptions
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.NetworkRepository
import javax.inject.Inject

class NetworkRepositoryImpl @Inject constructor(
    private val connectionRepo: ConnectionSettingsRepository,
    private val retrofit: Retrofit.Builder,
    private val httpAuthInterceptor: HttpAuthInterceptor,
    tokenRefresh: TokenRefresh
): NetworkRepository {

    private var activeConnection: ConnectionSettings? = null
    private var activeOptions: UserOptions? = null
    private var token: String = ""
    private var apiService: HttpClientApi? = null
    private val gson = Gson()

    private var tokenCounter = 0
    private val maxTokenRefresh = 3

    init {

        tokenRefresh.setRefreshToken(::refreshToken)

        connectionRepo.currentConnection.onEach { settings ->
            Log.d("RC_NetworkRepository", "current connection: $settings")
            if (settings == null) return@onEach

            val baseUrl = settings.getBaseUrl()
            if (baseUrl.isBlank()) return@onEach

            if (activeConnection != null && activeConnection?.isDifferent(settings) == false) {
                return@onEach
            }
            activeOptions = null

            httpAuthInterceptor.setCredentials(settings.user, settings.password)

            try {
                val retrofit = retrofit.baseUrl(baseUrl).build()
                apiService = retrofit.create(HttpClientApi::class.java)
                activeConnection = settings
            } catch (e: Exception) {
                Log.e("RC_NetworkRepository", "current connection: ${e.message}")
                apiService = null
            }

        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    @Synchronized
    private fun refreshToken(tag: String = "interceptor"): String {
        tokenCounter++
        if (tokenCounter > maxTokenRefresh) {
            throw Exception("token refresh limit reached")
        }
        if (activeConnection == null) {
            throw Exception("no active connection settings")
        }
        val accountGuid = activeConnection?.guid ?: ""
        if (accountGuid.isEmpty()) {
            throw Exception("account guid is empty")
        }
        val response = runBlocking { apiService?.check(accountGuid) }

        if (response == null) {
            throw Exception("$tag: response is null")
        }
        if (response.result != "ok") {
            throw Exception("$tag: response error: ${response.message}")
        }
        token = response.token
        if (response.data.isNotEmpty()) {
            activeOptions = response.data[0]
            runBlocking{
                connectionRepo.save(activeConnection!!.copy(userOptions = gson.toJson(userOptions)))
            }
        }
        return response.token
    }

    override val userOptions: Flow<UserOptions> = flow {
        if (activeConnection == null) emit(UserOptions(isEmpty = true))

        if (activeOptions != null && !activeOptions!!.isEmpty) emit(activeOptions!!)

        if (activeConnection?.userOptions.isNullOrEmpty()) {
            Log.d("RC_NetworkRepository", "userOptions is empty, refreshing")
            runCatching {
                refreshToken()
                emit(readUserOptions())
                return@flow
            }.onFailure {
                Log.e("RC_NetworkRepository", "checkConnection refresh: ${it.message}")
                emit(UserOptions(isEmpty = true))
                return@flow
            }
        }

        Log.d("RC_NetworkRepository", "reading userOptions from settings")
        runCatching {
            activeOptions = gson.fromJson(activeConnection!!.userOptions, UserOptions::class.java)
            emit(activeOptions!!)
        }.onFailure {
            Log.e("RC_NetworkRepository", "checkConnection JSON: ${it.message}")
            emit(UserOptions(isEmpty = true))
        }

    }

    private fun readUserOptions(): UserOptions {
        activeConnection?.let {
            if (it.userOptions.isEmpty()) return UserOptions(isEmpty = true)
            return gson.fromJson(it.userOptions, UserOptions::class.java)
        }
        return UserOptions(isEmpty = true)
    }

}