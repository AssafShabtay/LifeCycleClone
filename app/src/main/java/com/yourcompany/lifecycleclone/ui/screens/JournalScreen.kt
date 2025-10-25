package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.serialization.json.Json

/**
 * Journal screen showing a weekly summary of time spent in various categories.  It loads the
 * summary from a [JournalViewModel] and displays the summary text along with any photo URIs.
 */
@Composable
fun JournalScreen(navController: NavController) {
    val viewModel: JournalViewModel = viewModel(factory = JournalViewModel.Factory)
    val journal = viewModel.journal.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Weekly Journal",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (journal == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Generating weekly summary…")
            }
        } else {
            // Decode summary lines from JSON (stored as JSON string)
            val summaryLines: String = Json.decodeFromString(journal.summaryJson)
            Text(summaryLines)
            // Decode photo URIs; currently empty list
            val photos: List<String> = Json.decodeFromString(journal.photoUrisJson)
            if (photos.isNotEmpty()) {
                Text("Photos:")
                photos.forEach { uri ->
                    Text(uri, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}