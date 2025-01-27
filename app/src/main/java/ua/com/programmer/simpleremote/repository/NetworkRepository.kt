package ua.com.programmer.simpleremote.repository

import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.UserOptions

interface NetworkRepository {
    val userOptions: Flow<UserOptions>
    fun documents(type: String): Flow<List<Document>>
    fun documentContent(type: String, guid: String): Flow<List<Content>>
    fun catalog(type: String, group: String, docGuid: String): Flow<List<Catalog>>
    fun barcode(type: String, guid: String, value: String): Flow<Product>
    suspend fun reconnect()
}