package ua.com.programmer.simpleremote.dao.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_documents")
data class CachedDocument(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "connection_guid") val connectionGuid: String,
    @ColumnInfo(name = "document_guid") val documentGuid: String,
    @ColumnInfo(name = "document_type") val documentType: String,
    @ColumnInfo(name = "document_title") val documentTitle: String,
    @ColumnInfo(name = "document_json") val documentJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
