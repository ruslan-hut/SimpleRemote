package ua.com.programmer.simpleremote.dao.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.UUID

@Entity(tableName = "connection_settings", primaryKeys = ["guid"])
data class ConnectionSettings(
    val guid: String = "",
    val description: String = "",
    @ColumnInfo(name = "server_address") val serverAddress: String = "",
    @ColumnInfo(name = "database_name") val databaseName: String = "",
    val user: String = "",
    val password: String = "",
    @ColumnInfo(name = "user_options") val userOptions: String = "",
    @ColumnInfo(name = "is_current") val isCurrent: Int = 0,
    @ColumnInfo(name = "auto_connect") val autoConnect: Int = 0,
){
    companion object Builder {
        fun buildDemo(): ConnectionSettings {
            return ConnectionSettings(
                guid = UUID.randomUUID().toString(),
                description = "Demo",
                serverAddress = "hoot.com.ua",
                databaseName = "simple",
                user = "Помощник",
                password = "12qwaszx",
            )
        }
    }
}

// Returns truncated guid for log or UI
fun ConnectionSettings.getGuid(): String {
    return if (guid.length > 7) guid.subSequence(0,8).toString() else "<?>"
}