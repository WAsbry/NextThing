import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("kotlin-parcelize")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

val asrBackend = providers.gradleProperty("asrBackend")
    .orElse("npu")
    .get()
    .lowercase()
check(asrBackend in setOf("cpu", "npu")) {
    "Unsupported -PasrBackend=$asrBackend. Expected cpu or npu."
}

val backendBaseUrl = localProperties.getProperty("BACKEND_BASE_URL")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "https://api.qianqian.chat/"
check(backendBaseUrl.endsWith('/')) {
    "BACKEND_BASE_URL must end with '/'."
}

val releaseKeystorePath = localProperties.getProperty("RELEASE_STORE_FILE")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "release.keystore"
val releaseKeystoreFile = file(releaseKeystorePath)
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD").orEmpty()
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS").orEmpty()
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD").orEmpty()
val hasReleaseSigning = releaseKeystoreFile.isFile &&
    releaseStorePassword.isNotBlank() &&
    releaseKeyAlias.isNotBlank() &&
    releaseKeyPassword.isNotBlank()

android {
    namespace = "com.nextthing.app"
    compileSdk = 34
    // 真机仪器测试使用独立 smoke 包（com.nextthing.app.smoke），不覆盖用户安装的 release 包。
    testBuildType = "smoke"

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.nextthing.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += "arm64-v8a"
        }

        buildConfigField("String", "ASR_BACKEND", "\"$asrBackend\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")

        manifestPlaceholders["AMAP_API_KEY"] = localProperties.getProperty("AMAP_API_KEY") ?: ""
    }

    sourceSets {
        getByName("main") {
            if (asrBackend == "cpu") {
                assets.srcDir(rootProject.file("local-artifacts/original-cpu-sensevoice"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("smoke") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".smoke"
            versionNameSuffix = "-smoke"
            isDebuggable = true
            matchingFallbacks += listOf("debug")
            buildConfigField(
                "String",
                "BACKEND_BASE_URL",
                "\"http://127.0.0.1:18080/\""
            )
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val buildType = variant.buildType.name
                if (buildType == "release") {
                    output.outputFileName = if (hasReleaseSigning) {
                        "NextThing-release.apk"
                    } else {
                        "NextThing-release-unsigned.apk"
                    }
                }
            }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    aaptOptions {
        noCompress("tflite", "onnx")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit/OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // WorkManager + Hilt integration
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Glance Widget
    implementation("androidx.glance:glance:1.0.0")
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // LiteRT（端侧 AI 推理引擎）
    implementation(libs.litert)
    implementation(libs.litert.support)
    implementation(libs.litert.gpu)

    // Qualcomm Hexagon NPU via QNN HTP backend
    implementation("com.qualcomm.qti:qnn-runtime:2.34.0")
    implementation("com.qualcomm.qti:qnn-litert-delegate:2.34.0")

    // TarsosDSP（端侧音频处理，MFCC 提取）
    implementation("be.tarsos.dsp:core:2.5")

    // App Startup
    implementation("androidx.startup:startup-runtime:1.1.1")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // 高德 3D 地图合包，包含地图、定位和搜索 SDK；不要与旧 map2d/独立定位搜索依赖并存。
    implementation("com.amap.api:navi-3dmap-location-search:11.2.000_3dmap11.2.000_loc11.2.000_sea9.8.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.48.1")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
