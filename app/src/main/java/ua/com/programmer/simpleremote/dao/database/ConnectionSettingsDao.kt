package ua.com.programmer.simpleremote.dao.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

@Dao
interface ConnectionSettingsDao {

    @Query("SELECT * FROM connection_settings WHERE is_current=1 LIMIT 1")
    fun getCurrent(): Flow<ConnectionSettings?>

    @Query("SELECT * FROM connection_settings ORDER BY description")
    fun getAll(): Flow<List<ConnectionSettings>>

    @Query("SELECT * FROM connection_settings WHERE guid=:guid")
    fun getByGuid(guid: String): Flow<ConnectionSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionSettings): Long

    @Update
    suspend fun update(connection: ConnectionSettings): Int

    @Query("DELETE FROM connection_settings WHERE guid=:guid")
    suspend fun delete(guid: String): Int

    @Query("UPDATE connection_settings SET is_current=0")
    suspend fun resetIsCurrent()

    @Query("UPDATE connection_settings SET is_current=1 WHERE guid=:guid")
    suspend fun setIsCurrent(guid: String)
}