package com.jabonilla.balance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

// Contract example, as iOS writes it (docs/contracts/balance-json.md).
private const val IOS_FILE = """
{
  "activeTimer" : { "categoryID" : "gaming", "minutes" : 30, "side" : "fun", "start" : "2026-11-16T20:00:00Z" },
  "customCategories" : [
    { "archived" : false, "blurb" : "", "id" : "custom-0B1C2D3E-4F50-6172-8394-A5B6C7D8E9F0", "name" : "Gardening", "side" : "growth", "symbol" : "leaf.fill" }
  ],
  "entries" : [
    { "categoryID" : "reading", "date" : "2026-11-16T18:30:00Z", "id" : "5E0A7C6B-1D2E-4F30-9A8B-7C6D5E4F3A2B", "minutes" : 45, "side" : "growth", "source" : "manual" }
  ],
  "goals" : [
    { "categoryID" : "reading", "daysPerWeek" : 5, "id" : "27D35D15-0370-4BB3-A1AB-1796E951729F", "minutesPerDay" : 15 }
  ],
  "healthEnabled" : true,
  "hiddenHealthIDs" : [ "9F8E7D6C-5B4A-4392-8170-6F5E4D3C2B1A" ]
}
"""

class BalanceTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file(text: String? = null) = File(tmp.newFolder(), "balance.json").also { if (text != null) it.writeText(text) }
    private fun at(s: String) = Instant.parse(s)

    @Test fun ac3_balanceScore() {
        assertEquals(0.0, AppData.balance(0, 0), 0.0)
        assertEquals(0.0, AppData.balance(60, 300), 0.0)
        assertEquals(1.0, AppData.balance(90, 0), 0.0)
        assertEquals(-1.0, AppData.balance(0, 45), 0.0)
        assertEquals(-0.0909, AppData.balance(210, 1260), 0.001)
        assertEquals(-0.6667, AppData.balance(60, 1500), 0.001)
        assertEquals(0.5385, AppData.balance(400, 600), 0.001)
    }

    @Test fun ac4_weeklyTotalsUseCurrentWeekOnly() {
        val zone = ZoneId.of("UTC")
        val (start, end) = AppData.thisWeek(at("2026-11-18T12:00:00Z"), zone, Locale.US)
        assertEquals(at("2026-11-15T00:00:00Z"), start)
        assertEquals(at("2026-11-22T00:00:00Z"), end)
        val e = { id: String, side: Side, cat: String, date: String, m: Int -> Entry(id, side, cat, m, at(date)) }
        val data = AppData(entries = listOf(
            e("A", Side.GROWTH, "reading", "2026-11-14T23:00:00Z", 100), // an hour before the week starts
            e("B", Side.GROWTH, "reading", "2026-11-15T00:00:00Z", 10),
            e("C", Side.FUN, "gaming", "2026-11-17T10:00:00Z", 20),
            e("D", Side.GROWTH, "exercise", "2026-11-18T10:00:00Z", 30),
        ))
        assertEquals(40, data.minutes(Side.GROWTH, start, end))
        assertEquals(20, data.minutes(Side.FUN, start, end))
        assertEquals(10, data.minutes("reading", start, end))
    }

    @Test fun ac5_iosFileLoadsIntact() {
        val d = balanceJson.decodeFromString<AppData>(IOS_FILE)
        assertEquals(Goal("27D35D15-0370-4BB3-A1AB-1796E951729F", "reading", 15, 5), d.goals.single())
        assertEquals(Entry("5E0A7C6B-1D2E-4F30-9A8B-7C6D5E4F3A2B", Side.GROWTH, "reading", 45, at("2026-11-16T18:30:00Z"), Source.MANUAL), d.entries.single())
        assertEquals(Category("custom-0B1C2D3E-4F50-6172-8394-A5B6C7D8E9F0", "Gardening", "leaf.fill", Side.GROWTH), d.customCategories.single())
        assertTrue(d.healthEnabled)
        assertEquals(setOf("9F8E7D6C-5B4A-4392-8170-6F5E4D3C2B1A"), d.hiddenHealthIDs)
        assertEquals(ActiveTimer(Side.FUN, "gaming", at("2026-11-16T20:00:00Z"), 30), d.activeTimer)
        // A Store load also logs that timer (AC-10) because it is long past: the entry plus the logged timer.
        assertEquals(2, Store(file(IOS_FILE)).data.entries.size)
        assertEquals(AppData(), Store(file("{}")).data)
    }

    @Test fun ac6_legacyFileLoads() {
        val d = Store(file("""{"goals":[{"id":"27D35D15-0370-4BB3-A1AB-1796E951729F","categoryID":"reading","weeklyMinutes":60}],"entries":[],"healthEnabled":false}""")).data
        assertEquals(12, d.goals.single().minutesPerDay)
        assertEquals(5, d.goals.single().daysPerWeek)
        assertTrue(d.hiddenHealthIDs.isEmpty())
    }

    @Test fun ac7_writtenFileFormat() {
        val f = file()
        val s = Store(f)
        s.upsert(Goal(categoryID = "reading", minutesPerDay = 15, daysPerWeek = 4))
        s.upsert(Entry(side = Side.GROWTH, categoryID = "reading", minutes = 30, date = Instant.parse("2026-11-16T18:30:00.789Z")))
        s.add(Category("custom-X", "Gardening", "leaf.fill", Side.GROWTH))
        val text = f.readText()
        assertTrue(text.contains("\"date\": \"2026-11-16T18:30:00Z\""))
        assertTrue(Regex("\"id\": \"[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\"").containsMatchIn(text))
        assertTrue(text.contains("\"blurb\"") && text.contains("\"archived\"") && text.contains("\"source\": \"manual\""))
        assertFalse(text.contains("weeklyMinutes"))
        assertFalse(text.contains("activeTimer"))
    }

    @Test fun ac8_saveAndReload() {
        val f = file()
        val s = Store(f)
        val goal = Goal(categoryID = "reading", minutesPerDay = 15, daysPerWeek = 4)
        val entry = Entry(side = Side.FUN, categoryID = "tv", minutes = 20, date = at("2026-11-16T18:30:00Z"))
        s.upsert(goal); s.upsert(entry)
        val d = Store(f).data
        assertEquals(60, d.goals.single().weeklyMinutes)
        assertEquals(entry, d.entries.single())
    }

    @Test fun ac9_unreadableFileIsSetAside() {
        val f = file("not json")
        val s = Store(f)
        assertTrue(s.error!!.startsWith("Couldn't read balance.json. It was saved as balance.unreadable-"))
        assertEquals(AppData(), s.data)
        assertTrue(f.parentFile!!.listFiles()!!.any { it.name.contains("unreadable") })
    }

    @Test fun ac9_failedRenameBlocksSaving() {
        val f = file("not json")
        assumeTrue(f.parentFile!!.setWritable(false) && !f.parentFile!!.canWrite())
        try {
            val s = Store(f)
            s.upsert(Goal(categoryID = "reading", minutesPerDay = 15, daysPerWeek = 4))
            assertEquals("not json", f.readText())
        } finally {
            f.parentFile!!.setWritable(true)
        }
    }

    @Test fun ac10_timerLogic() {
        val start = at("2026-11-16T20:00:00Z")
        val timer = ActiveTimer(Side.FUN, "gaming", start, 30)
        // Due while the app was closed: logs the planned minutes at the start.
        val f = file()
        Store(f).startTimer(timer)
        val loaded = Store(f)
        assertNull(loaded.data.activeTimer)
        assertEquals(30, loaded.data.entries.single().minutes)
        assertEquals(start, loaded.data.entries.single().date)

        fun fresh() = Store(file()).also { it.startTimer(timer) }
        fresh().apply { finishTimerIfDue(start.plusSeconds(29 * 60)); assertNotNull(data.activeTimer) }
        fresh().apply { stopTimer(start.plusSeconds(10 * 60 + 30)); assertEquals(11, data.entries.single().minutes) } // half up
        fresh().apply { stopTimer(start.plusSeconds(45 * 60)); assertEquals(30, data.entries.single().minutes) } // capped
        fresh().apply { stopTimer(start.plusSeconds(29)); assertTrue(data.entries.isEmpty()); assertNull(data.activeTimer) }
        fresh().apply { stopTimer(start.plusSeconds(30)); assertEquals(1, data.entries.single().minutes) }
    }

    @Test fun ac13_customRounding() {
        val s = at("2026-11-16T10:00:00Z")
        assertEquals(90, AppData.roundedMinutes(s, s.plusSeconds(92 * 60)))
        assertEquals(95, AppData.roundedMinutes(s, s.plusSeconds(93 * 60)))
        assertEquals(95, AppData.roundedMinutes(s, s.plusSeconds(92 * 60 + 30)))
        assertEquals(0, AppData.roundedMinutes(s, s.plusSeconds(2 * 60)))
    }

    @Test fun ac16_customActivities() {
        val s = Store(file())
        val c = Category("custom-1", "Gardening", "leaf.fill", Side.GROWTH)
        s.add(c)
        assertTrue(s.data.categories(Side.GROWTH).any { it.id == "custom-1" })
        assertFalse(s.data.categories(Side.FUN).any { it.id == "custom-1" })
        s.archive(c)
        assertFalse(s.data.categories(Side.GROWTH).any { it.id == "custom-1" })
        assertEquals("Gardening", s.data.category("custom-1")?.name)
    }

    @Test fun ac17_iconsAndSounds() {
        for (sf in Category.all.map { it.symbol } + Category.customSymbols) assertTrue(sf, sf in symbolNames)
        assertEquals(R.drawable.ic_circle, symbolIcon("no.such.symbol"))
        assertEquals("brain head profile", symbolLabel("brain.head.profile"))
        assertEquals("star", symbolLabel("star.fill"))
        for (n in listOf("chime", "bell", "pulse", "gentle")) assertTrue(n, File("src/main/res/raw/$n.wav").exists())
    }

    @Test fun ac18_bonsiStage() {
        fun stage(side: Side, m: Int) = AppData(entries = if (m == 0) emptyList() else listOf(Entry(side = side, categoryID = "x", minutes = m, date = Instant.now()))).bonsiStage
        assertEquals(0, AppData().bonsiStage)
        assertEquals(0, stage(Side.GROWTH, 59))
        assertEquals(1, stage(Side.GROWTH, 60))
        assertEquals(2, stage(Side.GROWTH, 600))
        assertEquals(3, stage(Side.GROWTH, 1800))
        assertEquals(0, stage(Side.FUN, 1800))
    }

    @Test fun ac19_skyParamsAndCaption() {
        assertEquals(1.0, SkyParams.at(-1.0).rim, 0.0)
        assertEquals(3.0, SkyParams.at(0.0).stars, 0.0)
        assertEquals(1.0, SkyParams.at(1.0).desert, 0.0)
        assertEquals(1.0, SkyParams.at(5.0).desert, 0.0)
        val q = SkyParams.at(0.25)
        assertTrue(q.clouds > 0 && q.clouds < 1)
        assertTrue(q.sunR > SkyParams.golden.sunR && q.sunR < SkyParams.day.sunR)
        assertEquals("Log some minutes to wake your Bonsi", balanceCaption(0, 0))
        assertEquals("Deep night · the moon covers the sun", balanceCaption(0, 45))
        assertEquals("Golden hour · in balance", balanceCaption(60, 300))
        assertEquals("Heat wave · time to rest and play", balanceCaption(90, 0))
    }

    @Test fun ac22_healthIdsAreStableAndStayDeleted() {
        val a = HealthSync.healthEntry("rec-1", at("2026-11-16T10:00:00Z"), at("2026-11-16T10:30:00Z"), "exercise")
        val b = HealthSync.healthEntry("rec-1", at("2026-11-16T10:00:00Z"), at("2026-11-16T10:30:00Z"), "exercise")
        assertEquals(a.id, b.id)
        assertEquals(a.id, a.id.uppercase())
        assertEquals(30, a.minutes)
        assertEquals(Source.HEALTH, a.source)
        val s = Store(file())
        val w = at("2026-11-01T00:00:00Z") to at("2026-11-30T00:00:00Z")
        s.replaceHealthEntries(listOf(a), w.first, w.second)
        assertEquals(1, s.data.entries.size)
        s.delete(s.data.entries.single())
        s.replaceHealthEntries(listOf(b), w.first, w.second)
        assertTrue(s.data.entries.isEmpty())
        s.setHealthEnabled(false)
        assertFalse(s.data.healthEnabled)
    }

    @Test fun ac22_reimportOfSessionBeforeWindowDoesNotDuplicate() {
        val a = HealthSync.healthEntry("rec-2", at("2026-10-31T23:50:00Z"), at("2026-11-01T00:20:00Z"), "exercise")
        val s = Store(file())
        val w = at("2026-11-01T00:00:00Z") to at("2026-11-30T00:00:00Z")
        repeat(2) { s.replaceHealthEntries(listOf(a), w.first, w.second) }
        assertEquals(1, s.data.entries.size)
    }
}
