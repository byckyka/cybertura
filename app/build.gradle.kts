import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Hoja de Google del investigador (opcional). Se lee de local.properties, que no se sube a git:
//   cybertura.sheets.url=https://script.google.com/macros/s/.../exec
//   cybertura.sheets.token=la-clave-que-pusiste-en-el-script
// Sin estas líneas la app compila igual y el botón "Enviar a Google Sheets" no aparece.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun localProp(key: String): String =
    (localProps.getProperty(key) ?: "").trim().replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "io.github.byckyka.cybertura"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.byckyka.cybertura"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SHEETS_URL", "\"${localProp("cybertura.sheets.url")}\"")
        buildConfigField("String", "SHEETS_TOKEN", "\"${localProp("cybertura.sheets.token")}\"")
    }

    buildTypes {
        release {
            // R8 reduce y optimiza el código; se firma con la llave de depuración porque no se publica
            optimization {
                enable = true
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}