import java.util.Properties
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// ۱. خواندن ایمن اطلاعات سوپابیس با کلاس استاندارد Properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun config(name: String, env: String, fallback: String = "") =
    providers.environmentVariable(env).orNull ?: localProperties.getProperty(name, fallback)
fun quoted(value: String) = "\"" + value.trim().removeSurrounding("\"")
    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "").replace("\r", "") + "\""
val supabaseUrl = config("supabase.url", "SUPABASE_URL", "https://example.supabase.co/")
val supabaseKey = config("supabase.key", "SUPABASE_KEY", "unconfigured-public-key").trim().removeSurrounding("\"")
require(supabaseUrl.startsWith("https://")) { "Supabase URL must use HTTPS" }
require(!supabaseKey.startsWith("sb_secret_")) { "Never embed a Supabase secret key" }
val keyPayload = supabaseKey.split('.').getOrNull(1)?.let {
    runCatching { String(Base64.getUrlDecoder().decode(it)) }.getOrDefault("")
}.orEmpty()
require(!keyPayload.contains("service_role")) { "Never embed a service-role JWT" }
val finalUrl = quoted(supabaseUrl.trimEnd('/') + "/")
val finalKey = quoted(supabaseKey)
val legacyDemoAuth = config("hipka.legacyDemoAuth", "HIPKA_LEGACY_DEMO_AUTH", "false").toBoolean()

android {
    namespace = "com.hipka.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hipka.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ۲. ارسال آدرس و کلید سوپابیس به BuildConfig
        buildConfigField("String", "SUPABASE_URL", finalUrl)
        buildConfigField("String", "SUPABASE_KEY", finalKey)
        buildConfigField("boolean", "LEGACY_DEMO_AUTH", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "LEGACY_DEMO_AUTH", legacyDemoAuth.toString())
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ۳. ارتقاء به Java 17 جهت هماهنگی با SDK 35، کامپایلر K2 و Hilt
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

    // دانلود آفلاین در پس‌زمینه با WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

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

    // ۷. لایه واسط هماهنگی کامپوز با سیستم چندزبانگی بومی اندروید
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // ۸. سریالایز و تبدیل دیتا (DTO چت و پیام‌ها)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // ۹. کلاینت سوپابیس و شبکه Ktor
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.2")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.5.2")
    implementation("io.ktor:ktor-client-okhttp:2.3.13")
    implementation(platform("io.ktor:ktor-bom:2.3.13"))
}