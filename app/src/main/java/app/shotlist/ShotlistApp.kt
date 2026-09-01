package app.shotlist

import android.app.Application
import app.shotlist.diag.Diag
import app.shotlist.engine.EngineApi

class ShotlistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Diag.init(this)
        Diag.installCrashHook()
        EngineApi.ensureFreshClassification(this)
    }
}
