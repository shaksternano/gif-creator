import com.varabyte.kobweb.gradle.worker.util.configAsKobwebWorker

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kobweb.worker)
}

kotlin {
    configAsKobwebWorker("gif-worker")

    sourceSets {
        jsMain.dependencies {
            implementation(project(":processor"))
            implementation(project(":shared"))
            implementation(libs.kotlin.wrappers.browser)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kobweb.worker)
            implementation(libs.kobwebx.serialization.kotlinx)
            implementation(libs.gifkt)
            implementation(npm("web-demuxer", libs.versions.webDemuxer.get()))
        }
    }
}
