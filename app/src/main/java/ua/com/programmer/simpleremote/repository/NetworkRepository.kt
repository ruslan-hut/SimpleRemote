package ua.com.programmer.simpleremote.repository

import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.entity.Catalog
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.entity.FilterItem
import ua.com.programmer.simpleremote.entity.Product
import ua.com.programmer.simpleremote.entity.UserOptions

data class DocumentsResult(
    val documents: List<Document> = emptyList(),
    val filterSchema: List<FilterItem> = emptyList(),
)

interface NetworkRepository {
    val userOptions: Flow<UserOptions>
    fun documents(type: String, filter: List<FilterItem>): Flow<DocumentsResult>
    fun documentContent(type: String, guid: String): Flow<List<Content>>
    suspend fun saveDocument(document: Document): String
    suspend fun lockDocument(type: String, guid: String): String
    suspend fun unlockDocument(type: String, guid: String): String
    fun catalog(type: String, group: String, docGuid: String): Flow<List<Catalog>>
    fun barcode(type: String, guid: String, value: String): Flow<Product>
    suspend fun reconnect()
}