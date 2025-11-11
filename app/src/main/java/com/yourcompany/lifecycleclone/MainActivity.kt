package com.yourcompany.lifecycleclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import com.yourcompany.lifecycleclone.settings.TrackingController
import com.yourcompany.lifecycleclone.ui.LifeCycleCloneApp

class MainActivity : ComponentActivity() {

    private var permissionsFlowInFlight: Boolean = false

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionsFlowInFlight = false
        if (hasAllTrackingPermissions()) {
            startTrackingIfPermitted()
        } else {
            Toast.makeText(
                this,
                "Tracking stays paused until all permissions are granted.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasAllTrackingPermissions()) {
            launchPermissionsFlow()
        } else {
            startTrackingIfPermitted()
        }

        setContent {
            MaterialTheme {
                Surface {
                    LifeCycleCloneApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasAllTrackingPermissions()) {
            startTrackingIfPermitted()
        }
    }

    private fun launchPermissionsFlow() {
        if (permissionsFlowInFlight) return
        permissionsFlowInFlight = true
        permissionsLauncher.launch(Intent(this, PermissionsActivity::class.java))
    }

    private fun startTrackingIfPermitted() {
        if (!hasAllTrackingPermissions()) {
            launchPermissionsFlow()
            return
        }
        TrackingController.startTracking(this)
    }

    private fun hasAllTrackingPermissions(): Boolean {
        val hasForeground = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val backgroundOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val activityOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        val notificationsOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        return hasForeground && backgroundOk && activityOk && notificationsOk
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}
