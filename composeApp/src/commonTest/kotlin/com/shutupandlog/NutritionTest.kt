package com.shutupandlog

import com.shutupandlog.domain.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class NutritionTest {
    private val rice = FoodInput(name = "白米", amount = "180", mode = NutritionMode.PER_REFERENCE,
        reference = "100", protein = "2.5", fat = "0.3", carbs = "37.1")
    @Test fun referenceScalingAndCalories() {
        val p = rice.validate().actualPfc
        assertEquals(4.5, p.protein, 0.000001)
        assertEquals(0.54, p.fat, 0.000001)
        assertEquals(66.78, p.carbs, 0.000001)
        assertEquals(289.98, p.calories, 0.000001)
    }
    @Test fun directDoesNotScaleWhenAmountChanges() {
        val input = FoodInput(name = "ねぶた漬け", amount = "35", protein = "2.03", fat = "0.18", carbs = "6.62")
        assertEquals(Pfc(2.03, 0.18, 6.62), input.validate().actualPfc)
        assertEquals(input.validate().actualPfc, input.copy(amount = "70").validate().actualPfc)
    }
    @Test fun arbitraryUnitsAndReference() {
        val result = rice.copy(unit = QuantityUnit.PIECE, reference = "1", amount = "2.5").validate()
        assertEquals(6.25, result.actualPfc.protein)
    }
    @Test fun rejectsInvalidNumbersAndBlankName() {
        listOf("", "-1", "NaN", "Infinity", "1e309", "1,2", "1000001").forEach {
            assertFailsWith<IllegalArgumentException> { rice.copy(protein = it).validate() }
        }
        assertFailsWith<IllegalArgumentException> { rice.copy(name = "  ").validate() }
        assertFailsWith<IllegalArgumentException> { rice.copy(amount = "0").validate() }
        assertFailsWith<IllegalArgumentException> { rice.copy(reference = "0").validate() }
        assertFailsWith<IllegalArgumentException> { rice.copy(reference = "0.00000001").validate() }
    }
    @Test fun zeroMacrosAndFullWidthNumbers() {
        assertEquals(2.5, rice.copy(protein = "２．５", fat = "0", carbs = "0").validate().sourcePfc.protein)
        assertEquals(Pfc.Zero, rice.copy(protein = "0", fat = "0", carbs = "0").validate().actualPfc)
    }
    @Test fun totalIsUnroundedAndEmptyIsZero() {
        assertEquals(Pfc.Zero, Nutrition.total(emptyList()))
        val entry = MealEntry("1", "f", LocalDate(2026, 9, 27), "食品", 1.0, QuantityUnit.GRAM, Pfc(0.123, 0.456, 0.789), 0)
        val total = Nutrition.total(listOf(entry, entry.copy(id = "2")))
        assertEquals(0.246, total.protein, 0.000001)
        assertEquals("0.25", total.protein.display())
    }
    @Test fun suggestionsFavorPrefixAndCopyNutrition() {
        fun food(id: String, name: String, time: Long) = FoodMaster(id, name, QuantityUnit.GRAM, NutritionMode.PER_REFERENCE, 100.0, Pfc(20.0, 1.0, 0.0), time)
        val first = food("1", "鶏むね肉", 1)
        val candidates = suggestFoods(listOf(food("2", "蒸し鶏", 2), first, food("3", "白米", 3)), "鶏")
        assertEquals(listOf("1", "2"), candidates.map { it.id })
        assertEquals(first.pfc, FoodInput.from(first).validate().sourcePfc)
        assertEquals(first.id, FoodInput.from(first).foodId)
    }
}
