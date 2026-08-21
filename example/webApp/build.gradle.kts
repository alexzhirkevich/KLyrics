
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.composecompiler)
}

kotlin {

    applyDefaultHierarchyTemplate()

    js {
        browser()
        binaries.executable()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {

        webMain.dependencies {
            implementation(compose.ui)
            implementation(project(":example:shared"))
        }
    }
}
