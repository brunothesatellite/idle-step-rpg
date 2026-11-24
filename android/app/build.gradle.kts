plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val keystorePropertiesFile = rootProject.file("../.keystore/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
// Keystore debug du dépôt : garde la compatibilité avec l'APK debug déjà installé.
val debugKeystoreFile = rootProject.file("../.keystore/debug.keystore")

android {
    namespace = "com.brunothesatellite.idlesteprpg"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.brunothesatellite.idlesteprpg"
        minSdk = 34
        targetSdk = 36
        versionCode = 53
        versionName = "0.20.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
        manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_round"
    }

    flavorDimensions += "distribution"

    productFlavors {
        // Distribution GitHub : APK signé avec la clé release, mise à jour
        // intégrée depuis les releases GitHub.
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "PLAY_DISTRIBUTION", "false")
        }
        // Distribution Play : aucun installateur d'APK, aucune vérification de
        // mise à jour GitHub. Play gère les mises à jour.
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "PLAY_DISTRIBUTION", "true")
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file("../.keystore/" + keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
        // Utilise le keystore debug du dépôt s'il est présent. Sinon, Gradle
        // reprend la clé debug Android standard.
        if (debugKeystoreFile.exists()) {
            create("debugProject") {
                storeFile = debugKeystoreFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("debugProject")?.let { signingConfig = it }
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Copie de test installable à côté de la version de production.
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_test"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_test_round"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "wav"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20231013")
}

// Compteurs du Codex et version de l'application injectés dans le manuel au
// build : le HTML publié et l'asset embarqué restent alignés sur le projet,
// sans script externe.
val manualTemplate = rootProject.file("../manuel/index.template.html")
val manualTemplateEn = rootProject.file("../manuel/index.en.template.html")
val manualRepo = rootProject.file("../manuel/index.html")
val manualRepoEn = rootProject.file("../manuel/en/index.html")
val manualAsset = file("src/main/assets/manuel/index.html")
val manualAssetEn = file("src/main/assets/manuel/en/index.html")
val manualImagesEn = rootProject.file("../manuel/en/images")
val manualAssetImagesEn = file("src/main/assets/manuel/en/images")
val modelSource = file(
    "src/main/java/com/brunothesatellite/idlesteprpg/engine/Model.kt"
)
val appVersionName = android.defaultConfig.versionName ?: "0.0.0"

val generateManual by tasks.registering {
    inputs.file(modelSource)
    inputs.file(manualTemplate)
    inputs.file(manualTemplateEn)
    inputs.dir(manualImagesEn)
    inputs.property("versionName", appVersionName)
    outputs.file(manualRepo)
    outputs.file(manualRepoEn)
    outputs.file(manualAsset)
    outputs.file(manualAssetEn)
    outputs.dir(manualAssetImagesEn)

    doLast {
        val model = modelSource.readText()
        val mobCount = Regex("MobTemplate\\(\"([^\"]+)\"")
            .findAll(model)
            .map { it.groupValues[1] }
            .toSet()
            .size
        val bossCount = Regex("BossTemplate\\(\"([^\"]+)\"")
            .findAll(model)
            .map { it.groupValues[1] }
            .toSet()
            .size
        val setsBlock = Regex("val ITEM_SETS = listOf\\(([\\s\\S]*?)\\)")
            .find(model)
            ?.groupValues
            ?.get(1)
            .orEmpty()
        val setCount = Regex("\"([^\"]+)\"").findAll(setsBlock).count()
        fun render(template: File): String = template.readText()
            .replace("{{MOB_COUNT}}", mobCount.toString())
            .replace("{{BOSS_COUNT}}", bossCount.toString())
            .replace("{{SET_COUNT}}", setCount.toString())
            .replace("{{APP_VERSION}}", appVersionName)

        val html = render(manualTemplate)
        manualRepo.writeText(html)
        manualAsset.parentFile.mkdirs()
        manualAsset.writeText(html)

        val htmlEn = render(manualTemplateEn)
        manualRepoEn.parentFile.mkdirs()
        manualRepoEn.writeText(htmlEn)
        manualAssetEn.parentFile.mkdirs()
        manualAssetEn.writeText(htmlEn)

        manualAssetImagesEn.deleteRecursively()
        manualImagesEn.copyRecursively(manualAssetImagesEn, overwrite = true)
    }
}

tasks.named("preBuild") {
    dependsOn(generateManual)
}
