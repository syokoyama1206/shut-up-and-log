package com.shutupandlog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutupandlog.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class LogUiState(
    val date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val input: FoodInput = FoodInput(),
    val data: LogData = LogData(),
    val busy: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val showSuggestions: Boolean = false,
) {
    val meals get() = data.meals.filter { it.date == date }
    val total get() = Nutrition.total(meals)
    val suggestions get() = suggestFoods(data.foods, input.name)
    val preview get() = runCatching { input.validate().actualPfc }.getOrNull()
}

class LogViewModel(private val repository: LogRepository) : ViewModel() {
    private val mutable = MutableStateFlow(LogUiState())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch { repository.data.collect { mutable.value = mutable.value.copy(data = it) } }
        reload()
    }
    fun reload() = operation("保存データを読み込めませんでした") {
        repository.load()
        mutable.value = mutable.value.copy(loaded = true)
    }
    fun edit(change: (FoodInput) -> FoodInput) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(input = change(mutable.value.input), error = null, notice = null)
    }
    fun name(value: String) {
        edit { it.copy(name = value, foodId = null) }
        mutable.value = mutable.value.copy(showSuggestions = true)
    }
    fun select(food: FoodMaster) {
        edit { FoodInput.from(food) }
        mutable.value = mutable.value.copy(showSuggestions = false)
    }
    fun reset() { edit { FoodInput() }; mutable.value = mutable.value.copy(showSuggestions = false) }
    fun date(date: LocalDate) { mutable.value = mutable.value.copy(date = date, notice = null) }
    fun shift(days: Int) = date(mutable.value.date.plus(days, DateTimeUnit.DAY))
    fun today() = date(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    fun save() {
        if (mutable.value.busy || !mutable.value.loaded) return
        val input = try { mutable.value.input.validate() } catch (e: IllegalArgumentException) {
            mutable.value = mutable.value.copy(error = e.message); return
        }
        val selectedDate = mutable.value.date
        operation("保存できませんでした。入力は保持されています") {
            repository.save(input, selectedDate)
            mutable.value = mutable.value.copy(input = FoodInput(), showSuggestions = false, notice = "${input.name}を記録しました")
        }
    }
    fun delete(id: String) = operation("削除できませんでした") { repository.deleteMeal(id) }
    private fun operation(failure: String, block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = failure) }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }
}
