@file:OptIn(ExperimentalMindfulnessSessionApi::class)

package com.jabonilla.balance

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import kotlin.math.roundToInt

/** Imports exercise sessions (as Exercise) and mindfulness sessions (as Meditation) as growth minutes. */
object HealthSync {
    fun isAvailable(context: Context) = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun mindfulnessAvailable(client: HealthConnectClient) =
        client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    fun permissions(client: HealthConnectClient): Set<String> =
        setOf(HealthPermission.getReadPermission(ExerciseSessionRecord::class)) +
            if (mindfulnessAvailable(client)) setOf(HealthPermission.getReadPermission(MindfulnessSessionRecord::class)) else emptySet()

    val exercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)

    /** The same Health Connect record always maps to the same uppercase UUID, so hiddenHealthIDs keeps working. */
    fun healthEntry(recordId: String, start: Instant, end: Instant, categoryId: String) = Entry(
        id = java.util.UUID.nameUUIDFromBytes("healthconnect:$recordId".toByteArray(Charsets.UTF_8)).toString().uppercase(),
        side = Side.GROWTH,
        categoryID = categoryId,
        minutes = (Duration.between(start, end).toMillis() / 60_000.0).roundToInt(),
        date = start,
        source = Source.HEALTH,
    )

    class Import(val entries: List<Entry>, val start: Instant, val end: Instant)

    // shortcut: imports the last 28 days, one page of up to 1000 records per type; widen if users want older history.
    suspend fun fetch(client: HealthConnectClient, days: Long = 28): Import {
        val end = Instant.now()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(days).toInstant()
        val granted = client.permissionController.getGrantedPermissions()
        val range = TimeRangeFilter.between(start, end)
        val entries = mutableListOf<Entry>()
        if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted) {
            client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records
                .mapTo(entries) { healthEntry(it.metadata.id, it.startTime, it.endTime, "exercise") }
        }
        if (mindfulnessAvailable(client) && HealthPermission.getReadPermission(MindfulnessSessionRecord::class) in granted) {
            client.readRecords(ReadRecordsRequest(MindfulnessSessionRecord::class, range)).records
                .mapTo(entries) { healthEntry(it.metadata.id, it.startTime, it.endTime, "meditation") }
        }
        return Import(entries.filter { it.minutes > 0 }, start, end)
    }
}
