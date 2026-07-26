package com.urbii.pacelab.domain.usecase

import com.urbii.pacelab.domain.model.Split
import com.urbii.pacelab.domain.model.TimeSeriesSample
import kotlin.math.floor

/** Calculates kilometre splits from cumulative distance/time samples. */
fun calculateSplits(samples: List<TimeSeriesSample>, kilometreMeters: Double = 1000.0): List<Split> {
    if (samples.size < 2 || kilometreMeters <= 0) return emptyList()
    val sorted = samples.sortedBy { it.timestamp }
    val result = mutableListOf<Split>()
    var previousDistance = sorted.first().distanceMeters ?: return emptyList()
    var previousTime = sorted.first().timestamp.toDouble()
    var nextBoundary = (floor(previousDistance / kilometreMeters) + 1) * kilometreMeters
    var number = 1
    var splitStartTime = previousTime
    for (sample in sorted.drop(1)) {
        val distance = sample.distanceMeters ?: continue
        if (distance < previousDistance) { previousDistance = distance; previousTime = sample.timestamp.toDouble(); continue }
        while (distance >= nextBoundary) {
            val span = (distance - previousDistance).takeIf { it > 0 } ?: break
            val fraction = (nextBoundary - previousDistance) / span
            val boundaryTime = previousTime + (sample.timestamp - previousTime) * fraction
            val duration = boundaryTime - splitStartTime
            result += Split(number++, kilometreMeters, duration, paceSecondsPerKm(duration, kilometreMeters))
            previousTime = boundaryTime
            splitStartTime = boundaryTime
            previousDistance = nextBoundary
            nextBoundary += kilometreMeters
        }
        previousDistance = distance
        previousTime = sample.timestamp.toDouble()
    }
    return result
}
