package com.urbii.pacelab.domain.model

/** A measurement at [timestamp] (seconds from workout start) and optional distance. */
data class TimeSeriesSample(
    val timestamp: Long,
    val value: Double,
    val distanceMeters: Double? = null
)
