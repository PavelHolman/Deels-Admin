import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.apollo)
}

kotlin {
    explicitApi()

    android {
        namespace = "cz.deels.admin.feature.orders.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.featureOrders.domain)
            implementation(projects.coreGraphqlSchema)
            implementation(libs.bundles.apollo)
            implementation(libs.bundles.arrow)
            implementation(libs.bundles.kotlinx)
        }
    }
}

apollo {
    service("DeelsAPI") {
        packageName = "cz.deels.admin.feature.orders.data.graphql"
        generateAsInternal = true
        generateSourcesDuringGradleSync = true
        dependsOn(projects.coreGraphqlSchema)
    }
}
