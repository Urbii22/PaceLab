package com.urbii.pacelab.data.repository

import androidx.room.withTransaction
import com.urbii.pacelab.data.healthconnect.ExternalWorkout
import com.urbii.pacelab.data.healthconnect.HealthConnectDataSource
import com.urbii.pacelab.data.local.PaceLabDatabase
import com.urbii.pacelab.data.local.entity.SyncStateEntity
import com.urbii.pacelab.domain.model.ExerciseType
import com.urbii.pacelab.domain.model.TimeSeriesSample
import com.urbii.pacelab.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.security.MessageDigest

interface WorkoutRepository {
    val workouts: Flow<List<Workout>>
    val lastSync: StateFlow<SyncStatus>
    suspend fun seedIfEmpty()
    suspend fun sync()
    suspend fun workout(id: String): Workout?
    suspend fun saveAnnotation(workoutId: String, notes: String, effort: Int?)
}

data class SyncStatus(val inProgress: Boolean = false, val message: String? = null, val error: String? = null)

class RoomWorkoutRepository(
    private val database: PaceLabDatabase,
    private val healthConnectDataSource: HealthConnectDataSource? = null,
) : WorkoutRepository {
    override val workouts: Flow<List<Workout>> = database.workoutDao().observeActive().map { entities ->
        entities.map(WorkoutEntityAdapter::toDomain)
    }

    private val _lastSync = MutableStateFlow(SyncStatus())
    override val lastSync: StateFlow<SyncStatus> = _lastSync.asStateFlow()

    override suspend fun seedIfEmpty() {
        if (database.workoutDao().latest(1).isNotEmpty()) return
        database.workoutDao().upsertAll(SampleWorkouts.all.map { it.toEntity() })
    }

    override suspend fun sync() {
        _lastSync.value = SyncStatus(inProgress = true, message = "Sincronizando Health Connect…")
        runCatching {
            val imported = healthConnectDataSource?.readExerciseSessions(
                from = Instant.now().minus(3650, ChronoUnit.DAYS),
                to = Instant.now().plusSeconds(60),
            ).orEmpty().map(ExternalWorkout::toEntity)
            database.withTransaction {
                if (imported.isNotEmpty()) database.workoutDao().upsertAll(imported)
                database.syncDao().upsert(
                    SyncStateEntity(
                        recordType = "exercise_session",
                        lastSuccessfulSyncAt = Instant.now(),
                        lastAttemptAt = Instant.now(),
                        initialImportCompleted = true,
                    ),
                )
            }
        }.onSuccess {
            _lastSync.value = SyncStatus(message = "Sincronización completada${if (healthConnectDataSource != null) " · sesiones actualizadas" else ""}")
        }.onFailure { error ->
            _lastSync.value = SyncStatus(error = error.message ?: "Error desconocido")
        }
    }

    override suspend fun workout(id: String): Workout? = database.workoutDao().findById(id)?.let(WorkoutEntityAdapter::toDomain)

    override suspend fun saveAnnotation(workoutId: String, notes: String, effort: Int?) {
        database.workoutDao().upsertAnnotation(
            com.urbii.pacelab.data.local.entity.WorkoutAnnotationEntity(
                workoutId = workoutId,
                notes = notes,
                perceivedEffort = effort,
                updatedAt = Instant.now(),
            ),
        )
    }
}

private fun ExternalWorkout.toEntity(now: Instant = Instant.now()): com.urbii.pacelab.data.local.entity.WorkoutEntity {
    val normalized = when {
        sourceExerciseType.contains("RUN", ignoreCase = true) -> ExerciseType.RUNNING
        sourceExerciseType.contains("WALK", ignoreCase = true) -> ExerciseType.WALKING
        sourceExerciseType.contains("HIKE", ignoreCase = true) -> ExerciseType.HIKING
        sourceExerciseType.contains("BIK", ignoreCase = true) || sourceExerciseType.contains("CYCL", ignoreCase = true) -> ExerciseType.CYCLING
        else -> ExerciseType.OTHER
    }
    val fingerprintInput = "$sourceDataOrigin|${normalized.name}|$startTime|$endTime"
    val fingerprint = MessageDigest.getInstance("SHA-256").digest(fingerprintInput.toByteArray()).joinToString("") { "%02x".format(it) }
    return com.urbii.pacelab.data.local.entity.WorkoutEntity(
        id = sourceRecordId,
        sourceProvider = "Health Connect",
        sourceRecordId = sourceRecordId,
        sourceDataOrigin = sourceDataOrigin,
        sourceDeviceId = null,
        sourceDeviceName = sourceDeviceName,
        sourceExerciseType = sourceExerciseType,
        normalizedExerciseType = normalized.name,
        sourceTitle = title,
        startTimeUtc = startTime,
        endTimeUtc = endTime,
        zoneOffsetStart = null,
        zoneOffsetEnd = null,
        elapsedDurationSeconds = (endTime.epochSecond - startTime.epochSecond).coerceAtLeast(0),
        activeDurationSeconds = null,
        distanceMeters = null,
        totalCaloriesKcal = null,
        averageHeartRateBpm = null,
        maximumHeartRateBpm = null,
        averageSpeedMetersPerSecond = null,
        maximumSpeedMetersPerSecond = null,
        averagePaceSecondsPerKm = null,
        vo2MaxMlKgMin = null,
        elevationGainMeters = null,
        averageCadenceStepsPerMinute = null,
        hasRoute = false,
        fingerprint = fingerprint,
        sourceCreatedAt = null,
        sourceUpdatedAt = null,
        importedAt = now,
        lastSyncedAt = now,
    )
}

private object WorkoutEntityAdapter {
    fun toDomain(entity: com.urbii.pacelab.data.local.entity.WorkoutEntity): Workout = entity.toDomain()
}

private object SampleWorkouts {
    private val now = Instant.now().truncatedTo(ChronoUnit.DAYS)

    val all: List<Workout> = listOf(
        sample("run-1", ExerciseType.RUNNING, now.minus(1, ChronoUnit.DAYS), 6000.0, 1860.0, 154.0, 42.1),
        sample("run-2", ExerciseType.RUNNING, now.minus(3, ChronoUnit.DAYS), 10200.0, 3180.0, 149.0, 43.0),
        sample("walk-1", ExerciseType.WALKING, now.minus(5, ChronoUnit.DAYS), 4200.0, 3120.0, 116.0, null),
        sample("run-3", ExerciseType.RUNNING, now.minus(8, ChronoUnit.DAYS), 8000.0, 2500.0, 151.0, 41.4),
        sample("bike-1", ExerciseType.CYCLING, now.minus(12, ChronoUnit.DAYS), 21500.0, 3600.0, 132.0, null),
        sample("hike-1", ExerciseType.HIKING, now.minus(18, ChronoUnit.DAYS), 7600.0, 5400.0, 124.0, null),
    )

    private fun sample(id: String, type: ExerciseType, start: Instant, distance: Double, duration: Double, hr: Double, vo2: Double?): Workout {
        val speed = distance / duration
        val samples = listOf(
            TimeSeriesSample(0L, hr - 8, 0.0),
            TimeSeriesSample((duration / 2).toLong(), hr, distance / 2),
            TimeSeriesSample(duration.toLong(), hr + 4, distance),
        )
        return Workout(
            id = id,
            exerciseType = type,
            startTime = start,
            endTime = start.plusSeconds(duration.toLong()),
            elapsedDurationSeconds = duration,
            activeDurationSeconds = duration,
            distanceMeters = distance,
            averageHeartRateBpm = hr,
            maximumHeartRateBpm = hr + 14,
            averageSpeedMetersPerSecond = speed,
            maximumSpeedMetersPerSecond = speed * 1.2,
            totalCaloriesKcal = distance / 10,
            vo2Max = vo2,
            heartRateSamples = samples,
            speedSamples = samples.map { TimeSeriesSample(it.timestamp, speed, it.distanceMeters) },
            sourceProvider = "Sample",
            sourceDataOrigin = "com.urbii.pacelab.sample",
            sourceRecordId = id,
        )
    }
}
