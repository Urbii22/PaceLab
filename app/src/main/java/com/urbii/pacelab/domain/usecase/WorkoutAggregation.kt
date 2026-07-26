package com.urbii.pacelab.domain.usecase

import com.urbii.pacelab.domain.model.Workout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

data class WeeklySummary(val weekStart: LocalDate, val distanceMeters: Double, val durationSeconds: Double, val workoutCount: Int)

fun aggregateWeekly(workouts: Iterable<Workout>, zoneOffset: ZoneOffset = ZoneOffset.UTC): List<WeeklySummary> =
    workouts.filter { !it.startTime.isAfter(it.endTime ?: it.startTime) }.groupBy {
        it.startTime.atOffset(zoneOffset).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }.map { (week, items) -> WeeklySummary(week, items.mapNotNull { it.distanceMeters }.sum(), items.mapNotNull { it.activeDurationSeconds ?: it.elapsedDurationSeconds }.sum(), items.size) }.sortedBy { it.weekStart }
