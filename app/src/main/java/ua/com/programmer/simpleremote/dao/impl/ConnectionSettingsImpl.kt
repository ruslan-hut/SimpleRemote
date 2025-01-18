package ua.com.programmer.simpleremote.dao.impl

import ua.com.programmer.simpleremote.dao.database.ConnectionSettingsDao
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import javax.inject.Inject

class ConnectionSettingsImpl @Inject constructor(
   private val connectionSettingsDao: ConnectionSettingsDao
) : ConnectionSettingsRepository {

    override val currentConnection = connectionSettingsDao.getCurrent()

}