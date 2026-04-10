plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

android {
    namespace = "cz.deels.admin.androidapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "cz.deels.admin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["oidcRedirectScheme"] = "cz.deels.app"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.coreOidc)
    implementation(libs.androidx.activity.compose)
    implementation(libs.oidc.appsupport)
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
}
