package ua.com.programmer.simpleremote.repository

import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.entity.UserOptions

interface NetworkRepository {
    val userOptions: Flow<UserOptions>
}