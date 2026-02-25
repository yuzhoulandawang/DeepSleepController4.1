package com.example.deepsleep

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.deepsleep.data.StatsRepository
import com.example.deepsleep.service.DeepSleepService
import com.example.deepsleep.ui.logs.LogsScreen
import com.example.deepsleep.ui.main.MainScreen
import com.example.deepsleep.ui.main.MainViewModel
import com.example.deepsleep.ui.stats.SceneCheckScreen
import com.example.deepsleep.ui.stats.StatsScreen
import com.example.deepsleep.ui.theme.DeepSleepTheme
import com.example.deepsleep.ui.whitelist.WhitelistScreen
import com.example.deepsleep.root.RootCommander
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SettingsRepository 已在 Application 中初始化

        lifecycleScope.launch {
            StatsRepository.ensureLoaded()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        lifecycleScope.launch {
            RootCommander.requestRootAccess()
            viewModel.refreshRootStatus()
        }

        viewModel.registerScreenStateReceiver(this)

        Intent(this, DeepSleepService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        setContent {
            DeepSleepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                onNavigateToLogs = { navController.navigate("logs") },
                                onNavigateToWhitelist = { navController.navigate("whitelist") },
                                onNavigateToStats = { navController.navigate("stats") },
                                onNavigateToSceneCheck = { navController.navigate("sceneCheck") },
                                viewModel = viewModel
                            )
                        }
                        composable("logs") {
                            LogsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("whitelist") {
                            WhitelistScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("stats") {
                            StatsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("sceneCheck") {
                            SceneCheckScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            viewModel.refreshRootStatus()
        }
    }

    override fun onDestroy() {
        viewModel.unregisterScreenStateReceiver(this)
        super.onDestroy()
    }
}