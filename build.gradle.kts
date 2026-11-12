plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", System.getenv("SONAR_PROJECT_KEY") ?: "kliutsuk-aliaksandr_MiniProject")
        property("sonar.projectName", "MiniProject")
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "https://sonarcloud.io")
        property("sonar.sources", "app/src/main/java")
        property("sonar.tests", "app/src/test/java,app/src/androidTest/java")
        property("sonar.junit.reportPaths", "app/build/test-results/testDebugUnitTest")
        property("sonar.coverage.jacoco.xmlReportPaths", "app/build/reports/jacoco/debugUnitTest/report.xml")
        property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.xml")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.qualitygate.wait", "true")
        property("sonar.qualitygate.timeout", "600")
        System.getenv("SONAR_ORGANIZATION")
            ?.takeIf { it.isNotBlank() }
            ?.let { property("sonar.organization", it) }
    }
}

tasks.named("sonar") {
    dependsOn(":app:jacocoDebugUnitTestReport")
}
