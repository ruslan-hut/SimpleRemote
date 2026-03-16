package ua.com.programmer.simpleremote.repository

import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document

data class CachedDocumentData(
    val document: Document,
    val content: List<Content>,
    val documentType: String,
    val documentTitle: String,
) {
    val displayTitle: String
        get() = documentTitle.ifEmpty { document.title.ifEmpty { document.number } }
}

interface DocumentCacheRepository {
    suspend fun cacheDocument(connectionGuid: String, document: Document, content: List<Content>, documentType: String, documentTitle: String)
    fun scheduleCacheContent(connectionGuid: String, documentGuid: String, content: List<Content>)
    fun scheduleCacheDocument(connectionGuid: String, document: Document)
    suspend fun getCachedDocuments(connectionGuid: String): List<CachedDocumentData>
    suspend fun deleteCachedDocument(connectionGuid: String, documentGuid: String)
    fun cachedDocumentCount(connectionGuid: String): Flow<Int>
    suspend fun cleanupStaleCache(maxAgeHours: Long = 72)
}
