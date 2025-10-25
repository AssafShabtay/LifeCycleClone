package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

/**
 * Places screen where users can manage their frequently visited locations such as Home and Work.
 * Users can view existing places and add new ones by entering a label.  For demonstration
 * purposes new places are created at (0,0) with a default category.  Deleting places is also
 * supported.
 */
@Composable
fun PlacesScreen(navController: NavController) {
    val viewModel: PlacesViewModel = viewModel(factory = PlacesViewModel.Factory)
    val places by viewModel.places.collectAsState()
    var newPlaceLabel by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Places",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = newPlaceLabel,
            onValueChange = { newPlaceLabel = it },
            label = { Text("New place label") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (newPlaceLabel.isNotBlank()) {
                    viewModel.addPlace(
                        label = newPlaceLabel,
                        latitude = 0.0,
                        longitude = 0.0,
                        category = "unknown"
                    )
                    newPlaceLabel = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Place")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (places.isEmpty()) {
            Text("No places saved yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(places) { place ->
                    PlaceRow(place.label) {
                        viewModel.deletePlace(place.placeId)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(label: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Button(onClick = onDelete) {
            Text("Delete")
        }
    }
}