package com.jinian.anniversary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jinian.anniversary.reminder.NotificationHelper
import com.jinian.anniversary.ui.detail.DetailScreen
import com.jinian.anniversary.ui.editor.EditorScreen
import com.jinian.anniversary.ui.home.HomeScreen
import com.jinian.anniversary.ui.settings.SettingsScreen
import com.jinian.anniversary.ui.theme.JinianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialEventId = intent?.getLongExtra(NotificationHelper.EXTRA_EVENT_ID, -1L) ?: -1L
        setContent {
            JinianApp(initialEventId = initialEventId)
        }
    }
}

@Composable
fun JinianApp(initialEventId: Long) {
    val viewModel: JinianViewModel = viewModel()
    val settings by viewModel.settings.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(initialEventId) {
        if (initialEventId > 0L) navController.navigate("detail/$initialEventId")
    }

    JinianTheme(settings = settings) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home",
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onAdd = { navController.navigate("editor/new") },
                        onEvent = { navController.navigate("detail/$it") },
                        onSettings = { navController.navigate("settings") },
                    )
                }
                                composable("editor/new") { entry ->
                    EditorScreen(
                        viewModel = viewModel,
                        eventId = -1L,
                        formResetKey = "new-${entry.id}",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "editor/{eventId}",
                    arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
                ) { entry ->
                    val eventId = entry.arguments?.getLong("eventId") ?: -1L
                    EditorScreen(
                        viewModel = viewModel,
                        eventId = eventId,
                        formResetKey = "event-${entry.id}-$eventId",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "detail/{eventId}",
                    arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
                ) { entry ->
                    val eventId = entry.arguments?.getLong("eventId") ?: -1L
                    DetailScreen(
                        viewModel = viewModel,
                        eventId = eventId,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("editor/$it") },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}