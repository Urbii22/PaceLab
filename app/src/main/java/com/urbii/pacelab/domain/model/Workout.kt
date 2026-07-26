package com.urbii.pacelab.domain.model

import java.time.Instant

data class Workout(
    val id: String,
    val exerciseType: ExerciseType,
    val startTime: Instant,
    val endTime: Instant? = null,
    val elapsedDurationSeconds: Double? = null,
    val activeDurationSeconds: Double? = null,
    val distanceMeters: Double? = null,
    val averageHeartRateBpm: Double? = null,
    val maximumHeartRateBpm: Double? = null,
    val averageSpeedMetersPerSecond: Double? = null,
    val maximumSpeedMetersPerSecond: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val vo2Max: Double? = null,
    val heartRateSamples: List<TimeSeriesSample> = emptyList(),
    val speedSamples: List<TimeSeriesSample> = emptyList(),
    val cadenceSamples: List<TimeSeriesSample> = emptyList(),
    val elevationSamples: List<TimeSeriesSample> = emptyList(),
    val distanceSamples: List<TimeSeriesSample> = emptyList(),
    val vo2MaxSamples: List<TimeSeriesSample> = emptyList(),
    val routePoints: List<RoutePoint> = emptyList(),
    val notes: String = "",
    val perceivedEffort: Int? = null,
    val feeling: String? = null,
    val isFavorite: Boolean = false,
    val sourceProvider: String? = null,
    val sourceDataOrigin: String? = null,
    val sourceRecordId: String? = null
)
