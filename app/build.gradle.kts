plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    // 添加 kapt 插件用于处理注解
    id("kotlin-kapt")
}

android {
    namespace = "com.wasbry.nextthing"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wasbry.nextthing"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.material:material:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.4.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")

    // Room 相关依赖
    // Room Kotlin 扩展
    implementation("androidx.room:room-ktx:2.5.2")
    // Room 注解处理器，使用 kapt 进行注解处理
    // 如果使用 Room 的测试库，可以添加以下依赖
    testImplementation("androidx.room:room-testing:2.5.2")

    // 添加 RecyclerView 依赖
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // 降级 lifecycle 依赖（关键）
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1") // 兼容 compileSdk 34
    implementation("androidx.activity:activity-compose:1.7.1") // 匹配低版本

    // 明确指定 Compose runtime 版本（避免高版本引入）
    implementation("androidx.compose.runtime:runtime-livedata:1.4.3")
    implementation("androidx.compose.runtime:runtime-saveable:1.4.3")

    // Room 依赖保持一致
    implementation("androidx.room:room-runtime:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")

    // 添加 androidx.lifecycle:lifecycle-runtime-compose-android 依赖
//    implementation("androidx.lifecycle:lifecycle-runtime-compose-android:2.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
}