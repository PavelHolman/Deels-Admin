import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.apollo)
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect")
    }

    android {
        namespace = "cz.deels.admin.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {}
        commonMain.dependencies {
            implementation(projects.coreGraphqlSchema)
            implementation(projects.coreOidc)
            implementation(libs.bundles.compose)
            implementation(libs.bundles.apollo)
            implementation(libs.bundles.arrow)
            implementation(libs.bundles.kotlinx)
        }
        iosMain.dependencies {}
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

apollo {
    service("DeelsAPI") {
        packageName = "cz.deels.admin.app.graphql"
        generateAsInternal = true
        generateSourcesDuringGradleSync = true
        dependsOn(projects.coreGraphqlSchema)
    }
}
