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

// ۱. خواندن اطلاعات سوپابیس در بالاترین سطح فایل (بیرون از بلاک defaultConfig برای حل ارور)
val localPropFile = rootProject.file("local.properties")
var supabaseUrl = ""
var supabaseKey = ""

if (localPropFile.exists()) {
    val lines = localPropFile.readLines()
    supabaseUrl = lines.find { it.startsWith("supabase.url=") }?.substringAfter("=") ?: ""
    supabaseKey = lines.find { it.startsWith("supabase.key=") }?.substringAfter("=") ?: ""
}

val finalUrl = "\"${supabaseUrl.trim().removeSurrounding("\"")}\""
val finalKey = "\"${supabaseKey.trim().removeSurrounding("\"")}\""

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

        // ۲. استفاده مستقیم از مقادیر پردازش شده بدون ایجاد تداخل با اسکوپ defaultConfig
        buildConfigField("String", "SUPABASE_URL", finalUrl)
        buildConfigField("String", "SUPABASE_KEY", finalKey)
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
    // ۱. کتابخانه‌های پایه اندروید و لایف‌سایکل
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ۲. سیستم طراحی کامپوز و متریال ۳
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ۳. ابزارهای دیتابیس محلی و تزریق وابستگی (Hilt & Room)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ۴. حافظه ذخیره‌سازی و مدیریت کارهای همزمان
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // ۵. کتابخانه‌های مالتی‌مدیا و ارتباط با سرور (پخش موزیک و وب‌سوکت)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette.ktx)

    // ۶. ابزارهای تست و دیباگ برنامه
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // لایه واسط هماهنگی کامپوز با سیستم چندزبانگی بومی اندروید
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // تبدیل اطلاعات شبکه و سرور به کدهای کاتلین (پشتیبانی از DTOهای جدول چت و پیام‌ها)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // کدهای حیاتی تو برای سوپابیس و کلاینت Ktor
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.2")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.5.2")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")

}