package app.shotlist

import android.app.Application
import app.shotlist.diag.Diag

class ShotlistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Diag.init(this)
        Diag.installCrashHook()
    }
}
