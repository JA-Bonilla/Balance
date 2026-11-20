package com.jabonilla.balance

import android.app.Application
import java.io.File

/** Owns the one Store, so the activity, the timer receiver, and Health sync cannot log a timer twice. */
class BalanceApplication : Application() {
    val store: Store by lazy { Store(File(getExternalFilesDir(null) ?: filesDir, "balance.json")) }
}
