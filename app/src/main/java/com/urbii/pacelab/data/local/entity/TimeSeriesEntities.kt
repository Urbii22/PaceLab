package com.urbii.pacelab.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "heart_rate_samples", foreignKeys = [ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["workoutId", "timestampUtc"]), Index(value = ["workoutId", "sampleIndex"], unique = true)])
data class HeartRateSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "speed_samples", foreignKeys = [ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["workoutId", "timestampUtc"]), Index(value = ["workoutId", "sampleIndex"], unique = true)])
data class SpeedSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "cadence_samples", foreignKeys = [ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["workoutId", "timestampUtc"]), Index(value = ["workoutId", "sampleIndex"], unique = true)])
data class CadenceSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "elevation_samples", foreignKeys = [ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["workoutId", "timestampUtc"]), Index(value = ["workoutId", "sampleIndex"], unique = true)])
data class ElevationSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "distance_segments", foreignKeys = [ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["workoutId", "timestampUtc"]), Index(value = ["workoutId", "sampleIndex"], unique = true)])
data class DistanceSegmentEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "vo2max_samples", foreignKeys = [ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["workoutId", "timestampUtc"]), Index(value = ["workoutId", "sampleIndex"], unique = true)])
data class Vo2MaxSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "route_points", foreignKeys = [ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["workoutId", "timestampUtc"]), Index(value = ["workoutId", "sampleIndex"], unique = true)])
data class RoutePointEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double?,
    val bearingDegrees: Double?,
    val sampleIndex: Int,
)
