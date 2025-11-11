package com.yourcompany.lifecycleclone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Walks the user through granting the required permissions for automatic tracking. The activity
 * requests foreground location, background location, activity recognition and notifications
 * sequentially, guiding the user with a short explanation for each step. Once everything is
 * granted the activity finishes with [Activity.RESULT_OK].
 */
class PermissionsActivity : ComponentActivity() {

    enum class Step {
        ForegroundLocation,
        BackgroundLocation,
        ActivityRecognition,
        Notifications
    }

    private var currentStep: Step? = null
    private val pendingSteps = ArrayDeque<Step>()

    private val foregroundLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (!granted) {
            showDeniedToast()
        }
        requestNext()
    }

    private val singlePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            showDeniedToast()
        }
        requestNext()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rebuildQueue()

        setContent {
            MaterialTheme {
                Surface {
                    PermissionsScreen(
                        step = currentStep,
                        onOpenSettings = { openAppSettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rebuildQueue()
        requestNext()
    }

    private fun rebuildQueue() {
        pendingSteps.clear()
        if (!hasForegroundLocation()) {
            pendingSteps += Step.ForegroundLocation
        }
        if (needsBackgroundLocation() && !hasBackgroundLocation()) {
            pendingSteps += Step.BackgroundLocation
        }
        if (needsActivityRecognition() && !hasActivityRecognition()) {
            pendingSteps += Step.ActivityRecognition
        }
        if (needsNotificationPermission() && !hasNotifications()) {
            pendingSteps += Step.Notifications
        }
    }

    private fun requestNext() {
        if (allTrackingPermissionsGranted()) {
            setResult(Activity.RESULT_OK)
            finish()
            return
        }
        val nextStep = pendingSteps.removeFirstOrNull()
        currentStep = nextStep
        when (nextStep) {
            Step.ForegroundLocation -> {
                foregroundLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            Step.BackgroundLocation -> {
                singlePermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            Step.ActivityRecognition -> {
                singlePermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            Step.Notifications -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    requestNext()
                }
            }
            null -> {
                if (!allTrackingPermissionsGranted()) {
                    Toast.makeText(
                        this,
                        "Permissions remain denied. Enable them in Settings to continue.",
                        Toast.LENGTH_LONG
                    ).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    private fun allTrackingPermissionsGranted(): Boolean {
        return hasForegroundLocation() && (!needsBackgroundLocation() || hasBackgroundLocation()) &&
            (!needsActivityRecognition() || hasActivityRecognition()) &&
            (!needsNotificationPermission() || hasNotifications())
    }

    private fun hasForegroundLocation(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun needsBackgroundLocation(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun hasBackgroundLocation(): Boolean {
        if (!needsBackgroundLocation()) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun needsActivityRecognition(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun hasActivityRecognition(): Boolean {
        if (!needsActivityRecognition()) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun hasNotifications(): Boolean {
        if (!needsNotificationPermission()) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun showDeniedToast() {
        Toast.makeText(
            this,
            "Permission denied. You can enable it in System Settings if needed.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openAppSettings() {
        val uri = android.net.Uri.fromParts("package", packageName, null)
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = uri
        })
    }
}

@Composable
private fun PermissionsScreen(step: PermissionsActivity.Step?, onOpenSettings: () -> Unit) {
    val message by remember(step) {
        mutableStateOf(
            when (step) {
                PermissionsActivity.Step.ForegroundLocation -> "Allow location so tracking can begin."
                PermissionsActivity.Step.BackgroundLocation -> "Allow background location to log visits when the app is closed."
                PermissionsActivity.Step.ActivityRecognition -> "Allow activity recognition to detect walks, drives, and other motion."
                PermissionsActivity.Step.Notifications -> "Allow notifications so the foreground tracker can run."
                null -> "Review app permissions in Settings if you previously denied them."
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
}
