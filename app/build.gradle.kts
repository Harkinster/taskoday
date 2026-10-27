plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.example.taskoday"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.harkinster.taskoday"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.0-beta07"
        // Public certificate pinned from the existing signed beta03 APK and confirmed against
        // the configured external keystore. Override only when deliberately rotating the key.
        buildConfigField("String", "UPDATE_SIGNER_SHA256", "\"${providers.gradleProperty("taskodayReleaseSignerSha256").orElse("4783e3e7f20c4bfec5e6b22fb0e3cfcaeb0dfddca3ebc380929c7fb1243a666b").get()}\"")
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"https://harkserv.ddns.net/taskoday/version.json\"")
        buildConfigField("String", "UPDATE_SERVER_HOST", "\"harkserv.ddns.net\"")
        buildConfigField("int", "UPDATE_SERVER_PORT", "443")
        buildConfigField("long", "UPDATE_CHECK_INTERVAL_MS", "86400000L")
        buildConfigField("long", "UPDATE_FAILURE_RETRY_INTERVAL_MS", "1800000L")
        buildConfigField("String", "TASKODAY_BASE_URL", "\"https://harkserv.ddns.net/taskoday-api/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val betaStoreFile = providers.gradleProperty("taskodayKeystorePath").orElse(providers.environmentVariable("TASKODAY_KEYSTORE_PATH")).orNull
    val betaStorePassword = providers.gradleProperty("taskodayKeystorePassword").orElse(providers.environmentVariable("TASKODAY_KEYSTORE_PASSWORD")).orNull
    val betaKeyAlias = providers.gradleProperty("taskodayKeyAlias").orElse(providers.environmentVariable("TASKODAY_KEY_ALIAS")).orNull
    val betaKeyPassword = providers.gradleProperty("taskodayKeyPassword").orElse(providers.environmentVariable("TASKODAY_KEY_PASSWORD")).orNull
    val betaSigningReady = listOf(betaStoreFile, betaStorePassword, betaKeyAlias, betaKeyPassword).all { !it.isNullOrBlank() }

    signingConfigs {
        create("beta") {
            if (betaSigningReady) {
                storeFile = file(requireNotNull(betaStoreFile))
                storePassword = requireNotNull(betaStorePassword)
                keyAlias = requireNotNull(betaKeyAlias)
                keyPassword = requireNotNull(betaKeyPassword)
            }
        }
    }

    if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) || it.contains("Qa", ignoreCase = true) } && !betaSigningReady) {
        throw GradleException(
            "Signature Release/QA absente. Fournir taskodayKeystorePath/taskodayKeystorePassword/taskodayKeyAlias/taskodayKeyPassword dans gradle.properties utilisateur ou via TASKODAY_*.",
        )
    }

    buildTypes {
        debug {
            // V4 preview/gallery stays isolated from an installed beta package.
            val localFamilyQa = providers.gradleProperty("taskodayLocalFamilyQa").orNull == "true"
            applicationIdSuffix = if (localFamilyQa) ".familyqa" else ".v43gallery"
            if (localFamilyQa) {
                // Explicit opt-in: genuine local API, separate Android storage.
                buildConfigField("String", "TASKODAY_BASE_URL", "\"http://10.0.2.2:18765/\"")
                buildConfigField("String", "UPDATE_MANIFEST_URL", "\"http://10.0.2.2:18765/qa/no-update\"")
                buildConfigField("String", "UPDATE_SERVER_HOST", "\"10.0.2.2\"")
                buildConfigField("int", "UPDATE_SERVER_PORT", "18765")
            }
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("beta")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("qa") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("beta")
            buildConfigField("String", "UPDATE_MANIFEST_URL", "\"https://10.0.2.2:8443/taskoday-test/version.json\"")
            buildConfigField("String", "UPDATE_SERVER_HOST", "\"10.0.2.2\"")
            buildConfigField("int", "UPDATE_SERVER_PORT", "8443")
            buildConfigField("long", "UPDATE_CHECK_INTERVAL_MS", "5000L")
            buildConfigField("long", "UPDATE_FAILURE_RETRY_INTERVAL_MS", "2000L")
            // QA builds deliberately cannot read or write the production family API.
            buildConfigField("String", "TASKODAY_BASE_URL", "\"https://harkserv.ddns.net/taskoday-test-api/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Workaround for current lint/Compose detector crash with Kotlin Analysis API.
        disable +=
            listOf(
                "RememberInComposition",
                "FrequentlyChangingValue",
                "NullSafeMutableLiveData",
            )
    }
}

val taskodayQaVersionCode = providers.gradleProperty("taskodayQaVersionCode").map { it.toInt() }.orElse(6)
val taskodayQaVersionName = providers.gradleProperty("taskodayQaVersionName").orElse("1.0-beta05-qa")

androidComponents {
    onVariants(selector().withBuildType("qa")) { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(taskodayQaVersionCode)
            output.versionName.set(taskodayQaVersionName)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logging)
    implementation(libs.google.gson)
    implementation(libs.androidx.security.crypto)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.google.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    ksp(libs.google.hilt.compiler)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}
