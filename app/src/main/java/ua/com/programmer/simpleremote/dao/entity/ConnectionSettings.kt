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

// Returns true if the connection settings are the same
// WARN use only for UI purposes
fun ConnectionSettings.isEquals(other: ConnectionSettings): Boolean {
    return guid == other.guid
            && description == other.description
            && serverAddress == other.serverAddress
            && user == other.user
            && isCurrent == other.isCurrent
}

fun ConnectionSettings.getBaseUrl(): String {
    var url = ""
    if (serverAddress.isNotBlank()) {
        url = if (serverAddress.contains("http://") || serverAddress.contains("https://")) {
            serverAddress
        }else{
            "http://$serverAddress"
        }
        if (!url.endsWith("/")) url = "$url/"
        if (databaseName.isNotBlank()) {
            url = "$url$databaseName/hs/rc/"
        }
    }
    return url
}

// check settings for changes to perform reconnection
fun ConnectionSettings.isDifferent(other: ConnectionSettings): Boolean {
    return this.guid != other.guid ||
            this.serverAddress != other.serverAddress ||
            this.databaseName != other.databaseName ||
            this.user != other.user ||
            this.password != other.password
}