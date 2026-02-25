package com.example.deepsleep

import android.app.Application
import com.example.deepsleep.data.SettingsRepository
import com.topjohnwu.superuser.Shell

class DeepSleepApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.initialize(this)
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }
}