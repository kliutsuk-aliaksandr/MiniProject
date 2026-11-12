pluginManagement {
    // Kotlin 2.4.x requires R8 9.1.29 or newer for Kotlin metadata rewriting.
    // Override the older R8 bundled with AGP 8.13.2 while retaining the existing toolchain.
    buildscript {
        repositories {
            google()
            mavenCentral()
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
