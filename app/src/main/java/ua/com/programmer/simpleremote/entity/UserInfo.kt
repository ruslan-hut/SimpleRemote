package ua.com.programmer.simpleremote.entity

import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

data class UserInfo(
    val guid: String = "",
    val description: String = "",
    val serverAddress: String = "",
    val databaseName: String = "",
    val user: String = "",
    val password: String = "",
    val version: String = "",
    val loginDate: String = "",
){
    companion object Builder {
        fun build(connection: ConnectionSettings): UserInfo {
            return UserInfo(
                guid = connection.guid,
                description = connection.description,
                serverAddress = connection.serverAddress,
                databaseName = connection.databaseName,
                user = connection.user,
                password = connection.password,
            )
        }
    }
}
