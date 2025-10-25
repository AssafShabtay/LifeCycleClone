package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Places screen where users can manage their frequently visited locations such as Home and Work.
 * In a complete implementation this screen will allow renaming, editing radius and adding new
 * geofences.  For now it simply displays a placeholder message.
 */
@Composable
fun PlacesScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Places")
        Text("Manage your saved locations like Home, Work, Gym, etc.")
        Text("TODO: implement place management UI and geofence registration.")
    }
}