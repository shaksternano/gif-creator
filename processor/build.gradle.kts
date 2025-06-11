import com.varabyte.kobweb.gradle.worker.util.configAsKobwebWorker

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kobweb.worker)
}

kotlin {
    configAsKobwebWorker()

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kobweb.worker)
            implementation(libs.kobwebx.serialization.kotlinx)
            implementation(libs.gifkt)
            implementation(project(":util"))
        }
    }
}
