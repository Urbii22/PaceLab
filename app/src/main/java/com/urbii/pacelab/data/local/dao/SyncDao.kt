package com.urbii.pacelab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.urbii.pacelab.data.local.entity.SyncStateEntity

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_state WHERE recordType = :recordType")
    suspend fun find(recordType: String): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: SyncStateEntity)
}
