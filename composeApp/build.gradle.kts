import com.apollographql.apollo.annotations.ApolloExperimental
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
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

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
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
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.apollo.runtime)
            implementation(libs.apollo.normalized.cache)
            implementation(libs.apollo.adapters)
            implementation(libs.arrow.core)
            implementation(libs.arrow.fx.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.oidc.appsupport)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "cz.deels.admin"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cz.deels.admin"
        manifestPlaceholders["oidcRedirectScheme"] = "cz.deels.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

apollo {
    service("DeelsAPI") {
        packageName = "cz.deels.graphql"

        @OptIn(ApolloExperimental::class)
        plugin("com.apollographql.cache:normalized-cache-apollo-compiler-plugin:1.0.0-alpha.6") {
            argument("com.apollographql.cache.packageName", packageName.get())
        }
        introspection {
            endpointUrl = "https://test.deels.cz/graphql"
            schemaFile = file("src/commonMain/graphql/schema.graphqls")
        }
        mapScalarToKotlinLong("BigInt")
        mapScalarToKotlinString("UUID")
        mapScalar(
            "Datetime",
            "kotlin.time.Instant",
            "cz.deels.admin.Apollo.InstantAdapter"
        )
        mapScalar(
            "Instant",
            "kotlin.time.Instant",
            "cz.deels.admin.Apollo.InstantAdapter"
        )
        mapScalar(
            "Time",
            "kotlinx.datetime.LocalTime",
            "com.apollographql.adapter.datetime.KotlinxLocalTimeAdapter"
        )
        mapScalar(
            "BigFloat",
            "kotlin.Double",
            "cz.deels.admin.Apollo.BigDecimalAdapter"
        )
    }
}
