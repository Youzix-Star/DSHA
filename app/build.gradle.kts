plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.youzixstar.dsha"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "com.youzixstar.dsha"
        // 与 NekoPlus 对齐：液态玻璃底栏依赖 miuix-blur，其 AAR 声明 minSdk 33
        minSdk = 33
        targetSdk = 37
        // 版本由 CI 依据 git tag 注入。此前这里写死成 0.1.0，导致装了 v0.3.0
        // 的应用在关于页仍自称 0.1.0；现在没有 tag 时会明确显示 -dev 而不是假版本号。
        val injectedName = System.getenv("DSHA_VERSION_NAME")
            ?: (project.findProperty("DSHA_VERSION_NAME") as String?)
        val injectedCode = System.getenv("DSHA_VERSION_CODE")
            ?: (project.findProperty("DSHA_VERSION_CODE") as String?)
        versionCode = injectedCode?.toIntOrNull() ?: 1
        versionName = injectedName ?: "0.0.0-dev"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.miuix.core)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.blur)
}
