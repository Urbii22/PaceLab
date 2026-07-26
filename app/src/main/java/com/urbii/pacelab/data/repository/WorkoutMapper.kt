package com.urbii.pacelab.data.repository

import com.urbii.pacelab.data.local.entity.WorkoutEntity
import com.urbii.pacelab.domain.model.ExerciseType
import com.urbii.pacelab.domain.model.RoutePoint
import com.urbii.pacelab.domain.model.TimeSeriesSample
import com.urbii.pacelab.domain.model.Workout
import java.time.Instant
import java.time.ZoneOffset

fun WorkoutEntity.toDomain(
    heartRateSamples: List<TimeSeriesSample> = emptyList(),
    speedSamples: List<TimeSeriesSample> = emptyList(),
    cadenceSamples: List<TimeSeriesSample> = emptyList(),
    elevationSamples: List<TimeSeriesSample> = emptyList(),
    distanceSamples: List<TimeSeriesSample> = emptyList(),
    vo2MaxSamples: List<TimeSeriesSample> = emptyList(),
    routePoints: List<RoutePoint> = emptyList(),
    notes: String = "",
    perceivedEffort: Int? = null,
    feeling: String? = null,
    isFavorite: Boolean = false,
): Workout = Workout(
    id = id,
    exerciseType = normalizedExerciseType.toExerciseType(),
    startTime = startTimeUtc,
    endTime = endTimeUtc,
    elapsedDurationSeconds = elapsedDurationSeconds.toDouble(),
    activeDurationSeconds = activeDurationSeconds?.toDouble(),
    distanceMeters = distanceMeters,
    averageHeartRateBpm = averageHeartRateBpm,
    maximumHeartRateBpm = maximumHeartRateBpm,
    averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
    maximumSpeedMetersPerSecond = maximumSpeedMetersPerSecond,
    totalCaloriesKcal = totalCaloriesKcal,
    vo2Max = vo2MaxMlKgMin,
    heartRateSamples = heartRateSamples,
    speedSamples = speedSamples,
    cadenceSamples = cadenceSamples,
    elevationSamples = elevationSamples,
    distanceSamples = distanceSamples,
    vo2MaxSamples = vo2MaxSamples,
    routePoints = routePoints,
    notes = notes,
    perceivedEffort = perceivedEffort,
    feeling = feeling,
    isFavorite = isFavorite,
    sourceProvider = sourceProvider,
    sourceDataOrigin = sourceDataOrigin,
    sourceRecordId = sourceRecordId,
)

private fun String.toExerciseType(): ExerciseType = when (uppercase()) {
    "RUNNING", "RUN", "EXERCISE_TYPE_RUNNING" -> ExerciseType.RUNNING
    "WALKING", "WALK" -> ExerciseType.WALKING
    "HIKING", "HIKE" -> ExerciseType.HIKING
    "CYCLING", "BIKING", "BIKE" -> ExerciseType.CYCLING
    else -> ExerciseType.OTHER
}

fun Workout.toEntity(now: Instant = Instant.now()): WorkoutEntity {
    val end = endTime ?: startTime.plusSeconds((elapsedDurationSeconds ?: 0.0).toLong())
    val duration = elapsedDurationSeconds ?: activeDurationSeconds ?: 0.0
    return WorkoutEntity(
        id = id,
        sourceProvider = sourceProvider ?: "PaceLab",
        sourceRecordId = sourceRecordId ?: id,
        sourceDataOrigin = sourceDataOrigin,
        sourceDeviceId = null,
        sourceDeviceName = null,
        sourceExerciseType = exerciseType.name,
        normalizedExerciseType = exerciseType.name,
        sourceTitle = null,
        startTimeUtc = startTime,
        endTimeUtc = end,
        zoneOffsetStart = ZoneOffset.UTC,
        zoneOffsetEnd = ZoneOffset.UTC,
        elapsedDurationSeconds = duration.toLong(),
        activeDurationSeconds = activeDurationSeconds?.toLong(),
        distanceMeters = distanceMeters,
        totalCaloriesKcal = totalCaloriesKcal,
        averageHeartRateBpm = averageHeartRateBpm,
        maximumHeartRateBpm = maximumHeartRateBpm,
        averageSpeedMetersPerSecond = averageSpeedMetersPerSecond,
        maximumSpeedMetersPerSecond = maximumSpeedMetersPerSecond,
        averagePaceSecondsPerKm = null,
        vo2MaxMlKgMin = vo2Max,
        elevationGainMeters = null,
        averageCadenceStepsPerMinute = null,
        hasRoute = false,
        fingerprint = id,
        sourceCreatedAt = null,
        sourceUpdatedAt = null,
        importedAt = now,
        lastSyncedAt = now,
    )
}
