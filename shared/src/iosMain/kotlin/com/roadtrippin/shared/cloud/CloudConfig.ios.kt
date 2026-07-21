package com.roadtrippin.shared.cloud

import platform.Foundation.NSBundle

actual object CloudConfig {
    actual val supabaseUrl: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_URL") as? String ?: ""
    actual val supabasePublishableKey: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_PUBLISHABLE_KEY") as? String ?: ""
}
