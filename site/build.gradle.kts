import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kobweb.application)
}

kobweb {
    app {
        index {
            description.set("GIF Creator")
        }
    }
}

kotlin {
    configAsKobwebApplication("gifcreator")

    sourceSets {
        jsMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.runtime)
            implementation(libs.compose.html.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.silk)
            implementation(libs.silk.icons.fa)
            implementation(libs.gifkt)
            implementation(project(":worker"))
            implementation(project(":shared"))
        }
    }
}
