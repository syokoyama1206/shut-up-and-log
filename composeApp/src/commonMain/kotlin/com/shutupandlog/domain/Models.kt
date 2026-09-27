package com.shutupandlog.domain

import kotlinx.datetime.LocalDate
import kotlin.math.round

enum class QuantityUnit(val label: String) { GRAM("g"), PIECE("個"), MILLILITER("ml"), SERVING("食") }
enum class NutritionMode { DIRECT, PER_REFERENCE }

data class Pfc(val protein: Double, val fat: Double, val carbs: Double) {
    init { require(listOf(protein, fat, carbs).all { it.isFinite() && it >= 0 }) }
    val calories: Double get() = protein * 4 + fat * 9 + carbs * 4
    operator fun times(factor: Double) = Pfc(protein * factor, fat * factor, carbs * factor)
    operator fun plus(other: Pfc) = Pfc(protein + other.protein, fat + other.fat, carbs + other.carbs)
    companion object { val Zero = Pfc(0.0, 0.0, 0.0) }
}
data class FoodMaster(
    val id: String, val name: String, val unit: QuantityUnit,
    val mode: NutritionMode, val referenceAmount: Double, val pfc: Pfc, val updatedAt: Long,
)
data class MealEntry(
    val id: String, val foodId: String, val date: LocalDate, val name: String,
    val amount: Double, val unit: QuantityUnit, val pfc: Pfc, val createdAt: Long,
)
enum class MuscleCategory(val label: String) {
    CHEST("胸"), BACK("背中"), SHOULDERS("肩"), ARMS("腕"),
    ABS("腹"), LEGS("脚"), GLUTES("尻"), OTHER("その他"),
}

data class ExercisePreset(val name: String, val category: MuscleCategory)

val exercisePresets = listOf(
    ExercisePreset("ベンチプレス", MuscleCategory.CHEST),
    ExercisePreset("ダンベルフライ", MuscleCategory.CHEST),
    ExercisePreset("懸垂", MuscleCategory.BACK),
    ExercisePreset("ラットプルダウン", MuscleCategory.BACK),
    ExercisePreset("デッドリフト", MuscleCategory.BACK),
    ExercisePreset("ショルダープレス", MuscleCategory.SHOULDERS),
    ExercisePreset("サイドレイズ", MuscleCategory.SHOULDERS),
    ExercisePreset("アームカール", MuscleCategory.ARMS),
    ExercisePreset("トライセプスプレスダウン", MuscleCategory.ARMS),
    ExercisePreset("クランチ", MuscleCategory.ABS),
    ExercisePreset("プランク", MuscleCategory.ABS),
    ExercisePreset("スクワット", MuscleCategory.LEGS),
    ExercisePreset("レッグプレス", MuscleCategory.LEGS),
    ExercisePreset("ヒップスラスト", MuscleCategory.GLUTES),
    ExercisePreset("ヒップアブダクション", MuscleCategory.GLUTES),
)

data class TrainingSet(val weightKg: Double, val reps: Int) {
    init { require(weightKg.isFinite() && weightKg >= 0); require(reps > 0) }
}

data class WorkoutEntry(
    val id: String, val date: LocalDate, val exercise: String, val category: MuscleCategory,
    val sets: List<TrainingSet>, val createdAt: Long,
) {
    init { require(exercise.isNotBlank()); require(sets.isNotEmpty()) }
    val volume: Double get() = sets.sumOf { it.weightKg * it.reps }
}

data class LogData(
    val foods: List<FoodMaster> = emptyList(),
    val meals: List<MealEntry> = emptyList(),
    val workouts: List<WorkoutEntry> = emptyList(),
)

object Nutrition {
    fun calculate(mode: NutritionMode, pfc: Pfc, amount: Double, reference: Double): Pfc {
        require(amount.isFinite() && amount > 0)
        require(reference.isFinite() && reference > 0)
        return if (mode == NutritionMode.DIRECT) pfc else pfc * (amount / reference)
    }
    fun total(meals: List<MealEntry>): Pfc = meals.fold(Pfc.Zero) { total, meal -> total + meal.pfc }
}

// Round only for display; keep the unrounded snapshot in the database.
fun Double.display(): String {
    val rounded = round(this * 100).toLong()
    val fraction = (rounded % 100).toString().padStart(2, '0').trimEnd('0')
    return (rounded / 100).toString() + if (fraction.isEmpty()) "" else ".$fraction"
}
