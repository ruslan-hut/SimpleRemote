package ua.com.programmer.simpleremote.repository

import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

interface ConnectionSettingsRepository {
    val currentConnection: Flow<ConnectionSettings?>
}