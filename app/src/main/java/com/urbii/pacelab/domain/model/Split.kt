package com.urbii.pacelab.domain.model

data class Split(
    val number: Int,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val paceSecondsPerKm: Double?,
    val averageHeartRateBpm: Double? = null,
    val maximumHeartRateBpm: Double? = null
)
