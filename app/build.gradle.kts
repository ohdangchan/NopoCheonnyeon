import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.serialization)
}

// 키는 local.properties·keystore.properties(둘 다 git 제외)에서만 읽는다
fun loadProps(name: String) = Properties().apply {
    val f = rootProject.file(name)
    if (f.exists()) f.inputStream().use { load(it) }
}
val localProps = loadProps("local.properties")
val keystoreProps = loadProps("keystore.properties")

ksp {
    // Room 스키마 기록: 스키마를 바꾸면 버전을 올리고 Migration을 쓴다 (B-13)
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.nopo.cheonnyeon"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nopo.cheonnyeon"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["NAVER_MAP_KEY_ID"] = localProps.getProperty("NAVER_MAP_KEY_ID", "")
        buildConfigField("String", "NAVER_MAP_KEY_ID", "\"${localProps.getProperty("NAVER_MAP_KEY_ID", "")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 값 끝의 공백·줄바꿈이 섞여 들어가도 읽히도록 trim 한다
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile").trim())
                storePassword = keystoreProps.getProperty("storePassword").trim()
                keyAlias = keystoreProps.getProperty("keyAlias").trim()
                keyPassword = keystoreProps.getProperty("keyPassword").trim()
            }
        }
    }

    buildTypes {
        debug {
            // 진단 화면의 좌표→주소 시험에만 쓴다 (B-14)
            buildConfigField("String", "NAVER_MAP_CLIENT_SECRET", "\"${localProps.getProperty("NAVER_MAP_CLIENT_SECRET", "")}\"")
        }
        // debug 블록에 패키지 접미사를 두지 않는다: 네이버 지도·Firebase에 등록한 패키지명과 같아야 한다
        release {
            // 릴리스 APK에는 Client Secret을 넣지 않는다 (B-14)
            buildConfigField("String", "NAVER_MAP_CLIENT_SECRET", "\"\"")
            // keystore.properties가 없으면 디버그 키로 서명한다 (서명 없는 APK는 폰에 설치되지 않음, D-13)
            signingConfig = if (keystoreProps.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            // 폰용 APK 크기 축소(D-15). 에뮬레이터(x86)는 디버그 빌드로 확인한다
            ndk { abiFilters += listOf("arm64-v8a") }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // Robolectric(Room·ViewModel 단위 시험)이 리소스를 읽는다
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.firebase.crashlytics)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.play.services.location)
    implementation(libs.mlkit.text.korean)
    implementation(libs.naver.map)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    debugImplementation(libs.leakcanary)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}