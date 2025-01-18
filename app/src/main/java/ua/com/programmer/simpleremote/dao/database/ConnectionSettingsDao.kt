package ua.com.programmer.simpleremote.dao.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

@Dao
interface ConnectionSettingsDao {
    @Query("SELECT * FROM connection_settings WHERE is_current=1 LIMIT 1")
    fun getCurrent(): Flow<ConnectionSettings?>
}