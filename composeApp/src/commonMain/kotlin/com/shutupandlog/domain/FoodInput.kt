package com.shutupandlog.domain

data class FoodInput(
    val foodId: String? = null,
    val name: String = "",
    val amount: String = "",
    val unit: QuantityUnit = QuantityUnit.GRAM,
    val mode: NutritionMode = NutritionMode.DIRECT,
    val reference: String = "100",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
) {
    fun validate(): ValidFood {
        require(name.trim().isNotEmpty()) { "品目名を入力してください" }
        require(name.trim().length <= 100) { "品目名は100文字以内にしてください" }
        val quantity = number(amount, "摂取量", positive = true)
        val base = if (mode == NutritionMode.DIRECT) quantity else number(reference, "基準量", positive = true)
        val pfc = Pfc(number(protein, "P"), number(fat, "F"), number(carbs, "C"))
        val actual = Nutrition.calculate(mode, pfc, quantity, base)
        require(listOf(actual.protein, actual.fat, actual.carbs).all { it <= 1_000_000 }) { "計算結果が大きすぎます。量と基準量を確認してください" }
        return ValidFood(foodId, name.trim(), quantity, unit, mode, base, pfc, actual)
    }
    companion object {
        fun from(food: FoodMaster) = FoodInput(
            food.id, food.name, food.referenceAmount.toString(), food.unit, food.mode,
            food.referenceAmount.toString(), food.pfc.protein.toString(), food.pfc.fat.toString(), food.pfc.carbs.toString(),
        )
        private fun number(text: String, label: String, positive: Boolean = false): Double {
            val cleaned = text.trim().map { if (it in '０'..'９') '0' + (it - '０') else if (it == '．') '.' else it }.joinToString("")
            require(Regex("[0-9]+(?:\\.[0-9]+)?").matches(cleaned)) { "$label は数値で入力してください（例: 2.5）" }
            val value = cleaned.toDoubleOrNull()
            require(value != null && value.isFinite() && value <= 1_000_000 && if (positive) value > 0 else value >= 0) {
                "$label は${if (positive) "0より大きく" else "0以上で"}、1,000,000以下にしてください"
            }
            return value
        }
    }
}
data class ValidFood(
    val foodId: String?, val name: String, val amount: Double, val unit: QuantityUnit,
    val mode: NutritionMode, val reference: Double, val sourcePfc: Pfc, val actualPfc: Pfc,
)
