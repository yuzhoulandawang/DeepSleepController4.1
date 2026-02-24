package com.example.deepsleep.root

import com.example.deepsleep.model.DozeState

object DozeController {
    suspend fun getState(): DozeState {
        val output = RootCommander.exec("dumpsys deviceidle | grep 'mState=' | head -1").out.firstOrNull() ?: ""
        return when {
            output.contains("ACTIVE") -> DozeState.ACTIVE
            output.contains("INACTIVE") -> DozeState.INACTIVE
            output.contains("IDLE_MAINTENANCE") -> DozeState.IDLE_MAINTENANCE
            output.contains("IDLE") -> DozeState.IDLE
            else -> DozeState.UNKNOWN
        }
    }

    suspend fun forceIdle(): Boolean = RootCommander.exec("dumpsys deviceidle force-idle").isSuccess
    suspend fun step(): Boolean = RootCommander.exec("dumpsys deviceidle step").isSuccess
    suspend fun disableMotion(): Boolean = RootCommander.exec("dumpsys deviceidle disable motion").isSuccess
    suspend fun enableMotion(): Boolean = RootCommander.exec("dumpsys deviceidle enable motion").isSuccess
}