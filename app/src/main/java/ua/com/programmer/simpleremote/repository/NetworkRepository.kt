package ua.com.programmer.simpleremote.repository

import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.UserOptions

interface NetworkRepository {
    val userOptions: Flow<UserOptions>
    fun documents(type: String): Flow<List<Document>>
}