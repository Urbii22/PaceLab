package com.urbii.pacelab.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient

enum class HealthConnectAvailability { AVAILABLE, PROVIDER_UPDATE_REQUIRED, UNAVAILABLE }

class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext
    val client: HealthConnectClient? = runCatching { HealthConnectClient.getOrCreate(appContext) }.getOrNull()

    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(appContext)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED
        else -> HealthConnectAvailability.UNAVAILABLE
    }

    suspend fun grantedPermissions(): Set<String> = client?.permissionController?.getGrantedPermissions().orEmpty()

    companion object {
        val requiredReadPermissions: Set<String> = setOf(
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_DISTANCE",
            "android.permission.health.READ_SPEED",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED",
            "android.permission.health.READ_VO2_MAX",
            "android.permission.health.READ_ELEVATION_GAINED",
            "android.permission.health.READ_CADENCE",
            "android.permission.health.READ_EXERCISE_ROUTE",
        )
    }
}
