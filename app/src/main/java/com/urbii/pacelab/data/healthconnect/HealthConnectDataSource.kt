package com.urbii.pacelab.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass

enum class HealthConnectFailureKind {
    UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    PERMISSION_DENIED,
    EMPTY,
    READ_ERROR,
    CHANGES_TOKEN_EXPIRED,
}

sealed interface HealthConnectReadResult<out T> {
    data class Success<T>(val value: T, val warnings: List<String> = emptyList()) : HealthConnectReadResult<T>
    data class Failure(val kind: HealthConnectFailureKind, val message: String, val cause: Throwable? = null) : HealthConnectReadResult<Nothing>
}

data class ExternalMetricSample(
    val timestamp: Instant,
    val value: Double,
    val sourceRecordId: String,
)

data class ExternalRoutePoint(
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Double?,
    val verticalAccuracyMeters: Double?,
)

data class ExternalWorkout(
    val sourceRecordId: String,
    val sourceDataOrigin: String,
    val sourceDeviceId: String?,
    val sourceDeviceName: String?,
    val exerciseType: Int,
    val title: String?,
    val notes: String?,
    val startTime: Instant,
    val endTime: Instant,
    val startZoneOffset: ZoneOffset?,
    val endZoneOffset: ZoneOffset?,
    val laps: List<ExternalLap> = emptyList(),
    val segments: List<ExternalSegment> = emptyList(),
)

data class ExternalLap(val startTime: Instant, val endTime: Instant, val lengthMeters: Double)
data class ExternalSegment(val startTime: Instant, val endTime: Instant, val segmentType: Int, val repetitions: Int)

data class ExternalWorkoutBundle(
    val session: ExternalWorkout,
    val heartRateSamples: List<ExternalMetricSample> = emptyList(),
    val speedSamples: List<ExternalMetricSample> = emptyList(),
    val distanceSamples: List<ExternalMetricSample> = emptyList(),
    val cadenceSamples: List<ExternalMetricSample> = emptyList(),
    val elevationSamples: List<ExternalMetricSample> = emptyList(),
    val vo2MaxSamples: List<ExternalMetricSample> = emptyList(),
    val distanceMeters: Double? = null,
    val caloriesKcal: Double? = null,
    val elevationGainMeters: Double? = null,
    val route: List<ExternalRoutePoint> = emptyList(),
)

data class ExternalSessionChanges(
    val upserted: List<ExternalWorkout>,
    val deletedRecordIds: List<String>,
    val nextToken: String,
)

class HealthConnectDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val client: HealthConnectClient? = runCatching {
        HealthConnectClient.getOrCreate(appContext)
    }.getOrNull()

    suspend fun readWorkoutBundles(from: Instant, to: Instant): HealthConnectReadResult<List<ExternalWorkoutBundle>> {
        availabilityFailure()?.let { return it }
        val healthConnect = client ?: return HealthConnectReadResult.Failure(
            HealthConnectFailureKind.UNAVAILABLE,
            "Health Connect no está disponible en este dispositivo.",
        )
        return runCatching {
            val sessions = readAll<ExerciseSessionRecord>(healthConnect, from, to)
            if (sessions.isEmpty()) return@runCatching HealthConnectReadResult.Success(emptyList<ExternalWorkoutBundle>())

            val warnings = mutableListOf<String>()
            val heartRates = optionalRead(healthConnect, from, to, HeartRateRecord::class, warnings)
            val speeds = optionalRead(healthConnect, from, to, SpeedRecord::class, warnings)
            val distances = optionalRead(healthConnect, from, to, DistanceRecord::class, warnings)
            val calories = optionalRead(healthConnect, from, to, TotalCaloriesBurnedRecord::class, warnings)
            val vo2 = optionalRead(healthConnect, from, to, Vo2MaxRecord::class, warnings)
            val elevations = optionalRead(healthConnect, from, to, ElevationGainedRecord::class, warnings)
            val stepsCadence = optionalRead(healthConnect, from, to, StepsCadenceRecord::class, warnings)
            val cyclingCadence = optionalRead(healthConnect, from, to, CyclingPedalingCadenceRecord::class, warnings)

            val bundles = sessions.map { session ->
                val external = session.toExternal()
                val heartSamples = heartRates.flatMap { record ->
                    if (overlaps(record.startTime, record.endTime, session.startTime, session.endTime)) record.samples.map { sample ->
                        ExternalMetricSample(sample.time, sample.beatsPerMinute.toDouble(), record.metadata.id)
                    } else emptyList()
                }.filter { it.timestamp in session.startTime..session.endTime }
                val speedSamples = speeds.flatMap { record ->
                    if (overlaps(record.startTime, record.endTime, session.startTime, session.endTime)) record.samples.map { sample ->
                        ExternalMetricSample(sample.time, sample.speed.inMetersPerSecond, record.metadata.id)
                    } else emptyList()
                }.filter { it.timestamp in session.startTime..session.endTime }
                val distanceRecords = distances.filter { overlaps(it.startTime, it.endTime, session.startTime, session.endTime) }
                val calorieRecords = calories.filter { overlaps(it.startTime, it.endTime, session.startTime, session.endTime) }
                val vo2Samples = vo2.filter { it.time in session.startTime.minusSeconds(600)..session.endTime.plusSeconds(600) }
                    .map { ExternalMetricSample(it.time, it.vo2MillilitersPerMinuteKilogram, it.metadata.id) }
                val elevationRecords = elevations.filter { overlaps(it.startTime, it.endTime, session.startTime, session.endTime) }
                val cadenceSamples = stepsCadence.flatMap { record ->
                    if (overlaps(record.startTime, record.endTime, session.startTime, session.endTime)) record.samples.map { sample ->
                        ExternalMetricSample(sample.time, sample.rate, record.metadata.id)
                    } else emptyList()
                }.plus(cyclingCadence.flatMap { record ->
                    if (overlaps(record.startTime, record.endTime, session.startTime, session.endTime)) record.samples.map { sample ->
                        ExternalMetricSample(sample.time, sample.revolutionsPerMinute, record.metadata.id)
                    } else emptyList()
                }).filter { it.timestamp in session.startTime..session.endTime }
                val route = runCatching {
                    when (val routeResult = session.exerciseRouteResult) {
                        is ExerciseRouteResult.Data -> routeResult.exerciseRoute.route.map { location ->
                            ExternalRoutePoint(
                                timestamp = location.time,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                altitudeMeters = location.altitude?.inMeters,
                                horizontalAccuracyMeters = location.horizontalAccuracy?.inMeters,
                                verticalAccuracyMeters = location.verticalAccuracy?.inMeters,
                            )
                        }
                        else -> emptyList()
                    }
                }.getOrElse {
                    warnings += "No se pudo leer la ruta de ${session.metadata.id}."
                    emptyList()
                }
                ExternalWorkoutBundle(
                    session = external,
                    heartRateSamples = heartSamples,
                    speedSamples = speedSamples,
                    distanceSamples = distanceRecords.map { record ->
                        ExternalMetricSample(record.endTime, record.distance.inMeters, record.metadata.id)
                    },
                    cadenceSamples = cadenceSamples,
                    elevationSamples = elevationRecords.map { record ->
                        ExternalMetricSample(record.endTime, record.elevation.inMeters, record.metadata.id)
                    },
                    vo2MaxSamples = vo2Samples,
                    distanceMeters = distanceRecords.sumOf { it.distance.inMeters }.takeIf { it > 0 },
                    caloriesKcal = calorieRecords.sumOf { it.energy.inKilocalories }.takeIf { it > 0 },
                    elevationGainMeters = elevationRecords.sumOf { it.elevation.inMeters }.takeIf { it > 0 },
                    route = route,
                )
            }
            HealthConnectReadResult.Success(bundles, warnings)
        }.getOrElse { error ->
            val kind = if (error is SecurityException) HealthConnectFailureKind.PERMISSION_DENIED else HealthConnectFailureKind.READ_ERROR
            HealthConnectReadResult.Failure(kind, error.message ?: "No se pudieron leer los datos de Health Connect.", error)
        }
    }

    suspend fun readExerciseSessions(from: Instant, to: Instant): HealthConnectReadResult<List<ExternalWorkout>> =
        when (val result = readWorkoutBundles(from, to)) {
            is HealthConnectReadResult.Success -> HealthConnectReadResult.Success(result.value.map { it.session }, result.warnings)
            is HealthConnectReadResult.Failure -> result
        }

    suspend fun readExerciseSessionChanges(token: String): HealthConnectReadResult<ExternalSessionChanges> {
        availabilityFailure()?.let { return it }
        val healthConnect = client ?: return HealthConnectReadResult.Failure(
            HealthConnectFailureKind.UNAVAILABLE,
            "Health Connect no está disponible en este dispositivo.",
        )
        return runCatching {
            val upserted = mutableListOf<ExternalWorkout>()
            val deleted = mutableListOf<String>()
            var nextToken = token
            var hasMore: Boolean
            do {
                val response = healthConnect.getChanges(nextToken)
                if (response.changesTokenExpired) error("Health Connect changes token expired")
                response.changes.forEach { change ->
                    when (change) {
                        is UpsertionChange -> {
                            val record = change.record
                            if (record is ExerciseSessionRecord) upserted += record.toExternal()
                        }
                        is DeletionChange -> deleted += change.recordId
                    }
                }
                nextToken = response.nextChangesToken
                hasMore = response.hasMore
            } while (hasMore)
            HealthConnectReadResult.Success(
                ExternalSessionChanges(upserted.distinctBy { it.sourceRecordId }, deleted.distinct(), nextToken),
            )
        }.getOrElse { error ->
            val kind = if (error.message?.contains("expired", ignoreCase = true) == true) {
                HealthConnectFailureKind.CHANGES_TOKEN_EXPIRED
            } else if (error is SecurityException) {
                HealthConnectFailureKind.PERMISSION_DENIED
            } else {
                HealthConnectFailureKind.READ_ERROR
            }
            HealthConnectReadResult.Failure(kind, error.message ?: "No se pudieron leer los cambios de Health Connect.", error)
        }
    }

    private suspend inline fun <reified T : Record> readAll(
        healthConnect: HealthConnectClient,
        from: Instant,
        to: Instant,
    ): List<T> = readAll(healthConnect, from, to, T::class)

    private suspend fun <T : Record> readAll(
        healthConnect: HealthConnectClient,
        from: Instant,
        to: Instant,
        recordType: KClass<T>,
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val request = ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = TimeRangeFilter.between(from, to),
                pageToken = pageToken,
            )
            val response = healthConnect.readRecords(request)
            records += response.records
            pageToken = response.pageToken?.takeIf { it.isNotBlank() }
        } while (pageToken != null)
        return records
    }

    private suspend fun <T : Record> optionalRead(
        healthConnect: HealthConnectClient,
        from: Instant,
        to: Instant,
        recordType: KClass<T>,
        warnings: MutableList<String>,
    ): List<T> = runCatching { readAll(healthConnect, from, to, recordType) }.getOrElse {
        if (it is SecurityException) warnings += "Permiso no concedido para ${recordType.simpleName}."
        else warnings += "No se pudo leer ${recordType.simpleName}: ${it.message ?: "error desconocido"}."
        emptyList()
    }

    suspend fun currentExerciseSessionChangeToken(): String? = runCatching {
        client?.getChangesToken(ChangesTokenRequest(setOf(ExerciseSessionRecord::class)))
    }.getOrNull()

    private fun availabilityFailure(): HealthConnectReadResult.Failure? = runCatching {
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectReadResult.Failure(
                HealthConnectFailureKind.PROVIDER_UPDATE_REQUIRED,
                "Health Connect necesita actualizarse antes de leer datos.",
            )
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectReadResult.Failure(
                HealthConnectFailureKind.UNAVAILABLE,
                "Health Connect no está disponible en este dispositivo.",
            )
            else -> null
        }
    }.getOrNull()
}

private fun ExerciseSessionRecord.toExternal(): ExternalWorkout = ExternalWorkout(
    sourceRecordId = metadata.id,
    sourceDataOrigin = metadata.dataOrigin.packageName,
    sourceDeviceId = metadata.device?.model,
    sourceDeviceName = metadata.device?.let { "${it.manufacturer} ${it.model}".trim() },
    exerciseType = exerciseType,
    title = title,
    notes = notes,
    startTime = startTime,
    endTime = endTime,
    startZoneOffset = startZoneOffset,
    endZoneOffset = endZoneOffset,
    laps = laps.map { ExternalLap(it.startTime, it.endTime, it.length?.inMeters ?: 0.0) },
    segments = segments.map { ExternalSegment(it.startTime, it.endTime, it.segmentType, it.repetitions) },
)

private fun overlaps(recordStart: Instant, recordEnd: Instant, start: Instant, end: Instant): Boolean =
    recordStart < end && recordEnd > start
