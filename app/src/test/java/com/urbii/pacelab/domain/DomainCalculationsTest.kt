package com.urbii.pacelab.domain

import com.urbii.pacelab.domain.model.*
import com.urbii.pacelab.domain.usecase.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class DomainCalculationsTest {
    @Test fun paceAndSpeedHandleMissingData() {
        assertEquals(300.0, paceSecondsPerKm(300.0, 1000.0)!!, 0.001)
        assertEquals(2.0, speedMetersPerSecond(1000.0, 500.0)!!, 0.001)
        assertNull(paceSecondsPerKm(null, 1000.0))
        assertNull(speedMetersPerSecond(0.0, 20.0))
    }

    @Test fun zonesAndTimeAreCalculated() {
        val zones = defaultHeartRateZones(200)
        assertEquals(5, zones.size)
        assertEquals(2, heartRateZone(130, zones)?.number)
        val times = timeInHeartRateZones(listOf(TimeSeriesSample(0, 130.0), TimeSeriesSample(10, 130.0), TimeSeriesSample(20, 180.0)), zones)
        assertEquals(20.0, times[zones[1]]!!, 0.001)
        val withGap = timeInHeartRateZones(listOf(TimeSeriesSample(0, 130.0), TimeSeriesSample(60, 130.0)), zones)
        assertEquals(0.0, withGap[zones[1]]!!, 0.001)
    }

    @Test fun splitsUseCumulativeDistance() {
        val splits = calculateSplits(listOf(TimeSeriesSample(0, 0.0, 0.0), TimeSeriesSample(300, 0.0, 500.0), TimeSeriesSample(600, 0.0, 1000.0), TimeSeriesSample(900, 0.0, 1500.0), TimeSeriesSample(1200, 0.0, 2000.0)))
        assertEquals(2, splits.size)
        assertEquals(600.0, splits[0].durationSeconds, 0.001)
    }

    @Test fun weeklyAggregationAndFingerprintAreStable() {
        val start = Instant.parse("2026-07-20T10:00:00Z")
        val a = Workout("a", ExerciseType.RUNNING, start, distanceMeters = 5000.0, activeDurationSeconds = 1500.0, sourceProvider = "hc", sourceDataOrigin = "s")
        val b = a.copy(id = "b")
        assertEquals(workoutFingerprint(a), workoutFingerprint(b))
        assertTrue(isDuplicateWorkout(b, listOf(a)))
        val summary = aggregateWeekly(listOf(a)).single()
        assertEquals(5000.0, summary.distanceMeters, 0.001)
        assertEquals(1, summary.workoutCount)
    }
}
