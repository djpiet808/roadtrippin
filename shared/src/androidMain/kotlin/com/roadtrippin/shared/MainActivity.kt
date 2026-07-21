package com.roadtrippin.shared

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.roadtrippin.shared.data.DatabaseFactory
import com.roadtrippin.shared.cloud.CloudConfig
import com.roadtrippin.shared.cloud.CloudServices
import com.roadtrippin.shared.cloud.SecureSessionStorage
import com.roadtrippin.shared.diagnostics.Diagnostics
import com.roadtrippin.shared.diagnostics.DiagnosticsConfig
import com.roadtrippin.shared.platform.PlatformServices
import com.roadtrippin.shared.ui.RoadtrippinApp
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformServices.initialize(this)
        DatabaseFactory.initialize(this)
        CloudConfig.initialize(this)
        SecureSessionStorage.initialize(this)
        DiagnosticsConfig.initialize(this)
        Diagnostics.initialize()
        CloudServices.client?.handleDeeplinks(intent)
        setContent { RoadtrippinApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        CloudServices.client?.handleDeeplinks(intent)
    }
}
