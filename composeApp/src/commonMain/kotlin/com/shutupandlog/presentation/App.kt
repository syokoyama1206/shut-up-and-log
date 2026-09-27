package com.shutupandlog.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shutupandlog.domain.*
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(repository: LogRepository) {
    val model = viewModel { LogViewModel(repository) }
    val state by model.state.collectAsState()
    var datePicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MealEntry?>(null) }
    var deleteWorkoutTarget by remember { mutableStateOf<WorkoutEntry?>(null) }
    var section by remember { mutableStateOf(0) }
    val colors = lightColorScheme(primary = Color(0xFF246B4B), secondary = Color(0xFF536653),
        background = Color(0xFFF7F8F2), surface = Color(0xFFF7F8F2))
    MaterialTheme(colorScheme = colors) {
        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text("SHUT UP AND LOG", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("痩せたければ記録しろ。", style = MaterialTheme.typography.bodyMedium, color = colors.secondary)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(section == 0, { section = 0 }, label = { Text("食事・PFC") })
                        FilterChip(section == 1, { section = 1 }, label = { Text("トレーニング") })
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { model.shift(-1) }, enabled = !state.busy) { Text("前日") }
                        TextButton(onClick = { datePicker = true }, enabled = !state.busy) { Text(state.date.toString()) }
                        TextButton(onClick = { model.shift(1) }, enabled = !state.busy) { Text("翌日") }
                        TextButton(onClick = model::today, enabled = !state.busy) { Text("今日") }
                    }
                    if (section == 0) Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1日の合計", style = MaterialTheme.typography.labelLarge)
                            Text("${state.total.calories.display()} kcal", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            PfcRow(state.total)
                        }
                    }
                }
                item {
                    if (section == 0) {
                    Text("食べたものを記録", style = MaterialTheme.typography.titleLarge)
                    if (!state.loaded && !state.busy) {
                        Text("データの読み込みが必要です")
                        TextButton(onClick = model::reload) { Text("再読み込み") }
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { Text(it, color = colors.error, modifier = Modifier.padding(vertical = 8.dp)) }
                    state.notice?.let { Text(it, color = colors.primary, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                }
                item {
                    if (section == 0) {
                    OutlinedTextField(state.input.name, model::name, label = { Text("品目名") },
                        placeholder = { Text("例：鶏むね肉") }, singleLine = true,
                        enabled = state.loaded && !state.busy, modifier = Modifier.fillMaxWidth())
                    if (state.showSuggestions && state.suggestions.isNotEmpty()) {
                        Text("履歴から選ぶ", style = MaterialTheme.typography.labelMedium)
                        state.suggestions.forEach { food ->
                            TextButton(onClick = { model.select(food) }, enabled = !state.busy) {
                                Text("${food.name} · ${food.referenceAmount.display()}${food.unit.label} · ${if (food.mode == NutritionMode.DIRECT) "直接入力" else "基準量"}")
                            }
                        }
                    }
                    if (state.input.foodId != null) Text("履歴を使用中。PFCの変更は次回の候補にも保存されます。", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    if (section == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(state.input.mode == NutritionMode.DIRECT,
                            { model.edit { it.copy(mode = NutritionMode.DIRECT) } }, label = { Text("今回のPFC") }, enabled = !state.busy)
                        FilterChip(state.input.mode == NutritionMode.PER_REFERENCE,
                            { model.edit { it.copy(mode = NutritionMode.PER_REFERENCE) } }, label = { Text("基準量あたり") }, enabled = !state.busy)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityUnit.entries.forEach { unit ->
                            FilterChip(state.input.unit == unit, { model.edit { it.copy(unit = unit) } },
                                label = { Text(unit.label) }, enabled = !state.busy)
                        }
                    }
                    NumberInput("摂取量（${state.input.unit.label}）", state.input.amount, !state.busy) { value -> model.edit { it.copy(amount = value) } }
                    if (state.input.mode == NutritionMode.PER_REFERENCE) {
                        Spacer(Modifier.height(8.dp))
                        NumberInput("基準量（${state.input.unit.label}）", state.input.reference, !state.busy) { value -> model.edit { it.copy(reference = value) } }
                    }
                    Text(if (state.input.mode == NutritionMode.DIRECT) "今回食べた量のPFCを入力。量を変更してもPFCは変わりません。" else "基準量に含まれるPFCを入力。摂取量から自動計算します。",
                        modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInput("P（g）", state.input.protein, !state.busy, Modifier.weight(1f)) { value -> model.edit { it.copy(protein = value) } }
                        NumberInput("F（g）", state.input.fat, !state.busy, Modifier.weight(1f)) { value -> model.edit { it.copy(fat = value) } }
                        NumberInput("C（g）", state.input.carbs, !state.busy, Modifier.weight(1f)) { value -> model.edit { it.copy(carbs = value) } }
                    }
                    }
                }
                item {
                    if (section == 0) {
                    state.preview?.let { pfc ->
                        Text("今回の記録：${pfc.calories.display()} kcal", fontWeight = FontWeight.Bold)
                        PfcRow(pfc)
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = model::save, enabled = state.loaded && !state.busy, modifier = Modifier.weight(1f)) { Text("記録する") }
                        TextButton(onClick = model::reset, enabled = !state.busy) { Text("クリア") }
                    }
                    }
                }
                item {
                    if (section == 0) {
                    HorizontalDivider()
                    Text("この日の食事 · ${state.meals.size}件", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                    if (state.meals.isEmpty()) Text("まだ記録がありません。最初の食事を追加しましょう。", modifier = Modifier.padding(top = 8.dp))
                    }
                }
                items(if (section == 0) state.meals else emptyList(), key = { it.id }) { meal ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(meal.name, style = MaterialTheme.typography.titleMedium)
                            Text("${meal.amount.display()}${meal.unit.label} · ${meal.pfc.calories.display()} kcal")
                            PfcRow(meal.pfc)
                            TextButton(onClick = { deleteTarget = meal }, enabled = !state.busy) { Text("削除", color = colors.error) }
                        }
                    }
                }
                item { if (section == 0) Text("推定カロリーは P×4 + F×9 + C×4 で計算します。", style = MaterialTheme.typography.bodySmall) }
                if (section == 1) {
                    item {
                        Text("トレーニングを記録", style = MaterialTheme.typography.titleLarge)
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        state.error?.let { Text(it, color = colors.error) }
                        state.notice?.let { Text(it, color = colors.primary) }
                        Text("部位", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MuscleCategory.entries.take(4).forEach { category ->
                                FilterChip(state.workoutInput.category == category, { model.selectCategory(category) }, label = { Text(category.label) })
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MuscleCategory.entries.drop(4).forEach { category ->
                                FilterChip(state.workoutInput.category == category, { model.selectCategory(category) }, label = { Text(category.label) })
                            }
                        }
                        Text("種目", style = MaterialTheme.typography.labelLarge)
                        val choices = exercisePresets.filter { it.category == state.workoutInput.category }
                        if (choices.isEmpty()) {
                            OutlinedTextField(state.workoutInput.exercise, { value -> model.editWorkout { it.copy(exercise = value) } },
                                label = { Text("種目名") }, modifier = Modifier.fillMaxWidth())
                        } else choices.forEach { preset ->
                            FilterChip(state.workoutInput.exercise == preset.name,
                                { model.editWorkout { it.copy(exercise = preset.name) } }, label = { Text(preset.name) })
                        }
                        state.previousWorkout?.let { previous ->
                            TextButton(onClick = model::reusePrevious) {
                                Text("前回（${previous.date}）の${previous.sets.size}セットを使う")
                            }
                        }
                    }
                    items(state.workoutInput.sets.size) { index ->
                        val set = state.workoutInput.sets[index]
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${index + 1}", modifier = Modifier.padding(top = 18.dp))
                            NumberInput("重量 (kg)", set.weight, !state.busy, Modifier.weight(1f)) { value ->
                                model.editWorkout { input -> input.copy(sets = input.sets.toMutableList().also { it[index] = set.copy(weight = value) }) }
                            }
                            NumberInput("回数", set.reps, !state.busy, Modifier.weight(1f)) { value ->
                                model.editWorkout { input -> input.copy(sets = input.sets.toMutableList().also { it[index] = set.copy(reps = value) }) }
                            }
                            if (state.workoutInput.sets.size > 1) TextButton(onClick = {
                                model.editWorkout { input -> input.copy(sets = input.sets.filterIndexed { i, _ -> i != index }) }
                            }) { Text("削除") }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { model.editWorkout { it.copy(sets = it.sets + TrainingSetInput()) } }) { Text("セット追加") }
                            Button(onClick = model::saveWorkout, enabled = state.loaded && !state.busy) { Text("記録する") }
                        }
                        HorizontalDivider(Modifier.padding(top = 16.dp))
                        Text("この日のトレーニング · ${state.workouts.size}種目", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                        if (state.workouts.isEmpty()) Text("まだ記録がありません。")
                    }
                    items(state.workouts, key = { it.id }) { workout ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${workout.category.label} · ${workout.exercise}", style = MaterialTheme.typography.titleMedium)
                                workout.sets.forEachIndexed { index, set -> Text("${index + 1}. ${set.weightKg.display()} kg × ${set.reps} reps") }
                                Text("ボリューム ${workout.volume.display()} kg")
                                TextButton(onClick = { deleteWorkoutTarget = workout }) { Text("削除", color = colors.error) }
                            }
                        }
                    }
                }
            }
        }
        if (datePicker) {
            val picker = rememberDatePickerState(initialSelectedDateMillis = state.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
            DatePickerDialog(onDismissRequest = { datePicker = false }, confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { model.date(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date) }
                    datePicker = false
                }) { Text("選択") }
            }, dismissButton = { TextButton(onClick = { datePicker = false }) { Text("キャンセル") } }) { DatePicker(picker) }
        }
        deleteTarget?.let { meal ->
            AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("記録を削除しますか？") },
                text = { Text("${meal.name}の食事記録を削除します。食品履歴は残ります。") },
                confirmButton = { TextButton(onClick = { model.delete(meal.id); deleteTarget = null }) { Text("削除") } },
                dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("キャンセル") } })
        }
        deleteWorkoutTarget?.let { workout ->
            AlertDialog(onDismissRequest = { deleteWorkoutTarget = null }, title = { Text("記録を削除しますか？") },
                text = { Text("${workout.exercise}のトレーニング記録を削除します。") },
                confirmButton = { TextButton(onClick = { model.deleteWorkout(workout.id); deleteWorkoutTarget = null }) { Text("削除") } },
                dismissButton = { TextButton(onClick = { deleteWorkoutTarget = null }) { Text("キャンセル") } })
        }
    }
}

@Composable
private fun PfcRow(pfc: Pfc) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("P  ${pfc.protein.display()}g")
        Text("F  ${pfc.fat.display()}g")
        Text("C  ${pfc.carbs.display()}g")
    }
}
@Composable
private fun NumberInput(label: String, value: String, enabled: Boolean, modifier: Modifier = Modifier.fillMaxWidth(), change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier)
}
