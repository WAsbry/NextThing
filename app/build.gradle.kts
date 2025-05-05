plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt") // 处理 Room 注解
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
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures.compose = true
    composeOptions { kotlinCompilerExtensionVersion = "1.6.0" } // 与 Compose 1.6.0 匹配
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    // ------------------- 1. Compose 核心依赖 -------------------
    implementation(platform("androidx.compose:compose-bom:2023.10.01")) // 推荐使用 BOM 管理版本
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.ui:ui-graphics:1.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0") // 开发预览工具
    implementation("androidx.compose.foundation:foundation:1.6.0") // 基础布局组件
    implementation("androidx.compose.material:material:1.6.0") // Material 2 组件
    implementation("androidx.compose.material3:material3:1.2.0") // Material 3 组件（独立版本）

    // ------------------- 2. AndroidX 基础依赖 -------------------
    implementation("androidx.core:core-ktx:1.12.0") // 最新稳定版（替代旧的 1.9.0）
    implementation("androidx.appcompat:appcompat:1.6.1") // AppCompat 兼容性库
    implementation("com.google.android.material:material:1.10.0") // Material 组件（最新稳定版）

    // ------------------- 3. 架构与数据层依赖 -------------------
    // Room 数据库
    implementation("androidx.room:room-ktx:2.5.2")
    implementation("androidx.room:room-runtime:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2") // Room 注解处理器

    // Lifecycle 组件
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // 最新生命周期库
    implementation("androidx.activity:activity-compose:1.8.0") // Activity 与 Compose 集成

    // ------------------- 4. 功能组件依赖 -------------------
    implementation("androidx.navigation:navigation-compose:2.7.0") // 最新导航组件
    implementation("androidx.recyclerview:recyclerview:1.3.0") // RecyclerView 列表组件

    // ------------------- 5. 开发与调试依赖 -------------------
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.0") // Compose 开发工具
    debugImplementation("androidx.compose.ui:ui-tooling-preview:1.6.0") // 开发预览工具（重复声明可移除）

    // ------------------- 6. 测试依赖 -------------------
    testImplementation("junit:junit:4.13.2") // JUnit4 单元测试
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // Android 扩展测试
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // UI 自动化测试
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0") // Compose UI 测试
}