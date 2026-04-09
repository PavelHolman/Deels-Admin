import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.apollo) apply false
}

subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
            freeCompilerArgs.add("-Xexpect-actual-classes")
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
            optIn.add("kotlin.concurrent.atomics.ExperimentalAtomicApi")
            optIn.add("kotlinx.coroutines.FlowPreview")
            optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
            optIn.add("com.apollographql.apollo.annotations.ApolloExperimental")
        }
    }
}
