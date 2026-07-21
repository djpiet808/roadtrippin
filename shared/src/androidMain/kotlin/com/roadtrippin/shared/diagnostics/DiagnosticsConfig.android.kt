package com.roadtrippin.shared.diagnostics

import android.content.Context
import android.content.pm.PackageManager

actual object DiagnosticsConfig {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual val dsn: String
        get() {
            val appContext = context ?: return ""
            val info = appContext.packageManager.getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
            return info.metaData?.getString("com.roadtrippin.SENTRY_DSN").orEmpty()
        }
}
