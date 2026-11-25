import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

// Datos de la llave de firma (.jks). Se leen desde android-app/keystore.properties
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "cl.duoc.museo"
    compileSdk = 35

    defaultConfig {
        applicationId = "cl.duoc.museo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Microservicios propios. 10.0.2.2 = localhost de tu PC visto desde el emulador.
        // Si usas un teléfono físico, reemplaza 10.0.2.2 por la IP de tu PC (ej: 192.168.1.50)
        buildConfigField("String", "MUSEOS_URL", "\"http://10.0.2.2:8081/\"")
        buildConfigField("String", "CATALOGO_URL", "\"http://10.0.2.2:8082/\"")
        buildConfigField("String", "PEDIDOS_URL", "\"http://10.0.2.2:8083/\"")
        // API externa (no es nuestra)
        buildConfigField("String", "OPEN_METEO_URL", "\"https://api.open-meteo.com/\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    testOptions {
        // Evita errores si una prueba toca sin querer una clase de Android
        unitTests.isReturnDefaultValues = true
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
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    // Room / SQLite
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // REST
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Imágenes
    implementation(libs.coil.compose)

    // Mapa (OpenStreetMap, sin API key)
    implementation(libs.osmdroid)

    // Cámara + lectura QR
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.zxing.core) // generar QR de prueba

    implementation(libs.kotlinx.coroutines.android)

    // Pruebas unitarias: JUnit 4 + MockK + corrutinas + servidor HTTP falso para la API externa
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

// Cobertura de pruebas con Kover. Se mide solo la LÓGICA (ViewModels, repositorios, utilidades
// y mapeos de red); se excluye la interfaz Compose, Room generado y el código de arranque.
// Reporte:   gradlew koverHtmlReportDebug   → app/build/reports/kover/htmlDebug/index.html
// Verificar: gradlew koverVerifyDebug       (falla si la cobertura baja de 80%)
kover {
    reports {
        filters {
            excludes {
                packages(
                    "cl.duoc.museo.ui.screens",
                    "cl.duoc.museo.ui.components",
                    "cl.duoc.museo.ui.theme",
                    "cl.duoc.museo.ui.navigation",
                    "cl.duoc.museo.data.local",
                    "cl.duoc.museo.notifications"
                )
                classes(
                    "*.BuildConfig",
                    "*ComposableSingletons*",
                    "cl.duoc.museo.MainActivity*",
                    "cl.duoc.museo.MuseoApp*",
                    "cl.duoc.museo.AppContainer*",
                    "cl.duoc.museo.data.SessionManager*",
                    "cl.duoc.museo.data.remote.RetrofitClient*",
                    "cl.duoc.museo.util.QrGenerator*",
                    "cl.duoc.museo.ui.viewmodel.AppViewModelProvider*"
                )
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
