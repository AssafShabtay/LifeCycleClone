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
 * Insights screen that summarizes how the user's time is distributed across categories. This
 * placeholder will eventually include donut charts and trend lines to visualize weekly and
 * monthly breakdowns along with correlations to sleep and health data.
 */
@Composable
fun InsightsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Insights")
        Text("Here you'll see breakdowns of your time by category and correlations with sleep.")
        Text("TODO: implement DonutChart and fetch weekly summaries.")
    }
}