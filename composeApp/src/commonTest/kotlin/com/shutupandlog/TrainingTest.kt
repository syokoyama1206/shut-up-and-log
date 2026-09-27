package com.shutupandlog

import com.shutupandlog.domain.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class TrainingTest {
    @Test fun validatesFullWidthInputAndCalculatesVolume() {
        val workout = WorkoutInput(
            exercise = "ベンチプレス",
            sets = listOf(TrainingSetInput("６０．５", "１０"), TrainingSetInput("60.5", "8")),
        ).validate()
        assertEquals(60.5, workout.sets.first().weightKg)
        assertEquals(10, workout.sets.first().reps)
        assertEquals(1089.0, WorkoutEntry("id", LocalDate(2026, 9, 27), workout.exercise,
            workout.category, workout.sets, 1).volume)
    }

    @Test fun rejectsInvalidSetsAndFindsLatestPreviousWorkout() {
        assertFailsWith<IllegalArgumentException> { TrainingSetInput("-1", "10").validate() }
        assertFailsWith<IllegalArgumentException> { TrainingSetInput("10", "0").validate() }
        val old = WorkoutEntry("old", LocalDate(2026, 9, 20), "スクワット", MuscleCategory.LEGS, listOf(TrainingSet(80.0, 8)), 1)
        val latest = old.copy(id = "latest", date = LocalDate(2026, 9, 25))
        val today = old.copy(id = "today", date = LocalDate(2026, 9, 27))
        assertEquals(latest, LogData(workouts = listOf(old, latest, today)).previousWorkout("スクワット", LocalDate(2026, 9, 27)))
    }
}
