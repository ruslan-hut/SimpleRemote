package ua.com.programmer.simpleremote.dao.impl

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ua.com.programmer.simpleremote.dao.database.DocumentCacheDao
import ua.com.programmer.simpleremote.dao.entity.CachedContentLine
import ua.com.programmer.simpleremote.dao.entity.CachedDocument
import ua.com.programmer.simpleremote.entity.Content
import ua.com.programmer.simpleremote.entity.Document
import ua.com.programmer.simpleremote.repository.CachedDocumentData
import ua.com.programmer.simpleremote.repository.DocumentCacheRepository
import javax.inject.Inject
import javax.inject.Singleton

private sealed class PendingWrite {
    abstract val cacheId: String

    data class ContentWrite(
        override val cacheId: String,
        val content: List<Content>,
    ) : PendingWrite()

    data class DocumentWrite(
        override val cacheId: String,
        val document: Document,
    ) : PendingWrite()
}

@Singleton
class DocumentCacheRepositoryImpl @Inject constructor(
    private val dao: DocumentCacheDao,
    private val gson: Gson,
) : DocumentCacheRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val pendingWrites = MutableSharedFlow<PendingWrite>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            val buffer = mutableMapOf<String, PendingWrite>()
            pendingWrites.collect { write ->
                buffer[write.cacheId] = write
                // Debounce: wait 300ms then flush all buffered writes
                delay(300)
                val snapshot = buffer.toMap()
                buffer.clear()
                for ((_, pending) in snapshot) {
                    try {
                        when (pending) {
                            is PendingWrite.ContentWrite -> {
                                val lines = pending.content.mapIndexed { index, content ->
                                    CachedContentLine(
                                        cacheId = pending.cacheId,
                                        line = index,
                                        contentJson = gson.toJson(content),
                                    )
                                }
                                dao.replaceContentLines(pending.cacheId, lines)
                                dao.updateTimestamp(pending.cacheId, System.currentTimeMillis())
                            }
                            is PendingWrite.DocumentWrite -> {
                                val now = System.currentTimeMillis()
                                val docJson = gson.toJson(pending.document.copy(lines = emptyList()))
                                dao.updateDocumentData(pending.cacheId, docJson, now)
                            }
                        }
                    } catch (e: Exception) {
                        // Silently ignore write failures — cache is best-effort
                    }
                }
            }
        }
    }

    private fun buildCacheId(connectionGuid: String, documentGuid: String): String {
        return "${connectionGuid}_${documentGuid}"
    }

    override suspend fun cacheDocument(
        connectionGuid: String,
        document: Document,
        content: List<Content>,
        documentType: String,
        documentTitle: String,
    ) {
        val cacheId = buildCacheId(connectionGuid, document.guid)
        val now = System.currentTimeMillis()
        val docJson = gson.toJson(document.copy(lines = emptyList()))
        Log.d(TAG, "cacheDocument: cacheId=$cacheId, contentSize=${content.size}, docGuid=${document.guid}, type=$documentType")

        dao.insertDocument(
            CachedDocument(
                id = cacheId,
                connectionGuid = connectionGuid,
                documentGuid = document.guid,
                documentType = documentType,
                documentTitle = documentTitle,
                documentJson = docJson,
                createdAt = now,
                updatedAt = now,
            )
        )

        val lines = content.mapIndexed { index, item ->
            CachedContentLine(
                cacheId = cacheId,
                line = index,
                contentJson = gson.toJson(item),
            )
        }
        dao.replaceContentLines(cacheId, lines)
    }

    override fun scheduleCacheContent(
        connectionGuid: String,
        documentGuid: String,
        content: List<Content>
    ) {
        val cacheId = buildCacheId(connectionGuid, documentGuid)
        pendingWrites.tryEmit(PendingWrite.ContentWrite(cacheId, content))
    }

    override fun scheduleCacheDocument(connectionGuid: String, document: Document) {
        val cacheId = buildCacheId(connectionGuid, document.guid)
        pendingWrites.tryEmit(PendingWrite.DocumentWrite(cacheId, document))
    }

    override suspend fun getCachedDocuments(connectionGuid: String): List<CachedDocumentData> {
        val cachedDocs = dao.getCachedDocuments(connectionGuid)
        Log.d(TAG, "getCachedDocuments: connectionGuid=$connectionGuid, found=${cachedDocs.size} docs")
        return cachedDocs.mapNotNull { cached ->
            try {
                val document = gson.fromJson(cached.documentJson, Document::class.java)
                val rawLines = dao.getContentLines(cached.id)
                Log.d(TAG, "getCachedDocuments: cacheId=${cached.id}, rawLines=${rawLines.size}, docGuid=${document.guid}, type=${cached.documentType}")
                val contentLines = rawLines.map { line ->
                    gson.fromJson(line.contentJson, Content::class.java)
                }
                Log.d(TAG, "getCachedDocuments: deserialized ${contentLines.size} content lines, first=${contentLines.firstOrNull()?.description}")
                CachedDocumentData(
                    document = document,
                    content = contentLines,
                    documentType = cached.documentType,
                    documentTitle = cached.documentTitle,
                )
            } catch (e: Exception) {
                Log.e(TAG, "getCachedDocuments: failed to deserialize cached=${cached.id}", e)
                dao.deleteDocument(cached.id)
                null
            }
        }
    }

    override suspend fun deleteCachedDocument(connectionGuid: String, documentGuid: String) {
        val cacheId = buildCacheId(connectionGuid, documentGuid)
        dao.deleteDocument(cacheId)
    }

    override fun cachedDocumentCount(connectionGuid: String): Flow<Int> {
        return dao.cachedDocumentCount(connectionGuid)
    }

    override suspend fun cleanupStaleCache(maxAgeHours: Long) {
        val threshold = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000)
        dao.deleteStaleDocuments(threshold)
        Log.d(TAG, "cleanupStaleCache: cleaned entries older than ${maxAgeHours}h")
    }

    companion object {
        private const val TAG = "RC_DocCache"
    }
}
