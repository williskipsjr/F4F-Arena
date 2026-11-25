package com.f4f.arena

import android.app.Application
import com.f4f.arena.data.auth.SessionManager

class F4FArenaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
