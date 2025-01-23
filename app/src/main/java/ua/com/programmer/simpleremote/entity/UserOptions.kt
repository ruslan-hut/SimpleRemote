package ua.com.programmer.simpleremote.entity

import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

data class UserOptions(
    val isEmpty: Boolean = true,
    val userId: String = "",
    val write: Boolean = false,
    val read: Boolean = false,
    val mode: String = "",
    val loadImages: Boolean = false,
    val user: String = "",
    val token: String = "",
    val catalog: List<ListType> = listOf(),
    val document: List<ListType> = listOf()
)

class UserOptionsBuilder {

    private fun fromString(options: String, id: String = ""): UserOptions {
        if (options.isEmpty()) return UserOptions(isEmpty = true)
        val optionsMap = XMap(options)
        if (optionsMap.isEmpty()) return UserOptions(isEmpty = true)
        val userId = optionsMap.getString("userId").ifEmpty { id }
        return UserOptions(
            isEmpty = false,
            userId = userId,
            loadImages = optionsMap.getBoolean("loadImages"),
            read = optionsMap.getBoolean("read"),
            write = optionsMap.getBoolean("write"),
        )
    }

    fun fromConnectionSettings(settings: ConnectionSettings?): UserOptions {
        return fromString(settings?.userOptions ?: "", settings?.guid ?: "")
    }

    companion object {
        fun build(settings: ConnectionSettings?): UserOptions {
            return UserOptionsBuilder().fromConnectionSettings(settings)
        }
    }
}