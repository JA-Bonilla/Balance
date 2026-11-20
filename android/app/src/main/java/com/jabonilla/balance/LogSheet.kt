package com.jabonilla.balance

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val muted = Color.White.copy(alpha = 0.7f)

/** Bottom sheet with a Cancel / title / Save header, tinted by side. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetScaffold(
    title: String, tint: Color, onDismiss: () -> Unit, onSave: () -> Unit,
    saveLabel: String = "Save", saveEnabled: Boolean = true, content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = tint)) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppBackground,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onDismiss) { Text("Cancel") }
                    Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    TextButton(onSave, enabled = saveEnabled) { Text(saveLabel, fontWeight = FontWeight.Bold) }
                }
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String, selected: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit,
    modifier: Modifier = Modifier, icon: (T) -> Int? = { null },
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            value = text(selected), onValueChange = {}, readOnly = true, label = { Text(label) },
            leadingIcon = icon(selected)?.let { { Icon(painterResource(it), null, Modifier.size(20.dp)) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(text(o)) },
                    leadingIcon = icon(o)?.let { { Icon(painterResource(it), null, Modifier.size(20.dp)) } },
                    onClick = { onSelect(o); expanded = false },
                )
            }
        }
    }
}

@Composable
fun SideSegments(side: Side, onSide: (Side) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Side.entries.forEachIndexed { i, s ->
            SegmentedButton(
                selected = s == side, onClick = { onSide(s) }, shape = SegmentedButtonDefaults.itemShape(i, Side.entries.size),
                colors = SegmentedButtonDefaults.colors(activeContainerColor = sideColor(s), activeContentColor = Color.Black),
            ) { Text(s.label) }
        }
    }
}

/** Built-in and custom activities for one side; keeps an archived current choice visible when editing. */
@Composable
fun ActivityPicker(side: Side, selection: String, data: AppData, onSelect: (String) -> Unit) {
    val current = data.category(selection)
    val options = data.categories(side).let { if (it.none { c -> c.id == selection } && current != null) it + current else it }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = sideColor(side))) {
        DropdownField(
            "Activity", current ?: options[0], options, { it.name }, { onSelect(it.id) },
            Modifier.fillMaxWidth(), icon = { symbolIcon(it.symbol) },
        )
    }
}

val presets = listOf(15, 30, 60, 90, 120, 180, 240)

/** Preset chips (15m ... 4h) plus a 5-minute stepper; optionally a "Custom" chip the caller handles. */
@Composable
fun MinutesPicker(minutes: Int, color: Color, onMinutes: (Int) -> Unit, custom: Boolean? = null, onCustom: (Boolean) -> Unit = {}) {
    val isCustom = custom == true
    val chips = presets.map { m -> Triple(m.minutesShort, m.minutesText, !isCustom && minutes == m) to { onMinutes(m); onCustom(false) } } +
        if (custom != null) listOf(Triple("Custom", "Custom start and end time", isCustom) to { onCustom(true) }) else emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (c, click) ->
                    val (title, label, selected) = c
                    val colors = if (selected) ButtonDefaults.filledTonalButtonColors(containerColor = color, contentColor = Color.Black) else ButtonDefaults.filledTonalButtonColors()
                    FilledTonalButton(click, Modifier.weight(1f).semantics { contentDescription = label }, colors = colors, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
                        Text(title, maxLines = 1, softWrap = false)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (!isCustom) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(minutes.minutesText, Modifier.weight(1f), fontSize = 17.sp)
                IconButton({ onMinutes((minutes - 5).coerceAtLeast(1)) }, enabled = minutes > 1) { Icon(painterResource(R.drawable.ic_remove), "Decrease") }
                IconButton({ onMinutes((minutes + 5).coerceAtMost(1440)) }, enabled = minutes < 1440) { Icon(painterResource(R.drawable.ic_add), "Increase") }
            }
        }
    }
}

fun floor5(t: Instant): Instant = Instant.ofEpochSecond(t.epochSecond / 300 * 300)

/** Date and time of one moment, limited to [min, max] and to 5-minute marks. Never later than now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, value: Instant, onChange: (Instant) -> Unit, min: Instant? = null, max: Instant = Instant.now()) {
    val zone = ZoneId.systemDefault()
    val ceiling = floor5(max)
    fun apply(t: Instant) { onChange(t.coerceAtMost(ceiling).let { if (min != null && it < min) min else it }) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val local = value.atZone(zone)

    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 17.sp)
        TextButton({ pickDate = true }) { Text(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(local)) }
        TextButton({ pickTime = true }) { Text(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local)) }
    }

    if (pickDate) {
        val today = LocalDate.now(zone)
        val minDate = min?.atZone(zone)?.toLocalDate()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !d.isAfter(today) && (minDate == null || !d.isBefore(minDate))
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        apply(d.atTime(local.toLocalTime()).atZone(zone).toInstant())
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val is24 = DateFormat.is24HourFormat(LocalContext.current)
        var hour by remember { mutableIntStateOf(local.hour) }
        var minute by remember { mutableIntStateOf(local.minute / 5 * 5) }
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text(label) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val maxHour = ceiling.atZone(zone).let { if (it.toLocalDate() == local.toLocalDate()) it.hour else 23 }
                    DropdownField("Hour", hour, (0..maxHour).toList(), { h ->
                        if (is24) "%02d".format(h) else LocalTime.of(h, 0).format(DateTimeFormatter.ofPattern("h a"))
                    }, { hour = it }, Modifier.weight(1f))
                    DropdownField("Minute", minute, (0..55 step 5).toList(), { "%02d".format(it) }, { minute = it }, Modifier.weight(1f))
                }
            },
            confirmButton = {
                TextButton({
                    apply(local.toLocalDate().atTime(hour, minute).atZone(zone).toInstant())
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ pickTime = false }) { Text("Cancel") } },
        )
    }
}

/** Log or edit time. New entries default to the last-used side/category so a repeat log is 3 taps. */
@Composable
fun LogSheet(entry: Entry?, store: Store, onDismiss: () -> Unit) {
    val prefs = LocalContext.current.prefs()
    val isNew = entry == null
    val initialSide = if (prefs.getString("lastSide", "growth") == "fun") Side.FUN else Side.GROWTH
    var side by remember { mutableStateOf(entry?.side ?: initialSide) }
    var categoryID by remember { mutableStateOf(entry?.categoryID ?: prefs.getString("lastCategory", "reading")!!) }
    var minutes by remember { mutableIntStateOf(entry?.minutes ?: 30) }
    var date by remember { mutableStateOf(entry?.date ?: floor5(Instant.now())) }
    var custom by remember { mutableStateOf(false) }
    // Start on 5-minute marks so the values match what the 5-minute pickers display.
    var end by remember { mutableStateOf(floor5(Instant.now())) }
    var start by remember { mutableStateOf(floor5(Instant.now()).minusSeconds(3600)) }
    val customMinutes = AppData.roundedMinutes(start, end)

    SheetScaffold(
        title = if (isNew) "Log Time" else "Edit Entry", tint = sideColor(side), onDismiss = onDismiss,
        saveEnabled = !custom || customMinutes > 0,
        onSave = {
            val base = entry ?: Entry(side = side, categoryID = categoryID, minutes = minutes, date = date)
            store.upsert(
                base.copy(
                    side = side, categoryID = categoryID,
                    minutes = if (custom) customMinutes else minutes,
                    date = if (custom) start else date,
                ),
            )
            prefs.edit().putString("lastSide", side.name.lowercase()).putString("lastCategory", categoryID).apply()
            onDismiss()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SideSegments(side) { s ->
                side = s
                if (store.data.category(categoryID)?.side != s) categoryID = store.data.categories(s)[0].id
            }
            ActivityPicker(side, categoryID, store.data) { categoryID = it }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Time spent", fontSize = 13.sp, color = muted)
                MinutesPicker(minutes, sideColor(side), { minutes = it }, custom, { custom = it })
                if (custom) {
                    DateTimeField("Start", start, { start = it; if (end < it) end = it })
                    DateTimeField("End", end, { end = it }, min = start)
                    Row(Modifier.fillMaxWidth()) {
                        Text("Time spent", Modifier.weight(1f), fontSize = 17.sp)
                        Text(if (customMinutes > 0) customMinutes.minutesText else "—", fontSize = 17.sp)
                    }
                    Text("Rounded to the nearest 5 minutes.", fontSize = 13.sp, color = muted)
                }
            }
            if (!custom) DateTimeField("When", date, { date = it })
        }
    }
}
