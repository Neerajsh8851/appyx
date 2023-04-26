plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

kotlin {
    android()
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvmTarget.get()
        }
    }
//    js(IR) {
//        // Adding moduleName as a workaround for this issue: https://youtrack.jetbrains.com/issue/KT-51942
//        moduleName = "appyx-components-backstack-commons"
//        browser()
//    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":appyx-interactions:common"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting
        val desktopMain by getting
//        val jsMain by getting
    }
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
}

android {
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":ksp:mutable-ui-processor"))
    add("kspAndroid", project(":ksp:mutable-ui-processor"))
    add("kspDesktop", project(":ksp:mutable-ui-processor"))
//    add("kspJs", project(":ksp:mutable-ui-processor"))
}
