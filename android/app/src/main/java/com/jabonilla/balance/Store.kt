package com.jabonilla.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/** Plain JVM code (no Android imports) so unit tests run without a device. All calls run on the main thread. */
class Store(val file: File) {
    var data by mutableStateOf(AppData())
    var error by mutableStateOf<String?>(null)

    // Set when an unreadable file could not be moved aside: saving would destroy it.
    private var readOnly = false

    init { load() }

    fun load(now: Instant = Instant.now()) {
        if (!file.exists()) return
        val loaded = try {
            balanceJson.decodeFromString<AppData>(file.readText())
        } catch (e: Exception) {
            // Never overwrite a file we couldn't read: set it aside so the user can recover it.
            val backup = File(file.parentFile, "balance.unreadable-${now.epochSecond}.json")
            try {
                Files.move(file.toPath(), backup.toPath())
                error = "Couldn't read balance.json. It was saved as ${backup.name}."
            } catch (e2: Exception) {
                readOnly = true
                error = "Couldn't read balance.json and couldn't set it aside. Changes won't be saved this session."
            }
            return
        }
        data = loaded
        finishTimerIfDue(now)
    }

    fun save() {
        if (readOnly) return
        try {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "balance.json.tmp")
            tmp.writeText(balanceJson.encodeToString(data))
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            error = "Couldn't save: ${e.message}"
        }
    }

    private fun update(change: AppData.() -> AppData) {
        data = data.change()
        save()
    }

    fun upsert(entry: Entry) = update {
        copy(entries = if (entries.any { it.id == entry.id }) entries.map { if (it.id == entry.id) entry else it } else entries + entry)
    }

    fun delete(entry: Entry) = update {
        copy(
            entries = entries.filter { it.id != entry.id },
            hiddenHealthIDs = if (entry.source == Source.HEALTH) hiddenHealthIDs + entry.id else hiddenHealthIDs,
        )
    }

    fun upsert(goal: Goal) = update {
        copy(goals = if (goals.any { it.id == goal.id }) goals.map { if (it.id == goal.id) goal else it } else goals + goal)
    }

    fun delete(goal: Goal) = update { copy(goals = goals.filter { it.id != goal.id }) }

    fun add(category: Category) = update { copy(customCategories = customCategories + category) }

    fun archive(category: Category) {
        if (data.customCategories.none { it.id == category.id }) return
        update { copy(customCategories = customCategories.map { if (it.id == category.id) it.copy(archived = true) else it }) }
    }

    fun startTimer(timer: ActiveTimer) = update { copy(activeTimer = timer) }

    /** Ends the timer and logs the time actually spent (capped at the planned length). Under 30 seconds logs nothing. */
    fun stopTimer(now: Instant = Instant.now()) {
        val t = data.activeTimer ?: return
        val minutes = minOf(t.minutes, (Duration.between(t.start, now).toMillis() / 60_000.0).roundToInt())
        update {
            copy(
                activeTimer = null,
                entries = if (minutes >= 1) entries + Entry(side = t.side, categoryID = t.categoryID, minutes = minutes, date = t.start) else entries,
            )
        }
    }

    /** Logs a timer that ran out while the app was closed or in the background. */
    fun finishTimerIfDue(now: Instant = Instant.now()) {
        val t = data.activeTimer
        if (t != null && now >= t.end) stopTimer(now)
    }

    /** Replaces Health-sourced entries inside the window with a fresh import, so edits and deletions in Health carry over. */
    fun replaceHealthEntries(entries: List<Entry>, start: Instant, end: Instant) = update {
        copy(
            entries = this.entries.filterNot { it.source == Source.HEALTH && it.date in start..end } +
                entries.filter { it.id !in hiddenHealthIDs },
        )
    }

    /** Turning it off removes all health entries. */
    fun setHealthEnabled(on: Boolean) = update {
        copy(healthEnabled = on, entries = if (on) entries else entries.filter { it.source != Source.HEALTH })
    }
}
