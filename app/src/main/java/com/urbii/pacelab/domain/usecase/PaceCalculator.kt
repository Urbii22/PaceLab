package com.urbii.pacelab.domain.usecase

fun paceSecondsPerKm(durationSeconds: Double?, distanceMeters: Double?): Double? =
    if (durationSeconds != null && distanceMeters != null && durationSeconds >= 0 && distanceMeters > 0) {
        durationSeconds * 1000.0 / distanceMeters
    } else null

fun speedMetersPerSecond(distanceMeters: Double?, durationSeconds: Double?): Double? =
    if (distanceMeters != null && durationSeconds != null && distanceMeters > 0 && durationSeconds > 0) {
        distanceMeters / durationSeconds
    } else null

fun paceFromWorkout(workout: com.urbii.pacelab.domain.model.Workout): Double? =
    paceSecondsPerKm(workout.activeDurationSeconds ?: workout.elapsedDurationSeconds, workout.distanceMeters)
