package com.urbii.pacelab.data.repository

import androidx.room.withTransaction
import com.urbii.pacelab.data.healthconnect.ExternalMetricSample
import com.urbii.pacelab.data.healthconnect.ExternalRoutePoint
import com.urbii.pacelab.data.healthconnect.ExternalWorkoutBundle
import com.urbii.pacelab.data.healthconnect.HealthConnectDataSource
import com.urbii.pacelab.data.healthconnect.HealthConnectFailureKind
import com.urbii.pacelab.data.healthconnect.HealthConnectReadResult
import com.urbii.pacelab.data.healthconnect.normalizeExerciseType
import com.urbii.pacelab.data.local.PaceLabDatabase
import com.urbii.pacelab.data.local.entity.CadenceSampleEntity
import com.urbii.pacelab.data.local.entity.DistanceSegmentEntity
import com.urbii.pacelab.data.local.entity.ElevationSampleEntity
import com.urbii.pacelab.data.local.entity.HeartRateSampleEntity
import com.urbii.pacelab.data.local.entity.RoutePointEntity
import com.urbii.pacelab.data.local.entity.SpeedSampleEntity
import com.urbii.pacelab.data.local.entity.SyncStateEntity
import com.urbii.pacelab.data.local.entity.Vo2MaxSampleEntity
import com.urbii.pacelab.data.local.entity.WorkoutAnnotationEntity
import com.urbii.pacelab.data.local.entity.WorkoutEntity
import com.urbii.pacelab.domain.model.ExerciseType
import com.urbii.pacelab.domain.model.RoutePoint
import com.urbii.pacelab.domain.model.TimeSeriesSample
import com.urbii.pacelab.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

interface WorkoutRepository {
    val workouts: Flow<List<Workout>>
    val lastSync: StateFlow<SyncStatus>
    suspend fun sync()
    suspend fun workout(id: String): Workout?
    suspend fun saveAnnotation(workoutId: String, notes: String, effort: Int?, feeling: String? = null, favorite: Boolean = false)
}

data class SyncStatus(
    val inProgress: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val warnings: List<String> = emptyList(),
)

class RoomWorkoutRepository(
    private val database: PaceLabDatabase,
    private val healthConnectDataSource: HealthConnectDataSource? = null,
) : WorkoutRepository {
    override val workouts: Flow<List<Workout>> = database.workoutDao().observeActive().map { entities ->
        entities.map(WorkoutEntity::toDomain)
    }

    private val _lastSync = MutableStateFlow(SyncStatus())
    override val lastSync: StateFlow<SyncStatus> = _lastSync.asStateFlow()

    override suspend fun sync() {
        _lastSync.value = SyncStatus(inProgress = true, message = "Sincronizando Health Connect…")
        val source = healthConnectDataSource
        if (source == null) {
            val message = "Health Connect no está configurado."
            persistSyncFailure(HealthConnectFailureKind.UNAVAILABLE, message)
            _lastSync.value = SyncStatus(error = message)
            return
        }

        val previousSync = database.syncDao().find("exercise_session")
        val currentToken = source.currentExerciseSessionChangeToken()
        if (previousSync?.initialImportCompleted == true && previousSync.changeToken != null && currentToken != null && previousSync.changeToken == currentToken) {
            _lastSync.value = SyncStatus(message = "Sincronización correcta: no hay cambios.")
            return
        }

        if (previousSync?.initialImportCompleted == true && previousSync.changeToken != null) {
            when (val changes = source.readExerciseSessionChanges(previousSync.changeToken)) {
                is HealthConnectReadResult.Success -> {
                    val changed = changes.value
                    if (changed.upserted.isEmpty() && changed.deletedRecordIds.isEmpty()) {
                        persistSuccessfulSync(changed.nextToken)
                        return
                    }
                    val bundles = if (changed.upserted.isEmpty()) {
                        HealthConnectReadResult.Success(emptyList<ExternalWorkoutBundle>())
                    } else {
                        source.readWorkoutBundles(
                            from = changed.upserted.minOf { it.startTime }.minusSeconds(1),
                            to = changed.upserted.maxOf { it.endTime }.plusSeconds(1),
                        )
                    }
                    when (bundles) {
                        is HealthConnectReadResult.Failure -> {
                            persistSyncFailure(bundles.kind, bundles.message)
                            _lastSync.value = SyncStatus(error = bundles.message)
                        }
                        is HealthConnectReadResult.Success -> {
                            runCatching {
                                commitSync(
                                    bundles = bundles.value,
                                    deletedRecordIds = changed.deletedRecordIds,
                                    token = changed.nextToken,
                                )
                            }.onSuccess {
                                _lastSync.value = SyncStatus(
                                    message = "Sincronización incremental completada: ${bundles.value.size} actualizadas, ${changed.deletedRecordIds.size} eliminadas.",
                                    warnings = bundles.warnings,
                                )
                            }.onFailure { error ->
                                persistSyncFailure(HealthConnectFailureKind.READ_ERROR, error.message ?: "No se pudo guardar la importación.")
                                _lastSync.value = SyncStatus(error = error.message ?: "No se pudo guardar la importación.")
                            }
                        }
                    }
                    return
                }
                is HealthConnectReadResult.Failure -> if (changes.kind != HealthConnectFailureKind.CHANGES_TOKEN_EXPIRED) {
                    persistSyncFailure(changes.kind, changes.message)
                    _lastSync.value = SyncStatus(error = changes.message)
                    return
                }
            }
        }

        when (val result = source.readWorkoutBundles(
            from = Instant.now().minus(3650, ChronoUnit.DAYS),
            to = Instant.now().plusSeconds(60),
        )) {
            is HealthConnectReadResult.Failure -> {
                persistSyncFailure(result.kind, result.message)
                _lastSync.value = SyncStatus(error = result.message)
            }
            is HealthConnectReadResult.Success -> {
                runCatching {
                    commitSync(result.value, emptyList(), source.currentExerciseSessionChangeToken())
                }.onSuccess {
                    _lastSync.value = SyncStatus(
                        message = if (result.value.isEmpty()) "Sincronización correcta: no hay actividades en el periodo." else "Sincronización completada: ${result.value.size} actividades.",
                        warnings = result.warnings,
                    )
                }.onFailure { error ->
                    persistSyncFailure(HealthConnectFailureKind.READ_ERROR, error.message ?: "No se pudo guardar la importación.")
                    _lastSync.value = SyncStatus(error = error.message ?: "No se pudo guardar la importación.")
                }
            }
        }
    }

    private suspend fun commitSync(
        bundles: List<ExternalWorkoutBundle>,
        deletedRecordIds: List<String>,
        token: String?,
    ) {
        database.withTransaction {
            val syntheticIds = database.workoutDao().syntheticWorkoutIds()
            syntheticIds.forEach { database.timeSeriesDao().deleteAll(it) }
            database.workoutDao().deleteSyntheticWorkouts()
            deletedRecordIds.forEach { recordId ->
                database.timeSeriesDao().deleteAll(recordId)
                database.workoutDao().markDeleted(recordId, Instant.now())
            }
            bundles.forEach { bundle -> upsertBundle(bundle) }
            database.syncDao().upsert(
                SyncStateEntity(
                    recordType = "exercise_session",
                    changeToken = token,
                    lastSuccessfulSyncAt = Instant.now(),
                    lastAttemptAt = Instant.now(),
                    lastErrorCode = null,
                    lastErrorMessage = null,
                    initialImportCompleted = true,
                ),
            )
        }
    }

    private suspend fun persistSuccessfulSync(token: String?) {
        commitSync(emptyList(), emptyList(), token)
        _lastSync.value = SyncStatus(message = "Sincronización correcta: no hay cambios.")
    }

    override suspend fun workout(id: String): Workout? {
        val entity = database.workoutDao().findById(id) ?: return null
        val series = database.timeSeriesDao()
        val annotation = database.workoutDao().annotationFor(id)
        return entity.toDomain(
            heartRateSamples = series.heartRate(id).map { it.toDomain(entity.startTimeUtc) },
            speedSamples = series.speed(id).map { it.toDomain(entity.startTimeUtc) },
            cadenceSamples = series.cadence(id).map { it.toDomain(entity.startTimeUtc) },
            elevationSamples = series.elevation(id).map { it.toDomain(entity.startTimeUtc) },
            distanceSamples = series.distance(id).map { it.toDomain(entity.startTimeUtc) },
            vo2MaxSamples = series.vo2Max(id).map { it.toDomain(entity.startTimeUtc) },
            routePoints = series.route(id).map { it.toDomain(entity.startTimeUtc) },
            notes = annotation?.notes.orEmpty(),
            perceivedEffort = annotation?.perceivedEffort,
            feeling = annotation?.feeling,
            isFavorite = annotation?.isFavorite ?: false,
        )
    }

    override suspend fun saveAnnotation(workoutId: String, notes: String, effort: Int?, feeling: String?, favorite: Boolean) {
        require(effort == null || effort in 1..10) { "El esfuerzo percibido debe estar entre 1 y 10." }
        database.workoutDao().upsertAnnotation(
            WorkoutAnnotationEntity(
                workoutId = workoutId,
                notes = notes,
                perceivedEffort = effort,
                feeling = feeling,
                isFavorite = favorite,
                updatedAt = Instant.now(),
            ),
        )
    }

    private suspend fun uniqueFingerprint(session: com.urbii.pacelab.data.healthconnect.ExternalWorkout, distance: Double?): String {
        val base = stableFingerprint(session, distance)
        database.workoutDao().findBySourceRecordId(session.sourceRecordId)?.let { return it.fingerprint }
        val existing = database.workoutDao().findByFingerprint(base)
        return if (existing == null || existing.sourceRecordId == session.sourceRecordId) {
            base
        } else {
            MessageDigest.getInstance("SHA-256")
                .digest("$base|${session.sourceRecordId}".toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }

    private suspend fun upsertBundle(bundle: ExternalWorkoutBundle) {
        val now = Instant.now()
        val session = bundle.session
        val workoutId = session.sourceRecordId
        val duration = Duration.between(session.startTime, session.endTime).seconds.coerceAtLeast(0)
        val hrValues = bundle.heartRateSamples.map { it.value }
        val speedValues = bundle.speedSamples.map { it.value }
        val cadenceValues = bundle.cadenceSamples.map { it.value }
        val vo2 = bundle.vo2MaxSamples.maxByOrNull { it.timestamp }?.value
        val distance = bundle.distanceMeters
        val fingerprint = uniqueFingerprint(session, distance)
        database.workoutDao().upsert(
            WorkoutEntity(
                id = workoutId,
                sourceProvider = "Health Connect",
                sourceRecordId = workoutId,
                sourceDataOrigin = session.sourceDataOrigin,
                sourceDeviceId = session.sourceDeviceId,
                sourceDeviceName = session.sourceDeviceName,
                sourceExerciseType = session.exerciseType.toString(),
                normalizedExerciseType = normalizeExerciseType(session.exerciseType).name,
                sourceTitle = session.title,
                startTimeUtc = session.startTime,
                endTimeUtc = session.endTime,
                zoneOffsetStart = session.startZoneOffset,
                zoneOffsetEnd = session.endZoneOffset,
                elapsedDurationSeconds = duration,
                activeDurationSeconds = null,
                distanceMeters = distance,
                totalCaloriesKcal = bundle.caloriesKcal,
                averageHeartRateBpm = hrValues.averageOrNull(),
                maximumHeartRateBpm = hrValues.maxOrNull(),
                averageSpeedMetersPerSecond = speedValues.averageOrNull(),
                maximumSpeedMetersPerSecond = speedValues.maxOrNull(),
                averagePaceSecondsPerKm = paceSeconds(duration, distance),
                vo2MaxMlKgMin = vo2,
                elevationGainMeters = bundle.elevationGainMeters,
                averageCadenceStepsPerMinute = cadenceValues.averageOrNull(),
                hasRoute = bundle.route.isNotEmpty(),
                fingerprint = fingerprint,
                sourceCreatedAt = null,
                sourceUpdatedAt = null,
                importedAt = now,
                lastSyncedAt = now,
            ),
        )
        val series = database.timeSeriesDao()
        series.deleteAll(workoutId)
        series.insertHeartRate(bundle.heartRateSamples.toHeartRateEntities(workoutId, session.startTime))
        series.insertSpeed(bundle.speedSamples.toSpeedEntities(workoutId, session.startTime))
        series.insertCadence(bundle.cadenceSamples.toCadenceEntities(workoutId, session.startTime))
        series.insertElevation(bundle.elevationSamples.toElevationEntities(workoutId, session.startTime))
        series.insertDistance(bundle.distanceSamples.toDistanceEntities(workoutId, session.startTime))
        series.insertVo2Max(bundle.vo2MaxSamples.toVo2Entities(workoutId, session.startTime))
        series.insertRoute(bundle.route.toRouteEntities(workoutId, session.startTime))
    }

    private suspend fun persistSyncFailure(kind: HealthConnectFailureKind, message: String) {
        database.syncDao().upsert(
            SyncStateEntity(
                recordType = "exercise_session",
                lastAttemptAt = Instant.now(),
                lastErrorCode = kind.name,
                lastErrorMessage = message,
                initialImportCompleted = database.syncDao().find("exercise_session")?.initialImportCompleted ?: false,
            ),
        )
    }
}

private fun stableFingerprint(session: com.urbii.pacelab.data.healthconnect.ExternalWorkout, distance: Double?): String {
    val canonical = listOf(
        "Health Connect",
        session.sourceDataOrigin,
        session.exerciseType,
        session.startTime,
        session.endTime,
        distance?.let { "%.1f".format(java.util.Locale.US, it) }.orEmpty(),
    ).joinToString("|")
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString("") { "%02x".format(it) }
}

private fun paceSeconds(durationSeconds: Long, distanceMeters: Double?): Double? =
    distanceMeters?.takeIf { it > 0 && durationSeconds > 0 }?.let { durationSeconds * 1000.0 / it }

private fun List<Double>.averageOrNull(): Double? = takeIf { isNotEmpty() }?.average()

private fun ExternalMetricSample.elapsed(start: Instant): Double = Duration.between(start, timestamp).toMillis() / 1000.0

private fun ExternalMetricSample.toDomain(start: Instant): TimeSeriesSample = TimeSeriesSample(elapsed(start).toLong(), value)
private fun ExternalMetricSample.toHeartRate(id: String, start: Instant, index: Int) = HeartRateSampleEntity("$id:heart_rate:$index", id, timestamp, elapsed(start), null, value, index)
private fun ExternalMetricSample.toSpeed(id: String, start: Instant, index: Int) = SpeedSampleEntity("$id:speed:$index", id, timestamp, elapsed(start), null, value, index)
private fun ExternalMetricSample.toCadence(id: String, start: Instant, index: Int) = CadenceSampleEntity("$id:cadence:$index", id, timestamp, elapsed(start), null, value, index)
private fun ExternalMetricSample.toElevation(id: String, start: Instant, index: Int) = ElevationSampleEntity("$id:elevation:$index", id, timestamp, elapsed(start), null, value, index)
private fun ExternalMetricSample.toDistance(id: String, start: Instant, index: Int) = DistanceSegmentEntity("$id:distance:$index", id, timestamp, elapsed(start), value, value, index)
private fun ExternalMetricSample.toVo2(id: String, start: Instant, index: Int) = Vo2MaxSampleEntity("$id:vo2:$index", id, timestamp, elapsed(start), null, value, index)
private fun ExternalRoutePoint.toEntity(id: String, start: Instant, index: Int) = RoutePointEntity("$id:route:$index", id, timestamp, Duration.between(start, timestamp).toMillis() / 1000.0, latitude, longitude, altitudeMeters, horizontalAccuracyMeters, null, index)

private fun List<ExternalMetricSample>.toHeartRateEntities(id: String, start: Instant) = mapIndexed { index, sample -> sample.toHeartRate(id, start, index) }
private fun List<ExternalMetricSample>.toSpeedEntities(id: String, start: Instant) = mapIndexed { index, sample -> sample.toSpeed(id, start, index) }
private fun List<ExternalMetricSample>.toCadenceEntities(id: String, start: Instant) = mapIndexed { index, sample -> sample.toCadence(id, start, index) }
private fun List<ExternalMetricSample>.toElevationEntities(id: String, start: Instant) = mapIndexed { index, sample -> sample.toElevation(id, start, index) }
private fun List<ExternalMetricSample>.toDistanceEntities(id: String, start: Instant) = mapIndexed { index, sample -> sample.toDistance(id, start, index) }
private fun List<ExternalMetricSample>.toVo2Entities(id: String, start: Instant) = mapIndexed { index, sample -> sample.toVo2(id, start, index) }
private fun List<ExternalRoutePoint>.toRouteEntities(id: String, start: Instant) = mapIndexed { index, point -> point.toEntity(id, start, index) }

private fun com.urbii.pacelab.data.local.entity.HeartRateSampleEntity.toDomain(start: Instant) = TimeSeriesSample(elapsedSeconds.toLong(), value, distanceMetersFromStart)
private fun com.urbii.pacelab.data.local.entity.SpeedSampleEntity.toDomain(start: Instant) = TimeSeriesSample(elapsedSeconds.toLong(), value, distanceMetersFromStart)
private fun com.urbii.pacelab.data.local.entity.CadenceSampleEntity.toDomain(start: Instant) = TimeSeriesSample(elapsedSeconds.toLong(), value, distanceMetersFromStart)
private fun com.urbii.pacelab.data.local.entity.ElevationSampleEntity.toDomain(start: Instant) = TimeSeriesSample(elapsedSeconds.toLong(), value, distanceMetersFromStart)
private fun com.urbii.pacelab.data.local.entity.DistanceSegmentEntity.toDomain(start: Instant) = TimeSeriesSample(elapsedSeconds.toLong(), value, distanceMetersFromStart)
private fun com.urbii.pacelab.data.local.entity.Vo2MaxSampleEntity.toDomain(start: Instant) = TimeSeriesSample(elapsedSeconds.toLong(), value, distanceMetersFromStart)
private fun com.urbii.pacelab.data.local.entity.RoutePointEntity.toDomain(start: Instant) = RoutePoint(elapsedSeconds.toLong(), latitude, longitude, altitudeMeters, horizontalAccuracyMeters, bearingDegrees)
