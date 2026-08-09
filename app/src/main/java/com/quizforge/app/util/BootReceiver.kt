package com.quizforge.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reschedules the announcement background check after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AnnouncementNotifier.schedule(context)
        }
    }
}
