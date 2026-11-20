package com.jabonilla.balance

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController

private val muted = Color.White.copy(alpha = 0.7f)

/** Swipe end-to-start to delete (red background, trash icon). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRow(onDelete: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
    })
    SwipeToDismissBox(
        state, enableDismissFromStartToEnd = false, enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(Color.Red).padding(end = 16.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(painterResource(R.drawable.ic_delete), "Delete", tint = Color.White)
            }
        },
    ) { content() }
}

@Composable
private fun Section(title: String?, footer: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title != null) Text(title, fontSize = 13.sp, color = muted, modifier = Modifier.padding(horizontal = 4.dp))
        Column(Modifier.fillMaxWidth().background(Surface, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))) { content() }
        if (footer != null) Text(footer, fontSize = 13.sp, color = muted, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun SettingsScreen(store: Store, onBack: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var addingActivity by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(TimerSound.current(context)) }
    val healthAvailable = remember { HealthSync.isAvailable(context) }
    val data = store.data

    val healthPermission = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (HealthSync.exercisePermission in granted) store.setHealthEnabled(true)
        else store.error = "Couldn't get Health Connect access."
    }

    Column(Modifier.fillMaxSize().background(AppBackground).safeDrawingPadding()) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") }
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Section("Goals") {
                data.goals.forEach { goal ->
                    androidx.compose.runtime.key(goal.id) {
                        val c = data.category(goal.categoryID)
                        // Keep at least one goal; deleting the last would drop the user back into onboarding.
                        SwipeRow({ if (data.goals.size > 1) store.delete(goal) }, enabled = data.goals.size > 1) {
                            Row(
                                Modifier.fillMaxWidth().background(Surface).clickable { editingGoal = goal }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(painterResource(symbolIcon(c?.symbol ?: "circle")), null, Modifier.size(24.dp))
                                Text(c?.name ?: goal.categoryID, Modifier.weight(1f), fontSize = 17.sp)
                                Text("${goal.minutesPerDay} min × ${goal.daysPerWeek}d", color = muted)
                            }
                        }
                    }
                }
                TextButton({
                    val used = data.goals.map { it.categoryID }.toSet()
                    editingGoal = Goal(categoryID = data.categories(Side.GROWTH).firstOrNull { it.id !in used }?.id ?: "reading", minutesPerDay = 15, daysPerWeek = 5)
                }) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
                    Text("  Add goal")
                }
            }

            Section("Custom activities", "Removing an activity hides it from the lists; time you already logged keeps it.") {
                data.customCategories.filter { !it.archived }.forEach { c ->
                    androidx.compose.runtime.key(c.id) {
                        SwipeRow({ store.archive(c) }) {
                            Row(Modifier.fillMaxWidth().background(Surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(painterResource(symbolIcon(c.symbol)), null, Modifier.size(24.dp), tint = sideColor(c.side))
                                Text(c.name, Modifier.weight(1f), fontSize = 17.sp)
                                Text(c.side.label, color = muted)
                            }
                        }
                    }
                }
                TextButton({ addingActivity = true }) {
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
                    Text("  Add activity")
                }
            }

            Section("Timer") {
                DropdownField("Alarm sound", sound, TimerSound.entries, { it.label }, {
                    sound = it
                    context.prefs().edit().putString(TimerSound.KEY, it.id).apply()
                    it.preview(context)
                }, Modifier.fillMaxWidth().padding(8.dp))
            }

            Section(
                "Data sources",
                "Exercise sessions count as Exercise and mindfulness sessions as Meditation." +
                    if (healthAvailable) "" else " Install or update Health Connect to use this.",
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Count Health Connect exercise & mindfulness", Modifier.weight(1f), fontSize = 17.sp)
                    Switch(
                        checked = data.healthEnabled, enabled = healthAvailable,
                        onCheckedChange = { on ->
                            if (!on) store.setHealthEnabled(false)
                            else healthPermission.launch(HealthSync.permissions(HealthConnectClient.getOrCreate(context)))
                        },
                    )
                }
            }

            Text(
                "Your data is a single file, balance.json, in Android/data/com.jabonilla.balance/files on this phone. Nothing leaves your phone.",
                fontSize = 13.sp, color = muted,
            )
        }
    }

    editingGoal?.let { GoalSheet(it, store) { editingGoal = null } }
    if (addingActivity) ActivitySheet(store) { addingActivity = false }
}

@Composable
fun GoalSheet(goal: Goal, store: Store, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(goal) }
    SheetScaffold("Goal", sideColor(Side.GROWTH), onDismiss, onSave = { store.upsert(draft); onDismiss() }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ActivityPicker(Side.GROWTH, draft.categoryID, store.data) { draft = draft.copy(categoryID = it) }
            TargetFields(draft.minutesPerDay, draft.daysPerWeek, { draft = draft.copy(minutesPerDay = it) }, { draft = draft.copy(daysPerWeek = it) })
        }
    }
}

/** Create a custom growth or fun activity with a name and icon. */
@Composable
fun ActivitySheet(store: Store, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var side by remember { mutableStateOf(Side.GROWTH) }
    var symbol by remember { mutableStateOf(Category.customSymbols[0]) }
    val trimmed = name.trim()

    SheetScaffold(
        "New Activity", sideColor(side), onDismiss, saveLabel = "Add", saveEnabled = trimmed.isNotEmpty(),
        onSave = { store.add(Category("custom-${newId()}", trimmed, symbol, side)); onDismiss() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SideSegments(side) { side = it }
            OutlinedTextField(name, { name = it.take(30) }, Modifier.fillMaxWidth(), label = { Text("Name, e.g. Gardening") }, singleLine = true)
            Text("Icon", fontSize = 13.sp, color = muted)
            Column {
                Category.customSymbols.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { s ->
                            val selected = symbol == s
                            Box(
                                Modifier.size(48.dp).background(if (selected) sideColor(side) else Color.Transparent, CircleShape)
                                    .selectable(selected, role = Role.RadioButton) { symbol = s }
                                    .semantics { contentDescription = symbolLabel(s) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(painterResource(symbolIcon(s)), null, Modifier.size(24.dp), tint = if (selected) Color.White else sideColor(side))
                            }
                        }
                    }
                }
            }
        }
    }
}
