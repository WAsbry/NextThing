pluginManagement {
    repositories {
        // 官方源优先（代理已配置）
        gradlePluginPortal()
        google()
        mavenCentral()
        // 国内镜像兜底
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://mirrors.cloud.tencent.com/gradle") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 国内镜像优先
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 官方源兜底
        google()
        mavenCentral()
        // JitPack（TarsosDSP 等开源库）
        maven { url = uri("https://jitpack.io") }
        // TarsosDSP 官方仓库
        maven { url = uri("https://mvn.0110.be/releases") }
    }
}

rootProject.name = "NextThingB1"
include(":app")
 