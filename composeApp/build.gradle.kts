import com.apollographql.apollo.annotations.ApolloExperimental
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.apollo)
}

group = "cz.deels.admin.app"

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
        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(libs.bundles.compose)
            implementation(libs.bundles.apollo)
            implementation(libs.bundles.arrow)
            implementation(libs.bundles.kotlinx)
            implementation(libs.oidc.appsupport)
        }
        iosMain.dependencies {}
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

apollo {
    service("DeelsAPI") {
        packageName = "cz.deels.graphql"

        @OptIn(ApolloExperimental::class)
        plugin("com.apollographql.cache:normalized-cache-apollo-compiler-plugin:${libs.versions.apollo.cache.get()}") {
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
