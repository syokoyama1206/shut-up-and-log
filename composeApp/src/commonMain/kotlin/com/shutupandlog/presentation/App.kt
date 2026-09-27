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
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { model.shift(-1) }, enabled = !state.busy) { Text("前日") }
                        TextButton(onClick = { datePicker = true }, enabled = !state.busy) { Text(state.date.toString()) }
                        TextButton(onClick = { model.shift(1) }, enabled = !state.busy) { Text("翌日") }
                        TextButton(onClick = model::today, enabled = !state.busy) { Text("今日") }
                    }
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1日の合計", style = MaterialTheme.typography.labelLarge)
                            Text("${state.total.calories.display()} kcal", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            PfcRow(state.total)
                        }
                    }
                }
                item {
                    Text("食べたものを記録", style = MaterialTheme.typography.titleLarge)
                    if (!state.loaded && !state.busy) {
                        Text("データの読み込みが必要です")
                        TextButton(onClick = model::reload) { Text("再読み込み") }
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { Text(it, color = colors.error, modifier = Modifier.padding(vertical = 8.dp)) }
                    state.notice?.let { Text(it, color = colors.primary, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                item {
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
                item {
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
                item {
                    state.preview?.let { pfc ->
                        Text("今回の記録：${pfc.calories.display()} kcal", fontWeight = FontWeight.Bold)
                        PfcRow(pfc)
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = model::save, enabled = state.loaded && !state.busy, modifier = Modifier.weight(1f)) { Text("記録する") }
                        TextButton(onClick = model::reset, enabled = !state.busy) { Text("クリア") }
                    }
                }
                item {
                    HorizontalDivider()
                    Text("この日の食事 · ${state.meals.size}件", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                    if (state.meals.isEmpty()) Text("まだ記録がありません。最初の食事を追加しましょう。", modifier = Modifier.padding(top = 8.dp))
                }
                items(state.meals, key = { it.id }) { meal ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(meal.name, style = MaterialTheme.typography.titleMedium)
                            Text("${meal.amount.display()}${meal.unit.label} · ${meal.pfc.calories.display()} kcal")
                            PfcRow(meal.pfc)
                            TextButton(onClick = { deleteTarget = meal }, enabled = !state.busy) { Text("削除", color = colors.error) }
                        }
                    }
                }
                item { Text("推定カロリーは P×4 + F×9 + C×4 で計算します。", style = MaterialTheme.typography.bodySmall) }
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
