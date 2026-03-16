package ua.com.programmer.simpleremote.dao.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ua.com.programmer.simpleremote.dao.entity.CachedContentLine
import ua.com.programmer.simpleremote.dao.entity.CachedDocument

@Dao
interface DocumentCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: CachedDocument)

    @Query("SELECT * FROM cached_documents WHERE connection_guid = :connectionGuid")
    suspend fun getCachedDocuments(connectionGuid: String): List<CachedDocument>

    @Query("SELECT * FROM cached_documents WHERE id = :cacheId")
    suspend fun getCachedDocument(cacheId: String): CachedDocument?

    @Query("DELETE FROM cached_documents WHERE id = :cacheId")
    suspend fun deleteDocument(cacheId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentLines(lines: List<CachedContentLine>)

    @Query("DELETE FROM cached_content_lines WHERE cache_id = :cacheId")
    suspend fun deleteContentLines(cacheId: String)

    @Query("SELECT * FROM cached_content_lines WHERE cache_id = :cacheId ORDER BY line")
    suspend fun getContentLines(cacheId: String): List<CachedContentLine>

    @Query("SELECT COUNT(*) FROM cached_documents WHERE connection_guid = :connectionGuid")
    fun cachedDocumentCount(connectionGuid: String): Flow<Int>

    @Query("DELETE FROM cached_documents WHERE updated_at < :threshold")
    suspend fun deleteStaleDocuments(threshold: Long)

    @Query("UPDATE cached_documents SET updated_at = :updatedAt WHERE id = :cacheId")
    suspend fun updateTimestamp(cacheId: String, updatedAt: Long)

    @Query("UPDATE cached_documents SET document_json = :documentJson, updated_at = :updatedAt WHERE id = :cacheId")
    suspend fun updateDocumentData(cacheId: String, documentJson: String, updatedAt: Long)

    @Transaction
    suspend fun replaceContentLines(cacheId: String, lines: List<CachedContentLine>) {
        deleteContentLines(cacheId)
        insertContentLines(lines)
    }
}
