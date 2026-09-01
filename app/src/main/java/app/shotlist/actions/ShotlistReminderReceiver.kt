package app.shotlist.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ShotlistReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Receiver wiring belongs to the action layer. Notification delivery will be
        // connected once engine-created ActionItems are available.
    }
}
