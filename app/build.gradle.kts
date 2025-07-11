plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("kotlin-kapt")
}

android {
    namespace = "com.wasbry.nextthing"
    compileSdk = 35

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }


    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.15"  // 指定与 Kotlin 1.9.20 兼容的版本
//    }
}

dependencies {
    // AndroidX 基础库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.android.material)

    // Compose BOM（管理所有 Compose 依赖版本）
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Compose 核心库
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)  // 布局库
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)  // Material Design 3 支持

    // 架构组件
    implementation(libs.androidx.lifecycle.runtime.ktx)  // Lifecycle KTX 扩展
    implementation(libs.androidx.lifecycle.runtime.compose.android)  // Compose 生命周期集成

    // 导航组件
    implementation(libs.androidx.navigation.compose)  // Compose 导航库

    // Activity 与 Compose 集成
    implementation(libs.androidx.activity.compose)  // Activity 与 Compose 绑定

    // Room 数据库
    implementation(libs.androidx.room.ktx)  // Room KTX 扩展
    implementation(libs.androidx.room.runtime)  // Room 运行时库
    kapt(libs.androidx.room.compiler)  // Room 注解处理器

    // 测试框架
    androidTestImplementation(libs.androidx.espresso.core)  // Espresso 测试核心库

    // Glide 图片加载库
    implementation(libs.glide)
    kapt(libs.glide.compiler)
}