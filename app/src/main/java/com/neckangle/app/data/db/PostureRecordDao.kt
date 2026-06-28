package com.neckangle.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.neckangle.app.data.model.PostureRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PostureRecordDao {

    @Insert
    suspend fun insert(record: PostureRecord)

    @Query("SELECT * FROM records WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getRecordsInRange(start: Long, end: Long): Flow<List<PostureRecord>>

    @Query("DELETE FROM records WHERE timestamp BETWEEN :start AND :end")
    suspend fun deleteInRange(start: Long, end: Long)

    @Query("DELETE FROM records")
    suspend fun deleteAll()
}
