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
 * Journal screen providing a weekly recap of the user's time and a photo gallery.  The real
 * implementation will query the WeeklyJournalEntity table and display summaries of hours spent
 * along with selected photos from the user's device.
 */
@Composable
fun JournalScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Weekly Journal")
        Text("A snapshot of your last 7 days will appear here.")
        Text("TODO: implement photo selection and summary generation.")
    }
}