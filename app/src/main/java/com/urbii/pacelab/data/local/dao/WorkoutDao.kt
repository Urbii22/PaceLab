package com.urbii.pacelab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.urbii.pacelab.data.local.entity.WorkoutAnnotationEntity
import com.urbii.pacelab.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE isDeleted = 0 ORDER BY startTimeUtc DESC")
    fun observeActive(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE isDeleted = 0 ORDER BY startTimeUtc DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun findById(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE sourceRecordId = :sourceRecordId")
    suspend fun findBySourceRecordId(sourceRecordId: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE fingerprint = :fingerprint")
    suspend fun findByFingerprint(fingerprint: String): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(workouts: List<WorkoutEntity>)

    @Query("SELECT id FROM workouts WHERE sourceProvider = 'Sample'")
    suspend fun syntheticWorkoutIds(): List<String>

    @Query("DELETE FROM workouts WHERE sourceProvider = 'Sample'")
    suspend fun deleteSyntheticWorkouts()

    @Query("UPDATE workouts SET isDeleted = 1, lastSyncedAt = :syncedAt WHERE sourceRecordId = :sourceRecordId")
    suspend fun markDeleted(sourceRecordId: String, syncedAt: java.time.Instant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnnotation(annotation: WorkoutAnnotationEntity)

    @Query("SELECT * FROM workout_annotations WHERE workoutId = :workoutId")
    suspend fun annotationFor(workoutId: String): WorkoutAnnotationEntity?

    @Query("DELETE FROM workout_annotations WHERE workoutId IN (SELECT id FROM workouts WHERE isDeleted = 1)")
    suspend fun deleteAnnotationsForDeletedWorkouts()
}
