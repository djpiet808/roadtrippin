package com.roadtrippin.shared.diagnostics

import io.sentry.kotlin.multiplatform.Sentry

expect object DiagnosticsConfig {
    val dsn: String
}

object Diagnostics {
    fun initialize() {
        if (DiagnosticsConfig.dsn.isBlank()) return
        Sentry.init { options ->
            options.dsn = DiagnosticsConfig.dsn
            options.release = "roadtrippin@0.1.0-beta01"
            options.environment = "beta"
            options.sendDefaultPii = false
            options.attachScreenshot = false
            options.attachViewHierarchy = false
            options.tracesSampleRate = 0.0
            options.sessionReplay.sessionSampleRate = 0.0
            options.sessionReplay.onErrorSampleRate = 0.0
            options.beforeBreadcrumb = { null }
            options.beforeSend = { event ->
                event.user = null
                event.serverName = null
                event.message = null
                event.breadcrumbs.clear()
                event.tags.keys.filterNot { it in setOf("component", "failure_kind", "platform") }
                    .forEach(event.tags::remove)
                event.exceptions = event.exceptions.map { it.copy(value = null) }.toMutableList()
                event
            }
        }
    }

    fun reportSyncFailure(kind: String) {
        if (DiagnosticsConfig.dsn.isBlank()) return
        Sentry.configureScope { scope ->
            scope.setTag("component", "sync")
            scope.setTag("failure_kind", kind.take(64))
        }
        Sentry.captureMessage("Roadtrippin sync failure")
    }
}
