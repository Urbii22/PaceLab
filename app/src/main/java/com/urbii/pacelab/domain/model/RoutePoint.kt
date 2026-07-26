package com.urbii.pacelab.domain.model

data class RoutePoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double? = null,
    val horizontalAccuracyMeters: Double? = null,
    val bearingDegrees: Double? = null,
)
