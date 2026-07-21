package com.roadtrippin.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import com.roadtrippin.shared.ui.RoadtrippinApp
import com.roadtrippin.shared.diagnostics.Diagnostics

fun MainViewController(): UIViewController {
    Diagnostics.initialize()
    return ComposeUIViewController { RoadtrippinApp() }
}
