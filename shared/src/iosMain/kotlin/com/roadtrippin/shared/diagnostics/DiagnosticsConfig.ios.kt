package com.roadtrippin.shared.diagnostics

import platform.Foundation.NSBundle

actual object DiagnosticsConfig {
    actual val dsn: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("SENTRY_DSN") as? String ?: ""
}
