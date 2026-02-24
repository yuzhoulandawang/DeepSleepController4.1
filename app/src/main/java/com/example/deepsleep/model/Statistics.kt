package com.example.deepsleep.model

data class Statistics(
    // 总体统计
    val totalRuntime: Long = 0L,
    val totalOptimizations: Int = 0,
    val powerSaved: Long = 0L,
    val memoryReleased: Long = 0L,
    
    // GPU 优化统计
    val gpuOptimizations: Int = 0,
    val avgGpuFreq: Long = 770000000L,
    val gpuThrottlingCount: Int = 0,
    val currentGpuMode: String = "daily",
    
    // CPU 优化统计
    val cpuBindingCount: Int = 0,
    val currentCpuMode: String = "daily",
    val cpuUsageOptimized: Int = 0,
    
    // 进程压制统计
    val suppressedApps: Int = 0,
    val killedProcesses: Int = 0,
    val oomAdjustments: Int = 0,
    val avgOomScore: Int = 500,
    
    // 应用冻结统计
    val frozenApps: Int = 0,
    val thawedApps: Int = 0,
    val avgFreezeTime: Long = 0L,
    val preventedFreezes: Int = 0,
    
    // 场景检测统计
    val gameSceneCount: Int = 0,
    val navigationSceneCount: Int = 0,
    val chargingSceneCount: Int = 0,
    val callSceneCount: Int = 0,
    val castSceneCount: Int = 0,
    
    // 最近活动
    val recentActivities: List<String> = emptyList(),
    
    // 旧字段（保留兼容性）
    val totalEnterCount: Int = 0,
    val totalEnterSuccess: Int = 0,
    val totalExitCount: Int = 0,
    val totalExitSuccess: Int = 0,
    val totalAutoExitCount: Int = 0,
    val totalAutoExitRecover: Int = 0,
    val totalMaintenanceCount: Int = 0,
    val totalStateChangeCount: Int = 0,
    val serviceStartTime: Long = 0
)