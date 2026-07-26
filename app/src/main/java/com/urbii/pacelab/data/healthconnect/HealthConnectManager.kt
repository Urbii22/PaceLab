package com.urbii.pacelab.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord

enum class HealthConnectAvailability { AVAILABLE, PROVIDER_UPDATE_REQUIRED, UNAVAILABLE }

enum class HealthDataHistorySupport { AVAILABLE, UNAVAILABLE, UNKNOWN }

class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext
    val client: HealthConnectClient? = runCatching { HealthConnectClient.getOrCreate(appContext) }.getOrNull()

    fun availability(): HealthConnectAvailability = runCatching {
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED
            else -> HealthConnectAvailability.UNAVAILABLE
        }
    }.getOrDefault(HealthConnectAvailability.UNAVAILABLE)

    @OptIn(ExperimentalFeatureAvailabilityApi::class)
    fun healthDataHistorySupport(): HealthDataHistorySupport = when {
        client == null -> HealthDataHistorySupport.UNKNOWN
        client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE -> HealthDataHistorySupport.AVAILABLE
        else -> HealthDataHistorySupport.UNAVAILABLE
    }

    suspend fun grantedPermissions(): Set<String> = client?.permissionController?.getGrantedPermissions().orEmpty()

    companion object {
        const val READ_EXERCISE_ROUTES = "android.permission.health.READ_EXERCISE_ROUTES"

        private val metricReadPermissions: Set<String> = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(Vo2MaxRecord::class),
            HealthPermission.getReadPermission(ElevationGainedRecord::class),
            HealthPermission.getReadPermission(StepsCadenceRecord::class),
            HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class),
            READ_EXERCISE_ROUTES,
        )

        val requiredReadPermissions: Set<String>
            get() = metricReadPermissions + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

        fun readPermissions(includeHistory: Boolean): Set<String> =
            if (includeHistory) requiredReadPermissions else metricReadPermissions
    }
}
