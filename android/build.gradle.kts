plugins {
    id("com.android.application")
}

android {
    namespace = "com.cafeyokai.tennis"
    compileSdk = 36
    // Installed platform is android-36.1; plain 36 would trigger an SDK download.
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.cafeyokai.tennis"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].jniLibs.srcDirs("libs")
}

val natives: Configuration by configurations.creating

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.14.2")

    natives("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-x86_64")
}

// Extract LibGDX native .so files into per-ABI jniLibs folders before packaging.
tasks.register("copyAndroidNatives") {
    doFirst {
        natives.files.forEach { jar ->
            val abi = jar.nameWithoutExtension.substringAfterLast("natives-")
            val outputDir = file("libs/$abi")
            outputDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outputDir)
                include("*.so")
            }
        }
    }
}

tasks.matching { it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyAndroidNatives")
}
