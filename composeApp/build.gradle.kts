import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application")
    id("app.cash.sqldelight")
}
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework { baseName = "ComposeApp"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("app.cash.sqldelight:runtime:2.1.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("app.cash.sqldelight:android-driver:2.1.0")
        }
        iosMain.dependencies { implementation("app.cash.sqldelight:native-driver:2.1.0") }
        val desktopTest by getting {
            dependencies { implementation("app.cash.sqldelight:sqlite-driver:2.1.0") }
        }
    }
}
android {
    namespace = "com.shutupandlog"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.shutupandlog"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
sqldelight {
    databases {
        create("LogDatabase") {
            packageName.set("com.shutupandlog.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

// Keep explicit schema generation ordered before verification when both are requested.
tasks.matching { it.name == "verifyCommonMainLogDatabaseMigration" }.configureEach {
    mustRunAfter("generateCommonMainLogDatabaseSchema")
}
