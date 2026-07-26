package com.urbii.pacelab.domain.usecase

import com.urbii.pacelab.domain.model.HeartRateZone
import com.urbii.pacelab.domain.model.TimeSeriesSample

fun defaultHeartRateZones(maxHeartRateBpm: Int): List<HeartRateZone> {
    if (maxHeartRateBpm <= 0) return emptyList()
    val bounds = intArrayOf(0, 60, 70, 80, 90, 100)
    return (0..4).map { i ->
        HeartRateZone(i + 1, (maxHeartRateBpm * bounds[i] / 100.0).toInt(),
            if (i == 4) Int.MAX_VALUE else (maxHeartRateBpm * bounds[i + 1] / 100.0).toInt() - 1)
    }
}

fun heartRateZone(bpm: Int, zones: List<HeartRateZone>): HeartRateZone? = zones.firstOrNull { it.contains(bpm) }

/** Returns seconds spent in each zone. Samples are assumed ordered by timestamp. */
fun timeInHeartRateZones(samples: List<TimeSeriesSample>, zones: List<HeartRateZone>): Map<HeartRateZone, Double> {
    if (samples.isEmpty() || zones.isEmpty()) return emptyMap()
    val totals = zones.associateWith { 0.0 }.toMutableMap()
    samples.zipWithNext().forEach { (a, b) ->
        val seconds = (b.timestamp - a.timestamp).coerceAtLeast(0).toDouble()
        val startZone = heartRateZone(a.value.toInt(), zones)
        val endZone = heartRateZone(b.value.toInt(), zones)
        if (startZone != null && startZone == endZone) totals[startZone] = totals.getValue(startZone) + seconds
    }
    return totals
}
