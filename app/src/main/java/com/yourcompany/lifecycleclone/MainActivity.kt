package com.yourcompany.lifecycleclone

import android.Manifest
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
import com.yourcompany.lifecycleclone.ui.LifeCycleCloneApp

class MainActivity : ComponentActivity() {

    private val basePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { granted -> !granted }.keys
        if (denied.isNotEmpty()) {
            Toast.makeText(
                this,
                "Some permissions were denied. Features may be limited until granted in Settings.",
                Toast.LENGTH_LONG
            ).show()
        }
        requestBackgroundLocationIfNeeded()
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            Toast.makeText(
                this,
                "Background location is required for automatic visit tracking. Enable it in Settings if you change your mind.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()
        setContent {
            MaterialTheme {
                Surface {
                    LifeCycleCloneApp()
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableSetOf<String>()

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionsToRequest += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            permissionsToRequest += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            permissionsToRequest += Manifest.permission.ACTIVITY_RECOGNITION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                permissionsToRequest += Manifest.permission.READ_MEDIA_IMAGES
            }
            if (!hasPermission(Manifest.permission.READ_MEDIA_VIDEO)) {
                permissionsToRequest += Manifest.permission.READ_MEDIA_VIDEO
            }
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                permissionsToRequest += Manifest.permission.POST_NOTIFICATIONS
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            basePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return
        }
        if (!hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}
