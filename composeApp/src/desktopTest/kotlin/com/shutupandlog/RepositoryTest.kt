package com.shutupandlog

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.shutupandlog.data.SqlLogRepository
import com.shutupandlog.db.LogDatabase
import com.shutupandlog.domain.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import kotlin.test.*

class RepositoryTest {
    private val day = LocalDate(2026, 9, 27)
    private val rice = FoodInput(name = "白米", amount = "180", mode = NutritionMode.PER_REFERENCE,
        protein = "2.5", fat = "0.3", carbs = "37.1")

    @Test fun snapshotSurvivesMasterUpdateAndDeletionKeepsHistory() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            LogDatabase.Schema.create(driver)
            val repo = SqlLogRepository(LogDatabase(driver), StandardTestDispatcher(testScheduler))
            repo.load()
            repo.save(rice.validate(), day)
            val original = repo.data.value.meals.single()
            val master = repo.data.value.foods.single()
            repo.save(FoodInput.from(master).copy(protein = "99").validate(), LocalDate(2026, 9, 28))
            assertEquals(1, repo.data.value.foods.size)
            assertEquals(99.0, repo.data.value.foods.single().pfc.protein)
            assertEquals(original, repo.data.value.meals.single { it.id == original.id })
            assertEquals(2, repo.data.value.meals.map { it.id }.distinct().size)
            repo.deleteMeal(original.id)
            assertEquals(1, repo.data.value.meals.size)
            assertEquals(1, repo.data.value.foods.size)
        } finally { driver.close() }
    }
    @Test fun failedMealInsertRollsBackFoodUpdate() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            LogDatabase.Schema.create(driver)
            val db = LogDatabase(driver)
            val repo = SqlLogRepository(db, StandardTestDispatcher(testScheduler), newId = { "fixed" })
            repo.save(rice.validate(), day)
            assertFailsWith<Exception> { repo.save(rice.copy(protein = "100").validate(), day) }
            repo.load()
            assertEquals(2.5, repo.data.value.foods.single().pfc.protein)
            assertEquals(1, repo.data.value.meals.size)
        } finally { driver.close() }
    }
    @Test fun recordsSurviveClosingAndReopeningDatabase() = runTest {
        val path = Files.createTempFile("food-log", ".db")
        val url = "jdbc:sqlite:$path"
        try {
            val driver = JdbcSqliteDriver(url)
            LogDatabase.Schema.create(driver)
            val repo = SqlLogRepository(LogDatabase(driver), StandardTestDispatcher(testScheduler))
            repo.save(rice.validate(), day)
            val expected = repo.data.value
            driver.close()
            val reopened = JdbcSqliteDriver(url)
            try {
                val next = SqlLogRepository(LogDatabase(reopened), StandardTestDispatcher(testScheduler))
                next.load()
                assertEquals(expected, next.data.value)
            } finally { reopened.close() }
        } finally { Files.deleteIfExists(path) }
    }
}
