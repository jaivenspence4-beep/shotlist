package app.shotlist.actions

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.shotlist.data.ShotlistDb
import kotlinx.coroutines.runBlocking

class ShotlistReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = ShotlistActions.actionFromIntent(intent) ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (intent.action) {
            ShotlistActions.ACTION_POST, ShotlistActions.ACTION_REMINDER -> {
                manager.notify(
                    ShotlistActions.notificationId(action),
                    ShotlistActions.buildReminderNotification(context, action),
                )
            }
            ShotlistActions.ACTION_ACCEPT -> {
                manager.cancel(ShotlistActions.notificationId(action))
                ShotlistActions.scheduleEventReminders(context, action)
                context.startActivity(
                    ShotlistActions.calendarInsertIntent(action)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                updateState(context, action, "ACCEPTED")
            }
            ShotlistActions.ACTION_DISMISS -> {
                manager.cancel(ShotlistActions.notificationId(action))
                updateState(context, action, "DISMISSED")
            }
        }
    }

    private fun updateState(context: Context, action: ShotlistAction, state: String) {
        val findingId = action.findingId ?: return
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    ShotlistDb.get(context.applicationContext).findings().setState(findingId, state)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
