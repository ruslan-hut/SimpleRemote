package ua.com.programmer.simpleremote.repository

import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

interface ConnectionSettingsRepository {
    val currentConnection: Flow<ConnectionSettings?>
    fun getAll(): Flow<List<ConnectionSettings>>
    fun getByGuid(guid: String): Flow<ConnectionSettings>
    suspend fun save(connection: ConnectionSettings): Long
    suspend fun delete(guid: String): Int
    suspend fun setCurrent(guid: String)
    suspend fun checkAvailableConnection()
}