import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val roadtrippinLocalProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun roadtrippinSecret(name: String): String =
    roadtrippinLocalProperties.getProperty(name)
        ?: providers.environmentVariable(name).orNull
        ?: ""

android {
    namespace = "com.roadtrippin.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.roadtrippin.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 10
        versionName = "0.1.0-beta10"
        manifestPlaceholders["SUPABASE_URL"] = roadtrippinSecret("SUPABASE_URL")
        manifestPlaceholders["SUPABASE_PUBLISHABLE_KEY"] = roadtrippinSecret("SUPABASE_PUBLISHABLE_KEY")
        manifestPlaceholders["SENTRY_DSN"] = roadtrippinSecret("SENTRY_DSN")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/INDEX.LIST"
        )
    }
}

dependencies {
    implementation(project(":shared"))
}
