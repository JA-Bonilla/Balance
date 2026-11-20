package com.jabonilla.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val muted = Color.White.copy(alpha = 0.7f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(store: Store, onSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Entry?>(null) }
    var logging by remember { mutableStateOf(false) }
    var timerSide by remember { mutableStateOf<Side?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val data = store.data
    val (weekStart, weekEnd) = AppData.thisWeek(Instant.now(), ZoneId.systemDefault(), Locale.getDefault())
    val growth = data.minutes(Side.GROWTH, weekStart, weekEnd)
    val fun_ = data.minutes(Side.FUN, weekStart, weekEnd)

    suspend fun syncHealth() {
        if (!store.data.healthEnabled || !HealthSync.isAvailable(context)) return
        try {
            val import = HealthSync.fetch(HealthConnectClient.getOrCreate(context))
            store.replaceHealthEntries(import.entries, import.start, import.end)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            store.error = "Couldn't read Health: ${e.message}"
        }
    }

    LaunchedEffect(Unit) { syncHealth() }
    // Log the timer the moment it ends while the app is open.
    LaunchedEffect(data.activeTimer) {
        val t = data.activeTimer ?: return@LaunchedEffect
        delay(Duration.between(Instant.now(), t.end).toMillis().coerceAtLeast(0) + 250)
        TimerAlarm.finishIfDue(context, store)
    }

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        PullToRefreshBox(refreshing, { scope.launch { refreshing = true; syncHealth(); refreshing = false } }, Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SkyBackdrop(growth, fun_, data.bonsiStage) }
                item { BalanceCaption(growth, fun_, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 0.dp)) }

                item {
                    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val t = data.activeTimer
                        if (t != null) RunningTimerRow(t, store.data) { store.stopTimer(); TimerAlarm.cancel(context) }
                        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TimerButton(Side.FUN) { timerSide = Side.FUN }
                            TimerButton(Side.GROWTH) { timerSide = Side.GROWTH }
                        }
                    }
                }

                item { Header("Goals") }
                items(data.goals, key = { it.id }) { goal ->
                    val c = data.category(goal.categoryID)
                    val done = data.minutes(goal.categoryID, weekStart, weekEnd)
                    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(symbolIcon(c?.symbol ?: "circle")), null, Modifier.size(22.dp))
                            Text(c?.name ?: goal.categoryID, Modifier.weight(1f), fontSize = 17.sp)
                            Text("${done.minutesShort} / ${goal.weeklyMinutes.minutesShort}", color = muted)
                        }
                        LinearProgressIndicator({ minOf(done, goal.weeklyMinutes).toFloat() / goal.weeklyMinutes }, Modifier.fillMaxWidth(), color = GrowthColor)
                    }
                }

                item { Header("Recent") }
                val recent = data.entries.sortedByDescending { it.date }.take(20)
                if (recent.isEmpty()) item { Text("Nothing logged yet. Tap + to log minutes.", Modifier.padding(horizontal = 20.dp), color = muted) }
                items(recent, key = { it.id }) { entry ->
                    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                        SwipeRow({ store.delete(entry) }) { EntryRow(entry, data) { if (entry.source == Source.MANUAL) editing = entry } }
                    }
                }
            }
        }

        // Fixed over the sky while the list scrolls, like the iOS toolbar.
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ToolbarIcon(R.drawable.ic_settings, "Settings", onSettings)
            ToolbarIcon(R.drawable.ic_add, "Log minutes") { logging = true }
        }
    }

    if (logging) LogSheet(null, store) { logging = false }
    editing?.let { e -> LogSheet(e, store) { editing = null } }
    timerSide?.let { s -> TimerSheet(s, store) { timerSide = null } }
}

@Composable
private fun Header(text: String) = Text(text, Modifier.padding(horizontal = 20.dp), fontSize = 13.sp, color = muted)

@Composable
private fun EntryRow(entry: Entry, data: AppData, onClick: () -> Unit) {
    val c = data.category(entry.categoryID)
    val local = entry.date.atZone(ZoneId.systemDefault())
    Row(
        Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(symbolIcon(c?.symbol ?: "circle")), null, Modifier.size(24.dp), tint = sideColor(entry.side))
        Column(Modifier.weight(1f)) {
            Text(c?.name ?: entry.categoryID, fontSize = 17.sp)
            Text("${DateTimeFormatter.ofPattern("EEE").format(local)} ${DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local)}", fontSize = 12.sp, color = muted)
        }
        if (entry.source == Source.HEALTH) Icon(painterResource(R.drawable.ic_favorite), "From Health", Modifier.size(14.dp), tint = muted)
        Text(entry.minutes.minutesText)
    }
}

@Composable
private fun TimerButton(side: Side, onClick: () -> Unit) {
    OutlinedButton(onClick, shape = CircleShape) {
        Icon(painterResource(R.drawable.ic_timer), null, Modifier.size(18.dp), tint = sideColor(side))
        Text("  ${side.label} Timer", color = sideColor(side), fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
private fun RunningTimerRow(timer: ActiveTimer, data: AppData, stop: () -> Unit) {
    val c = data.category(timer.categoryID)
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(timer) { while (true) { now = Instant.now(); delay(1000) } }
    val left = Duration.between(now, timer.end).seconds.coerceAtLeast(0)
    val clock = if (left >= 3600) "%d:%02d:%02d".format(left / 3600, left / 60 % 60, left % 60) else "%d:%02d".format(left / 60, left % 60)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.ic_timer), null, tint = sideColor(timer.side))
        Column(Modifier.weight(1f)) {
            Text(c?.name ?: timer.categoryID, fontSize = 17.sp)
            Text("${timer.minutes.minutesText} timer", fontSize = 12.sp, color = muted)
        }
        Text(clock, style = TextStyle(fontSize = 20.sp, fontFeatureSettings = "tnum"))
        OutlinedButton(stop, Modifier.semantics { contentDescription = "Stop timer and log time so far" }) { Text("Stop") }
    }
}

/** Toolbar button over the sky: white glyph on a translucent black disc. */
@Composable
private fun ToolbarIcon(icon: Int, label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.22f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = Color.White)
        }
    }
}
