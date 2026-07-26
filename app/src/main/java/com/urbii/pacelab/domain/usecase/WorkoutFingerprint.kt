package com.urbii.pacelab.domain.usecase

import com.urbii.pacelab.domain.model.Workout
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.round

fun workoutFingerprint(workout: Workout): String {
    val canonical = listOf(workout.sourceProvider.orEmpty(), workout.sourceDataOrigin.orEmpty(), workout.exerciseType.name,
        workout.startTime.toEpochMilli(), workout.endTime?.toEpochMilli() ?: 0L, round((workout.distanceMeters ?: 0.0) * 10) / 10).joinToString("|")
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

fun isDuplicateWorkout(candidate: Workout, existing: Iterable<Workout>): Boolean =
    existing.any { (candidate.sourceRecordId != null && candidate.sourceRecordId == it.sourceRecordId) || workoutFingerprint(candidate) == workoutFingerprint(it) }
