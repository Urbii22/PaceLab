package com.urbii.pacelab.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

data class ExternalWorkout(
    val sourceRecordId: String,
    val sourceDataOrigin: String,
    val sourceDeviceName: String?,
    val sourceExerciseType: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
)

class HealthConnectDataSource(context: Context) {
    private val client: HealthConnectClient? = runCatching {
        HealthConnectClient.getOrCreate(context.applicationContext)
    }.getOrNull()

    suspend fun readExerciseSessions(from: Instant, to: Instant): List<ExternalWorkout> =
        client?.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(from, to),
            ),
        )?.records?.map { record ->
            ExternalWorkout(
                sourceRecordId = record.metadata.id,
                sourceDataOrigin = record.metadata.dataOrigin.packageName,
                sourceDeviceName = record.metadata.device?.model,
                sourceExerciseType = record.exerciseType.toString(),
                title = record.title,
                startTime = record.startTime,
                endTime = record.endTime,
            )
        }.orEmpty()
}
