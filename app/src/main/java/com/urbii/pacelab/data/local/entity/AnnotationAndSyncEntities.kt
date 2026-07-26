package com.urbii.pacelab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "workout_annotations")
data class WorkoutAnnotationEntity(
    @PrimaryKey val workoutId: String,
    val notes: String = "",
    val perceivedEffort: Int? = null,
    val feeling: String? = null,
    val isFavorite: Boolean = false,
    val updatedAt: Instant,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val recordType: String,
    val changeToken: String? = null,
    val lastSuccessfulSyncAt: Instant? = null,
    val lastAttemptAt: Instant? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val initialImportCompleted: Boolean = false,
)
