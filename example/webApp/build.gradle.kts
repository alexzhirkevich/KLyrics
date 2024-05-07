plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.composecompiler)
}

kotlin {
    js(IR){
        browser()
        binaries.executable()
    }

    wasmJs(){
        browser()
        binaries.executable()
    }
    sourceSets {

        commonMain.dependencies {
            implementation(compose.ui)
            implementation(project(":example:shared"))
        }
    }
}

compose.experimental.web.application{}
