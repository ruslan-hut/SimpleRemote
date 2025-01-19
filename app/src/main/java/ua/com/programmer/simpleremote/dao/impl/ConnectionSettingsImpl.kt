package ua.com.programmer.simpleremote.dao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.com.programmer.simpleremote.dao.database.ConnectionSettingsDao
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import javax.inject.Inject

class ConnectionSettingsImpl @Inject constructor(
   private val connectionSettingsDao: ConnectionSettingsDao
) : ConnectionSettingsRepository {

    override val currentConnection = connectionSettingsDao.getCurrent()

    override fun getAll(): Flow<List<ConnectionSettings>> {
        return connectionSettingsDao.getAll()
    }

    override fun getByGuid(guid: String): Flow<ConnectionSettings> {
        return connectionSettingsDao.getByGuid(guid).map {
            it ?: ConnectionSettings(guid = guid)
        }
    }

    override suspend fun save(connection: ConnectionSettings): Long {
        return connectionSettingsDao.insert(connection)
    }

    override suspend fun delete(guid: String): Int {
        return connectionSettingsDao.delete(guid)
    }

    override suspend fun setCurrent(guid: String) {
        if (guid.isEmpty()) return
        connectionSettingsDao.resetIsCurrent()
        connectionSettingsDao.setIsCurrent(guid)
    }

}