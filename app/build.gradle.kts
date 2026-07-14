import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = "com.hipka.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hipka.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

//        val properties = java.util.Properties()
//        val propertiesFile = rootProject.file("local.properties")
//        if (propertiesFile.exists()) {
//            properties.load(propertiesFile.inputStream())
//        }
//        buildConfigField("String", "SUPABASE_URL", properties.getProperty("supabase.url", "\"\""))
//        buildConfigField("String", "SUPABASE_KEY", properties.getProperty("supabase.key", "\"\""))

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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ۱. کتابخانه‌های پایه اندروید و لایف‌سایکل (از کاتالوگ پروژه شما)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ۲. سیستم طراحی کامپوز و متریال ۳ (از کاتالوگ پروژه شما)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // این دو مورد از قبل در کاتالوگ پروژه شما تعریف شده بود و نیازی به کد تکراری نبود:
    implementation(libs.androidx.material.icons.extended) // آیکون‌های منوی پایین
    implementation(libs.androidx.navigation.compose) // سیستم ناوبری (NavGraph)

    // ۳. ابزارهای دیتابیس محلی و تزریق وابستگی (Hilt & Room)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ۴. حافظه ذخیره‌سازی و مدیریت کارهای همزمان
    implementation(libs.androidx.datastore.preferences) // دیتااستور تنظیمات تم و زبان
    implementation(libs.kotlinx.coroutines.android)

    // ۵. کتابخانه‌های مالتی‌مدیا و ارتباط با سرور (پخش موزیک و وب‌سوکت)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    // ۶. ابزارهای تست و دیباگ برنامه
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // لایه واسط هماهنگی کامپوز با سیستم چندزبانگی بومی اندروید (پایه سیستم XML)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // تبدیل اطلاعات شبکه و سرور به کدهای کاتلین (پشتیبانی از DTOهای جدول چت و پیام‌ها)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    // وابستگی‌های سوپابیس برای چت آنلاین و اتصال به دیتابیس
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.2")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.5.2")
    // موتور ارتباطی شبکه برای کارکرد صحیح وب‌ساکت و سوپابیس
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
}