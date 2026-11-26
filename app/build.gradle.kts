plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    // Mide qué porcentaje del código cubren las pruebas unitarias
    alias(libs.plugins.kover)
}

android {
    namespace = "cl.duoc.nutriexpert"
    compileSdk = 36

    defaultConfig {
        applicationId = "cl.duoc.nutriexpert"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
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
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Consumo de los microservicios (Retrofit + Gson)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
}

// Cobertura de la lógica (Repository, ViewModel, dominio). Se excluye lo que no es lógica
// o que solo se prueba en un dispositivo: pantallas Compose, tema, Activity, Room y Retrofit.
// Reporte: ./gradlew koverHtmlReportDebug → app/build/reports/kover/htmlDebug/index.html
kover {
    reports {
        filters {
            excludes {
                packages("cl.duoc.nutriexpert.ui", "cl.duoc.nutriexpert.ui.theme")
                classes(
                    "cl.duoc.nutriexpert.MainActivity*",
                    "cl.duoc.nutriexpert.ComposableSingletons*",
                    "*_Impl", "*_Impl\$*",
                    "cl.duoc.nutriexpert.data.AppDatabase*",
                    "cl.duoc.nutriexpert.data.remote.RetrofitInstance*",
                    "cl.duoc.nutriexpert.data.remote.Conectividad*",
                    "cl.duoc.nutriexpert.BuildConfig"
                )
                annotatedBy("androidx.room.Dao", "androidx.room.Entity", "androidx.compose.runtime.Composable")
            }
        }
    }
}
