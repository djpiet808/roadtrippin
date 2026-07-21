package com.roadtrippin.shared.cloud

import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

object IosAuthBridge {
    fun handleDeepLink(url: String) {
        val nativeUrl = NSURL.URLWithString(url) ?: return
        CloudServices.client?.handleDeeplinks(nativeUrl)
    }
}
