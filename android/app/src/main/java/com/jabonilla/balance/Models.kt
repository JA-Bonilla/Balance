package com.jabonilla.balance

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Serializable
enum class Side(val label: String) {
    @SerialName("growth") GROWTH("Growth"),
    @SerialName("fun") FUN("Fun"),
}

@Serializable
enum class Source {
    @SerialName("manual") MANUAL,
    @SerialName("health") HEALTH,
}

/** Whole-second UTC dates like 2026-11-16T18:30:00Z; reads accept offsets and fractions. */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.SECONDS)))
    override fun deserialize(decoder: Decoder): Instant = OffsetDateTime.parse(decoder.decodeString()).toInstant()
}

/** UUIDs are written uppercase and normalized to uppercase on read. */
object UuidSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value.uppercase())
    override fun deserialize(decoder: Decoder): String = decoder.decodeString().uppercase()
}

fun newId(): String = UUID.randomUUID().toString().uppercase()

@Serializable
data class Category(
    val id: String,
    val name: String,
    val symbol: String,
    val side: Side,
    val blurb: String = "",
    /** Removed custom activities stay stored so old entries still show their name and icon. */
    val archived: Boolean = false,
) {
    companion object {
        val growth = listOf(
            Category("reading", "Reading", "book.fill", Side.GROWTH, "Books, articles, long-form reading"),
            Category("exercise", "Exercise", "figure.run", Side.GROWTH, "Cardio, strength, mobility"),
            Category("language", "Language", "character.bubble.fill", Side.GROWTH, "Practicing a new language"),
            Category("skill", "Skill Practice", "wrench.and.screwdriver.fill", Side.GROWTH, "Deliberate practice on a craft"),
            Category("meditation", "Meditation", "brain.head.profile", Side.GROWTH, "Mindfulness and quiet focus"),
            Category("journaling", "Journaling", "square.and.pencil", Side.GROWTH, "Writing and reflecting on your day"),
            Category("creative", "Creative Work", "paintbrush.pointed.fill", Side.GROWTH, "Drawing, writing, making things"),
            Category("courses", "Courses", "graduationcap.fill", Side.GROWTH, "Structured learning and classes"),
            Category("music", "Music Practice", "music.note", Side.GROWTH, "Instrument practice and theory"),
            Category("cooking", "Cooking", "frying.pan.fill", Side.GROWTH, "Cooking from scratch, meal prep"),
            Category("coding", "Coding", "chevron.left.forwardslash.chevron.right", Side.GROWTH, "Programming and technical projects"),
            Category("career", "Career & Finance", "chart.line.uptrend.xyaxis", Side.GROWTH, "Career and money goals"),
        )

        val fun_ = listOf(
            Category("social", "Social Media", "bubble.left.and.bubble.right.fill", Side.FUN, "Scrolling and posting"),
            Category("video", "Video", "play.tv.fill", Side.FUN, "Short and long-form video"),
            Category("gaming", "Gaming", "gamecontroller.fill", Side.FUN, "Video and mobile games"),
            Category("goingout", "Going Out", "party.popper.fill", Side.FUN, "Parties, bars, nights out"),
            Category("tv", "TV", "tv.fill", Side.FUN, "Shows and movies"),
            Category("browsing", "Browsing", "safari.fill", Side.FUN, "Open-ended browsing"),
            Category("listening", "Music & Podcasts", "headphones", Side.FUN, "Listening for entertainment"),
        )

        val all = growth + fun_
        fun list(side: Side) = if (side == Side.GROWTH) growth else fun_

        /** Icons offered for custom activities. */
        val customSymbols = listOf(
            "star.fill", "heart.fill", "leaf.fill", "flame.fill", "bolt.fill", "sun.max.fill",
            "moon.fill", "figure.walk", "figure.yoga", "dumbbell.fill", "bicycle", "tennisball.fill",
            "soccerball", "basketball.fill", "book.closed.fill", "pencil", "paintpalette.fill", "camera.fill",
            "guitars.fill", "pianokeys", "globe", "laptopcomputer", "gamecontroller.fill", "tv.fill",
            "film.fill", "popcorn.fill", "cup.and.saucer.fill", "wineglass.fill", "fork.knife", "cart.fill",
            "car.fill", "airplane", "pawprint.fill", "person.2.fill", "bubble.left.fill", "music.mic",
        )
    }
}

@Serializable(with = GoalSerializer::class)
data class Goal(
    val id: String = newId(),
    val categoryID: String,
    val minutesPerDay: Int,
    val daysPerWeek: Int,
) {
    val weeklyMinutes get() = minutesPerDay * daysPerWeek
}

@Serializable
private class GoalSurrogate(
    @Serializable(with = UuidSerializer::class) val id: String,
    val categoryID: String,
    val minutesPerDay: Int? = null,
    val daysPerWeek: Int? = null,
    val weeklyMinutes: Int? = null,
)

object GoalSerializer : KSerializer<Goal> {
    private val s = GoalSurrogate.serializer()
    override val descriptor = s.descriptor
    // weeklyMinutes is never written (null is omitted).
    override fun serialize(encoder: Encoder, value: Goal) =
        encoder.encodeSerializableValue(s, GoalSurrogate(value.id, value.categoryID, value.minutesPerDay, value.daysPerWeek))

    override fun deserialize(decoder: Decoder): Goal {
        val g = decoder.decodeSerializableValue(s)
        if (g.minutesPerDay != null) return Goal(g.id, g.categoryID, g.minutesPerDay, requireNotNull(g.daysPerWeek) { "daysPerWeek" })
        // v0.1 files stored only a weekly total; spread it over 5 days.
        return Goal(g.id, g.categoryID, maxOf(1, (requireNotNull(g.weeklyMinutes) { "weeklyMinutes" } + 4) / 5), 5)
    }
}

@Serializable
data class Entry(
    @Serializable(with = UuidSerializer::class) val id: String = newId(),
    val side: Side,
    val categoryID: String,
    val minutes: Int,
    @Serializable(with = InstantSerializer::class) val date: Instant,
    val source: Source = Source.MANUAL,
)

/** A running countdown; becomes an Entry when it ends or is stopped. */
@Serializable
data class ActiveTimer(
    val side: Side,
    val categoryID: String,
    @Serializable(with = InstantSerializer::class) val start: Instant,
    val minutes: Int,
) {
    val end: Instant get() = start.plusSeconds(minutes * 60L)
}

@Serializable
data class AppData(
    val goals: List<Goal> = emptyList(),
    val entries: List<Entry> = emptyList(),
    val healthEnabled: Boolean = false,
    /** Health samples the user deleted, so re-imports skip them. */
    val hiddenHealthIDs: Set<@Serializable(with = UuidSerializer::class) String> = emptySet(),
    val activeTimer: ActiveTimer? = null,
    val customCategories: List<Category> = emptyList(),
) {
    /** Built-in plus the user's active custom activities for one side. */
    fun categories(side: Side): List<Category> =
        Category.list(side) + customCategories.filter { it.side == side && !it.archived }

    /** Any category by id, including archived custom ones. */
    fun category(id: String): Category? = Category.all.firstOrNull { it.id == id } ?: customCategories.firstOrNull { it.id == id }

    // Inclusive of both ends, like DateInterval.contains.
    fun minutes(side: Side, start: Instant, end: Instant): Int =
        entries.filter { it.side == side && it.date in start..end }.sumOf { it.minutes }

    fun minutes(category: String, start: Instant, end: Instant): Int =
        entries.filter { it.categoryID == category && it.date in start..end }.sumOf { it.minutes }

    /** Bonsi grows with lifetime growth hours: 0 seed, 1+ sprout, 10+ sapling, 30+ bonsai. */
    val bonsiStage: Int
        get() {
            val hours = entries.filter { it.side == Side.GROWTH }.sumOf { it.minutes } / 60
            return if (hours >= 30) 3 else if (hours >= 10) 2 else if (hours >= 1) 1 else 0
        }

    companion object {
        /** Growth minutes per fun minute that counts as balanced. */
        const val GROWTH_PER_FUN = 0.2

        /** -1 = all fun, 0 = balanced (growth is about 20% of fun), +1 = all growth. */
        fun balance(growth: Int, fun_: Int): Double {
            val g = growth.toDouble()
            val target = GROWTH_PER_FUN * fun_
            return if (g + target == 0.0) 0.0 else (g - target) / (g + target)
        }

        /** Minutes between two times, rounded to the nearest 5 (half up). */
        fun roundedMinutes(start: Instant, end: Instant): Int =
            (Duration.between(start, end).toMillis() / 300_000.0).roundToInt() * 5

        /** Start of the week (first day from the locale, in the zone) and the same moment 7 days later. */
        fun thisWeek(now: Instant, zone: ZoneId, locale: Locale): Pair<Instant, Instant> {
            val first: DayOfWeek = WeekFields.of(locale).firstDayOfWeek
            val start = now.atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(first)).atStartOfDay(zone)
            return start.toInstant() to start.plusDays(7).toInstant()
        }
    }
}

val balanceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = true
}

/** "45 min", "2 hr", "1 hr 30 min". */
val Int.minutesText: String
    get() = (this / 60 to this % 60).let { (h, m) -> if (h == 0) "$m min" else if (m == 0) "$h hr" else "$h hr $m min" }

/** Compact form for chips and labels: "45m", "2h", "1h 30m". */
val Int.minutesShort: String
    get() = (this / 60 to this % 60).let { (h, m) -> if (h == 0) "${m}m" else if (m == 0) "${h}h" else "${h}h ${m}m" }
