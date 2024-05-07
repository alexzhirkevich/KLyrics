
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.compose)
    alias(libs.plugins.composecompiler)
}


kotlin {

    jvm()

    sourceSets {
        val jvmMain by getting {
            kotlin.srcDirs("src/main/kotlin")
            dependencies {
                implementation(project(":example:shared"))

                implementation(compose.desktop.currentOs)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(compose.ui)
                api(compose.materialIconsExtended)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "Main_desktopKt"
    }
}