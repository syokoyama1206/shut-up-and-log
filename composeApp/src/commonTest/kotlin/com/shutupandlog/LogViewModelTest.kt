package com.shutupandlog

import com.shutupandlog.domain.*
import com.shutupandlog.presentation.LogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {
    private class Fake : LogRepository {
        override val data = MutableStateFlow(LogData())
        var fail = false
        var saves = 0
        override suspend fun load() {}
        override suspend fun save(input: ValidFood, date: LocalDate) { if (fail) error("disk full"); saves++ }
        override suspend fun deleteMeal(id: String) {}
        override suspend fun saveWorkout(input: ValidWorkout, date: LocalDate) {}
        override suspend fun deleteWorkout(id: String) {}
    }
    @Test fun validationFailurePreservesInputAndDoubleTapSavesOnce() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = Fake()
            val model = LogViewModel(repository)
            runCurrent()
            model.save()
            assertNotNull(model.state.value.error)
            assertEquals(0, repository.saves)
            model.edit { FoodInput(name = "白米", amount = "100", protein = "2.5", fat = "0.3", carbs = "37.1") }
            repository.fail = true
            model.save()
            runCurrent()
            assertEquals("白米", model.state.value.input.name)
            assertNotNull(model.state.value.error)
            repository.fail = false
            model.save()
            model.save()
            runCurrent()
            assertEquals(1, repository.saves)
            assertEquals("", model.state.value.input.name)
        } finally { Dispatchers.resetMain() }
    }
}
