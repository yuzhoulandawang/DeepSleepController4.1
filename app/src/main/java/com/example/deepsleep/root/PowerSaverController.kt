package com.example.deepsleep.root

object PowerSaverController {
    suspend fun enablePowerSaver(): Boolean = RootCommander.exec("settings put global low_power 1").isSuccess
    suspend fun disablePowerSaver(): Boolean = RootCommander.exec("settings put global low_power 0").isSuccess
    suspend fun isEnabled(): Boolean = RootCommander.exec("settings get global low_power").out.firstOrNull()?.trim() == "1"
}