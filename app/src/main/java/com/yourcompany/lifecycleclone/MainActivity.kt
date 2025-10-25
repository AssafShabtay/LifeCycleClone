package com.yourcompany.lifecycleclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.yourcompany.lifecycleclone.ui.LifeCycleCloneApp

/**
 * The entry point into the application. This activity simply sets the Compose content and
 * delegates the UI to [LifeCycleCloneApp].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    LifeCycleCloneApp()
                }
            }
        }
    }
}