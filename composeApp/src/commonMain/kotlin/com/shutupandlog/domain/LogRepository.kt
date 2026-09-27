package com.shutupandlog.domain

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface LogRepository {
    val data: StateFlow<LogData>
    suspend fun load()
    suspend fun save(input: ValidFood, date: LocalDate)
    suspend fun deleteMeal(id: String)
    suspend fun saveWorkout(input: ValidWorkout, date: LocalDate)
    suspend fun deleteWorkout(id: String)
}

fun LogData.previousWorkout(exercise: String, before: LocalDate): WorkoutEntry? = workouts
    .filter { it.exercise == exercise && it.date < before }
    .maxWithOrNull(compareBy<WorkoutEntry> { it.date }.thenBy { it.createdAt })

fun suggestFoods(foods: List<FoodMaster>, query: String): List<FoodMaster> {
    val term = query.trim()
    return foods.filter { it.name.contains(term, ignoreCase = true) }
        .sortedWith(compareByDescending<FoodMaster> { it.name.startsWith(term, ignoreCase = true) }.thenByDescending { it.updatedAt })
        .take(6)
}
