package com.jabonilla.balance

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import java.time.Instant

fun Context.prefs(): SharedPreferences = getSharedPreferences("balance", Context.MODE_PRIVATE)

/** Sounds bundled in res/raw; Android fixes a channel's sound at creation, so each sound has its own channel. */
enum class TimerSound(val id: String, val label: String, val raw: Int?) {
    DEFAULT("default", "System Default", null),
    CHIME("chime", "Chime", R.raw.chime),
    BELL("bell", "Bell", R.raw.bell),
    PULSE("pulse", "Pulse", R.raw.pulse),
    GENTLE("gentle", "Gentle", R.raw.gentle);

    val channelId get() = "timer_$id"

    private fun uri(context: Context): Uri =
        if (raw == null) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        else Uri.parse("android.resource://${context.packageName}/raw/$id")

    /** Plays the sound in-app so the user can hear it while choosing. */
    fun preview(context: Context) {
        player?.release()
        player = null
        if (raw == null) {
            RingtoneManager.getRingtone(context, uri(context))?.play()
            return
        }
        player = MediaPlayer.create(context, raw)?.apply {
            setOnCompletionListener { it.release(); if (player === it) player = null }
            start()
        }
    }

    companion object {
        const val KEY = "timerSound"
        private var player: MediaPlayer? = null

        fun current(context: Context): TimerSound =
            entries.firstOrNull { it.id == context.prefs().getString(KEY, null) } ?: CHIME

        fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            for (s in entries) {
                nm.createNotificationChannel(
                    NotificationChannel(s.channelId, "Timer (${s.label})", NotificationManager.IMPORTANCE_HIGH).apply { setSound(s.uri(context), attrs) },
                )
            }
        }
    }
}

/** Alarm plus "timer done" notification, so the user knows even with the phone locked or the app closed. */
object TimerAlarm {
    private const val NOTIFICATION_ID = 1

    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context, 1, Intent(context, TimerReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(context: Context, timer: ActiveTimer) {
        val am = context.getSystemService(AlarmManager::class.java)
        val at = timer.end.toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context))
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    /** Logs a due timer and, if it did, posts "Timer done". Shared by the alarm receiver and the open app so exactly one runs the log. */
    fun finishIfDue(context: Context, store: Store, now: Instant = Instant.now()) {
        val timer = store.data.activeTimer ?: return
        store.finishTimerIfDue(now)
        if (store.data.activeTimer != null) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return
        TimerSound.createChannels(context)
        val name = store.data.category(timer.categoryID)?.name ?: "Activity"
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            NOTIFICATION_ID,
            Notification.Builder(context, TimerSound.current(context).channelId)
                .setSmallIcon(R.drawable.ic_eco)
                .setContentTitle("Timer done")
                .setContentText("$name: ${timer.minutes.minutesText} logged.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerAlarm.finishIfDue(context, (context.applicationContext as BalanceApplication).store)
    }
}

/** Pick an activity and a length; the time is logged automatically when the countdown ends. */
@Composable
fun TimerSheet(side: Side, store: Store, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val last = remember { context.prefs().getString("lastCategory", "") }
    val options = store.data.categories(side)
    var categoryID by remember { mutableStateOf(if (options.any { it.id == last }) last!! else options[0].id) }
    var minutes by remember { mutableIntStateOf(30) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    SheetScaffold(
        title = "${side.label} Timer", tint = sideColor(side), saveLabel = "Start", onDismiss = onDismiss,
        onSave = {
            // The timer starts whether or not notifications are allowed; it is still logged when due.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            val timer = ActiveTimer(side, categoryID, Instant.now(), minutes)
            store.startTimer(timer)
            TimerAlarm.schedule(context, timer)
            onDismiss()
        },
    ) {
        ActivityPicker(side, categoryID, store.data) { categoryID = it }
        Column(Modifier.padding(top = 16.dp)) {
            Text("Timer length", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
            MinutesPicker(minutes, sideColor(side), onMinutes = { minutes = it })
            Text(
                "When the timer ends, the time is logged for you. Stop early to log just the time so far.",
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
