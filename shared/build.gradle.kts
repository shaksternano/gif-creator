plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    js {
        browser()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kotlin.wrappers.browser)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kobweb.worker)
            implementation(libs.gifkt)
        }
    }
}
