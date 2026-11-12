pluginManagement {
    // Kotlin 2.4.x requires R8 9.1.29 or newer for Kotlin metadata rewriting.
    // Override the older R8 bundled with AGP 8.13.2 while retaining the existing toolchain.
    buildscript {
        repositories {
            google()
            mavenCentral()
            // R8's official Maven mirror also publishes stable and -dev prebuilt versions.
            maven {
                url = uri("https://storage.googleapis.com/r8-releases/raw")
            }
        }
        dependencies {
            classpath("com.android.tools:r8:9.1.29")
        }
    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MiniProject"
include(":app")
