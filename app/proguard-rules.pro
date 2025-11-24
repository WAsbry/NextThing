# ====================================================================================================
# NextThingB1 项目混淆配置
# 用途：保护源码不被反编译，优化APK体积
# ====================================================================================================

# ====================================================================================================
# 基础配置
# ====================================================================================================
# 代码混淆压缩比，在0~7之间，默认为5，一般不需要改
-optimizationpasses 5

# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses

# 不做预校验，preverify是proguard的4个步骤之一
# Android不需要preverify，去掉这一步可以加快混淆速度
-dontpreverify

# 有了verbose这句话，混淆后就会生成映射文件
# 包含有类名->混淆后类名的映射关系
# 然后使用printmapping指定映射文件的名称
-verbose
-printmapping proguardMapping.txt

# 指定混淆时采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不改变
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 保护代码中的Annotation不被混淆，这在JSON实体映射时非常重要
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留行号信息，方便调试（可选，用于生产环境定位问题）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ====================================================================================================
# Android基础组件
# ====================================================================================================
# 保留Activity、Service、BroadcastReceiver、ContentProvider等四大组件
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View

# 保留AndroidX和support库
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# 保留自定义View
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
    *** get*();
}

# 保留Activity中的View及其子类的get/set方法
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留Serializable序列化类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留R文件的静态成员
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ====================================================================================================
# Kotlin相关
# ====================================================================================================
# Kotlin反射
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.jvm.functions.** { *; }

# Kotlin协程
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ====================================================================================================
# Jetpack Compose
# ====================================================================================================
# 保留Compose相关类
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 保留Composable函数
-keep @androidx.compose.runtime.Composable class * { *; }
-keep @androidx.compose.runtime.Composable interface * { *; }
-keep class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 保留Compose UI相关
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.ui.tooling.** { *; }
-dontwarn androidx.compose.ui.tooling.**

# ====================================================================================================
# Hilt/Dagger依赖注入
# ====================================================================================================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }

# 保留@Inject、@Module、@Component等注解的类
-keep @dagger.hilt.* class * { *; }
-keep @javax.inject.* class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.Component class * { *; }

# 保留使用@HiltViewModel注解的ViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# 保留Hilt生成的类
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_Impl { *; }

# ====================================================================================================
# Room数据库
# ====================================================================================================
# 保留Room相关
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-dontwarn androidx.room.**

# 保留Room实体类
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }

# 保留Room DAO
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Dao interface * { *; }

# 保留Room TypeConverter
-keep class * {
    @androidx.room.TypeConverter <methods>;
}

# ====================================================================================================
# 项目数据模型
# ====================================================================================================
# 保留所有domain model类（业务实体类）
-keep class com.example.nextthingb1.domain.model.** { *; }

# 保留所有data entity类（数据库实体类）
-keep class com.example.nextthingb1.data.local.entity.** { *; }

# 保留所有DTO类（网络传输对象）
-keep class com.example.nextthingb1.data.remote.dto.** { *; }

# 保留ViewModel的所有UIState类
-keep class com.example.nextthingb1.presentation.**.ui.** { *; }
-keep class **.*UiState { *; }
-keep class **.*State { *; }

# ====================================================================================================
# Retrofit + OkHttp网络请求
# ====================================================================================================
# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson (如果使用)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }

# ====================================================================================================
# Coil图片加载
# ====================================================================================================
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ====================================================================================================
# WorkManager后台任务
# ====================================================================================================
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-dontwarn androidx.work.**

# 保留Worker类
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# ====================================================================================================
# Google Play Services & Location
# ====================================================================================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# 高德地图定位SDK
-keep class com.amap.api.** { *; }
-keep class com.amap.api.location.** { *; }
-keep class com.loc.** { *; }
-dontwarn com.amap.api.**

# ====================================================================================================
# Timber日志库
# ====================================================================================================
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# ====================================================================================================
# Material Design 3
# ====================================================================================================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ====================================================================================================
# Navigation组件
# ====================================================================================================
-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ====================================================================================================
# 移除日志（可选，用于release版本）
# ====================================================================================================
# 移除所有Log.v、Log.d、Log.i日志，保留Log.w和Log.e
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# 移除Timber的debug日志
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ====================================================================================================
# 其他常用库
# ====================================================================================================
# Kotlin Coroutines Flow
-keep class kotlinx.coroutines.flow.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ====================================================================================================
# 额外的代码保护措施
# ====================================================================================================
# 禁止反射访问私有成员（增强安全性）
-keepclassmembers class * {
    !private <fields>;
    !private <methods>;
}

# 混淆包名
-repackageclasses ''

# 允许ProGuard使用激进的优化
-allowaccessmodification
