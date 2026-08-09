plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.siemprecontigo.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.siemprecontigo.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // URL del backend (Vercel). Se puede sobrescribir en local.properties:
        // api.base.url=https://tu-proyecto.vercel.app
        val localProps = rootProject.file("local.properties")
        var apiBase = "https://siemprecontigo.app"
        if (localProps.exists()) {
            localProps.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("api.base.url=")) {
                    apiBase = trimmed.removePrefix("api.base.url=").trim()
                }
            }
        }
        buildConfigField("String", "API_BASE_URL", "\"$apiBase\"")
    }

    signingConfigs {
        // Configuración preparada, no ejecutada: el cliente debe crear el keystore
        // y definir las variables de entorno (ver README).
        create("release") {
            val storePath = System.getenv("SIEMPRE_CONTIGO_KEYSTORE")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("SIEMPRE_CONTIGO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIEMPRE_CONTIGO_KEY_ALIAS")
                keyPassword = System.getenv("SIEMPRE_CONTIGO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning?.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
}
