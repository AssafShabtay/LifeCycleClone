package com.yourcompany.lifecycleclone.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourcompany.lifecycleclone.ui.screens.HomeTimelineScreen
import com.yourcompany.lifecycleclone.ui.screens.InsightsScreen
import com.yourcompany.lifecycleclone.ui.screens.JournalScreen
import com.yourcompany.lifecycleclone.ui.screens.PlacesScreen
import com.yourcompany.lifecycleclone.ui.screens.SettingsScreen

/**
 * Top‑level composable that sets up navigation and bottom navigation bar.  It defines five
 * destinations corresponding to the major sections of the Life Cycle clone: timeline, insights,
 * weekly journal, places and settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeCycleCloneApp() {
    val navController: NavHostController = rememberNavController()

    val navItems = listOf(
        NavItem("timeline", "Timeline"),
        NavItem("insights", "Insights"),
        NavItem("journal", "Journal"),
        NavItem("places", "Places"),
        NavItem("settings", "Settings")
    )

    Column(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "timeline"
            ) {
                composable("timeline") { HomeTimelineScreen(navController) }
                composable("insights") { InsightsScreen(navController) }
                composable("journal") { JournalScreen(navController) }
                composable("places") { PlacesScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
            }
        }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        NavigationBar {
            navItems.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                // Pop up to the start destination to avoid building up a huge
                                // back stack of the same screens.
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item.
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    label = { Text(item.label) },
                    icon = { /* No icon defined. You can add icons here. */ }
                )
            }
        }
    }
}

/**
 * Helper data class representing a bottom navigation item with a route and a user‑visible label.
 */
data class NavItem(val route: String, val label: String)