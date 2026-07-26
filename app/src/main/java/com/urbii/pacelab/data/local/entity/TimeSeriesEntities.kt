package com.urbii.pacelab.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "heart_rate_samples", indices = [Index(value = ["workoutId", "timestampUtc"])])
data class HeartRateSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "speed_samples", indices = [Index(value = ["workoutId", "timestampUtc"])])
data class SpeedSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "cadence_samples", indices = [Index(value = ["workoutId", "timestampUtc"])])
data class CadenceSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "elevation_samples", indices = [Index(value = ["workoutId", "timestampUtc"])])
data class ElevationSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "distance_segments", indices = [Index(value = ["workoutId", "timestampUtc"])])
data class DistanceSegmentEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "vo2max_samples", indices = [Index(value = ["workoutId", "timestampUtc"])])
data class Vo2MaxSampleEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val timestampUtc: Instant,
    val elapsedSeconds: Double,
    val distanceMetersFromStart: Double?,
    val value: Double,
    val sampleIndex: Int,
)

@Entity(tableName = "route_points", indices = [Index(value = ["workoutId", "timestampUtc"])])
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
