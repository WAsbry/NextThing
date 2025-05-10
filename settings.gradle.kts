pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://mirrors.cloud.tencent.com/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://developer.huawei.com/repo/") }
        maven{ url = uri("https://maven.aliyun.com/nexus/content/groups/public/")  }
        maven{ url = uri("https://maven.aliyun.com/nexus/content/repositories/jcenter")  }

        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/maven") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://developer.huawei.com/repo/") }

        maven{ url = uri("https://maven.aliyun.com/nexus/content/groups/public/")  }
        maven{ url = uri("https://maven.aliyun.com/nexus/content/repositories/jcenter")  }

        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}


rootProject.name = "NextThing"
include(":app")
