package com.shutupandlog.data

import com.shutupandlog.db.LogDatabase
import com.shutupandlog.domain.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SqlLogRepository(
    private val database: LogDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val newId: () -> String = { Uuid.random().toString() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : LogRepository {
    private val state = MutableStateFlow(LogData())
    override val data = state.asStateFlow()
    private val mutex = Mutex()
    private val queries get() = database.logQueries

    override suspend fun load() = withContext(dispatcher) { mutex.withLock { refresh() } }
    override suspend fun save(input: ValidFood, date: LocalDate) = withContext(dispatcher) {
        mutex.withLock {
            val id = input.foodId ?: newId()
            val timestamp = now()
            database.transaction {
                queries.upsertFood(id, input.name, input.unit.name, input.mode.name, input.reference,
                    input.sourcePfc.protein, input.sourcePfc.fat, input.sourcePfc.carbs, timestamp)
                queries.insertMeal(newId(), id, date.toString(), input.name, input.amount, input.unit.name,
                    input.actualPfc.protein, input.actualPfc.fat, input.actualPfc.carbs, timestamp)
            }
            refresh()
        }
    }
    override suspend fun deleteMeal(id: String) = withContext(dispatcher) {
        mutex.withLock { queries.deleteMeal(id); refresh() }
    }
    private fun refresh() {
        state.value = LogData(
            queries.allFoods().executeAsList().map {
                FoodMaster(it.id, it.name, QuantityUnit.valueOf(it.unit), NutritionMode.valueOf(it.mode),
                    it.reference_amount, Pfc(it.protein, it.fat, it.carbs), it.updated_at)
            },
            queries.allMeals().executeAsList().map {
                MealEntry(it.id, it.food_id, LocalDate.parse(it.date), it.name, it.amount,
                    QuantityUnit.valueOf(it.unit), Pfc(it.protein, it.fat, it.carbs), it.created_at)
            },
        )
    }
}
