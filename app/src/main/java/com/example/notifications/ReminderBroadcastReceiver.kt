package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles daily reminder alarms and device boot events.
 * Queries the local SQLite database and only sends a push notification if there
 * are cards due for review today (`next_review_date <= NOW()`).
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefsRepo = UserPreferencesRepository(context)
                val prefs = prefsRepo.preferencesFlow.first()

                if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                    ReminderScheduler.scheduleDailyReminder(
                        context = context,
                        enabled = prefs.reminderEnabled,
                        hour = prefs.reminderHour,
                        minute = prefs.reminderMinute
                    )
                } else if (prefs.reminderEnabled) {
                    val dao = AppDatabase.getInstance(context).flashcardDao()
                    val dueCount = dao.countDueCards(System.currentTimeMillis())
                    if (dueCount > 0) {
                        ReminderScheduler.showDueCardsNotificationIfNeeded(context, dueCount)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
