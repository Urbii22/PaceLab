package com.urbii.pacelab.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneOffset

@Entity(
    tableName = "workouts",
    indices = [
        Index(value = ["sourceRecordId"], unique = true),
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["startTimeUtc"]),
        Index(value = ["normalizedExerciseType"]),
        Index(value = ["sourceDataOrigin"]),
        Index(value = ["isDeleted"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val sourceProvider: String,
    val sourceRecordId: String,
    val sourceDataOrigin: String?,
    val sourceDeviceId: String?,
    val sourceDeviceName: String?,
    val sourceExerciseType: String,
    val normalizedExerciseType: String,
    val sourceTitle: String?,
    val startTimeUtc: Instant,
    val endTimeUtc: Instant,
    val zoneOffsetStart: ZoneOffset?,
    val zoneOffsetEnd: ZoneOffset?,
    val elapsedDurationSeconds: Long,
    val activeDurationSeconds: Long?,
    val distanceMeters: Double?,
    val totalCaloriesKcal: Double?,
    val averageHeartRateBpm: Double?,
    val maximumHeartRateBpm: Double?,
    val averageSpeedMetersPerSecond: Double?,
    val maximumSpeedMetersPerSecond: Double?,
    val averagePaceSecondsPerKm: Double?,
    val vo2MaxMlKgMin: Double?,
    val elevationGainMeters: Double?,
    val averageCadenceStepsPerMinute: Double?,
    val hasRoute: Boolean,
    val fingerprint: String,
    val sourceCreatedAt: Instant?,
    val sourceUpdatedAt: Instant?,
    val importedAt: Instant,
    val lastSyncedAt: Instant,
    val isDeleted: Boolean = false,
)
