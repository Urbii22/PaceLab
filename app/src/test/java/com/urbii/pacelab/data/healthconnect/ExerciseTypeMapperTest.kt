package com.urbii.pacelab.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.urbii.pacelab.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTypeMapperTest {
    @Test fun runningVariantsAreRunning() {
        assertEquals(ExerciseType.RUNNING, normalizeExerciseType(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING))
        assertEquals(ExerciseType.RUNNING, normalizeExerciseType(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL))
    }

    @Test fun walkingHikingAndCyclingAreMapped() {
        assertEquals(ExerciseType.WALKING, normalizeExerciseType(ExerciseSessionRecord.EXERCISE_TYPE_WALKING))
        assertEquals(ExerciseType.HIKING, normalizeExerciseType(ExerciseSessionRecord.EXERCISE_TYPE_HIKING))
        assertEquals(ExerciseType.CYCLING, normalizeExerciseType(ExerciseSessionRecord.EXERCISE_TYPE_BIKING))
        assertEquals(ExerciseType.CYCLING, normalizeExerciseType(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY))
    }

    @Test fun unknownCodesRemainOther() {
        assertEquals(ExerciseType.OTHER, normalizeExerciseType(Int.MAX_VALUE))
    }
}
