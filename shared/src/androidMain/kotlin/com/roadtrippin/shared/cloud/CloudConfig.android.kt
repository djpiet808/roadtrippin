package com.roadtrippin.shared.cloud

import android.content.Context
import android.content.pm.PackageManager

actual object CloudConfig {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    private fun metadata(name: String): String {
        val appContext = context ?: return ""
        val info = appContext.packageManager.getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
        return info.metaData?.getString(name).orEmpty()
    }

    actual val supabaseUrl: String get() = metadata("com.roadtrippin.SUPABASE_URL")
    actual val supabasePublishableKey: String get() = metadata("com.roadtrippin.SUPABASE_PUBLISHABLE_KEY")
}
