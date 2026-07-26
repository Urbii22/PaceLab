package com.urbii.pacelab.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.urbii.pacelab.domain.model.ExerciseType

fun normalizeExerciseType(code: Int): ExerciseType = when (code) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
    -> ExerciseType.RUNNING
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ExerciseType.WALKING
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ExerciseType.HIKING
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
    -> ExerciseType.CYCLING
    else -> ExerciseType.OTHER
}

fun exerciseTypeLabel(code: Int): String = when (normalizeExerciseType(code)) {
    ExerciseType.RUNNING -> if (code == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL) "Running indoor" else "Running"
    ExerciseType.WALKING -> "Walking"
    ExerciseType.HIKING -> "Hiking"
    ExerciseType.CYCLING -> "Cycling"
    ExerciseType.OTHER -> "Other ($code)"
}
