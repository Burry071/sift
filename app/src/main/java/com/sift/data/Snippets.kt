package com.sift.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "snippets")
data class Snippet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long,
    val filedAt: Long? = null,
    val status: String = "UNFILED",
    val category: String? = null,
    val title: String? = null,
    val titleIsFallback: Boolean = false,
)

@Dao
interface SnippetDao {

    @Insert
    suspend fun insert(s: Snippet): Long

    @Query("SELECT COUNT(*) FROM snippets WHERE text = :text")
    suspend fun countByText(text: String): Int

    @Query("SELECT * FROM snippets WHERE status = 'UNFILED' ORDER BY createdAt LIMIT 1")
    suspend fun nextUnfiled(): Snippet?

    @Query("UPDATE snippets SET status='FILED', category=:category, title=:title, titleIsFallback=:titleIsFallback, filedAt=:filedAt WHERE id=:id")
    suspend fun file(id: Long, category: String, title: String, titleIsFallback: Boolean, filedAt: Long)

    @Query("SELECT * FROM snippets ORDER BY createdAt DESC")
    fun all(): Flow<List<Snippet>>

    @Query("SELECT COUNT(*) FROM snippets WHERE status = 'UNFILED'")
    fun unfiledCount(): Flow<Int>
}

@Database(entities = [Snippet::class], version = 1)
abstract class SiftDb : RoomDatabase() {
    abstract fun dao(): SnippetDao
}
