package ua.com.programmer.simpleremote.dao.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_content_lines",
    foreignKeys = [ForeignKey(
        entity = CachedDocument::class,
        parentColumns = ["id"],
        childColumns = ["cache_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["cache_id", "line"])]
)
data class CachedContentLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cache_id") val cacheId: String,
    val line: Int,
    @ColumnInfo(name = "content_json") val contentJson: String,
)
