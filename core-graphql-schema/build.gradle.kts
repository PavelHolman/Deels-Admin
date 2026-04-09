import com.apollographql.apollo.annotations.ApolloExperimental
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.apollo)
}

group = "cz.deels.admin.core.graphql.schema"

kotlin {
    android {
        namespace = "cz.deels.admin.core.graphql.schema"
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
            implementation(libs.bundles.apollo)
            implementation(libs.bundles.kotlinx)
        }
    }
}

apollo {
    service("DeelsAPI") {
        packageName = "cz.deels.admin.core.graphql"
        generateApolloMetadata = true
        generateSourcesDuringGradleSync = true

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
            "cz.deels.admin.core.graphql.schema.Apollo.InstantAdapter"
        )
        mapScalar(
            "Instant",
            "kotlin.time.Instant",
            "cz.deels.admin.core.graphql.schema.Apollo.InstantAdapter"
        )
        mapScalar(
            "Time",
            "kotlinx.datetime.LocalTime",
            "com.apollographql.adapter.datetime.KotlinxLocalTimeAdapter"
        )
        mapScalar(
            "BigFloat",
            "kotlin.Double",
            "cz.deels.admin.core.graphql.schema.Apollo.BigDecimalAdapter"
        )
        mapScalar(
            "Cursor",
            "cz.deels.admin.core.graphql.schema.Cursor",
            "cz.deels.admin.core.graphql.schema.Apollo.CursorAdapter"
        )
    }
}
