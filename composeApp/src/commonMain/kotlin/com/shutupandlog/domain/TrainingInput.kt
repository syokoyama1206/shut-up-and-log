package com.shutupandlog.domain

data class TrainingSetInput(val weight: String = "", val reps: String = "") {
    fun validate(): TrainingSet {
        val weightValue = weight.normalizeNumber().toDoubleOrNull()
            ?: throw IllegalArgumentException("重量を数値で入力してください")
        val repsValue = reps.normalizeNumber().toIntOrNull()
            ?: throw IllegalArgumentException("回数を整数で入力してください")
        require(weightValue.isFinite() && weightValue >= 0 && weightValue <= 10_000) { "重量は0〜10000kgで入力してください" }
        require(repsValue in 1..10_000) { "回数は1〜10000回で入力してください" }
        return TrainingSet(weightValue, repsValue)
    }
}

data class WorkoutInput(
    val category: MuscleCategory = MuscleCategory.CHEST,
    val exercise: String = exercisePresets.first().name,
    val sets: List<TrainingSetInput> = listOf(TrainingSetInput()),
) {
    fun validate(): ValidWorkout {
        val cleanName = exercise.trim()
        require(cleanName.isNotEmpty()) { "種目を選んでください" }
        require(sets.isNotEmpty()) { "1セット以上追加してください" }
        return ValidWorkout(cleanName, category, sets.map { it.validate() })
    }
}

data class ValidWorkout(val exercise: String, val category: MuscleCategory, val sets: List<TrainingSet>)

private fun String.normalizeNumber(): String = trim().map {
    when (it) {
        in '０'..'９' -> ('0'.code + (it.code - '０'.code)).toChar()
        '．' -> '.'
        else -> it
    }
}.joinToString("")
