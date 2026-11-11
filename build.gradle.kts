plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.sonarqube") version "7.5.0.8588"
}

val sonarCoverageXml = layout.buildDirectory.file("sonar/coverage.xml")
val sonarUnitCoverageDir = layout.projectDirectory.dir("app/build/reports/coverage/test/debug").asFile

tasks.register("prepareSonarCoverageReport") {
    dependsOn(":app:testDebugUnitTest", ":app:createDebugUnitTestCoverageReport")
    doLast {
        val report = sonarUnitCoverageDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .maxByOrNull { it.length() }
            ?: throw GradleException(
                "Android Gradle Plugin did not produce a JaCoCo XML report under $sonarUnitCoverageDir"
            )
        val destination = sonarCoverageXml.get().asFile
        destination.parentFile.mkdirs()
        report.copyTo(destination, overwrite = true)
    }
}

sonar {
    properties {
        property("sonar.projectKey", System.getenv("SONAR_PROJECT_KEY") ?: "kliutsuk-aliaksandr_MiniProject")
        property("sonar.projectName", "MiniProject")
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "https://sonarcloud.io")
        property("sonar.sources", "app/src/main/java")
        property("sonar.tests", "app/src/test/java,app/src/androidTest/java")
        property("sonar.junit.reportPaths", "app/build/test-results/testDebugUnitTest")
        property("sonar.coverage.jacoco.xmlReportPaths", sonarCoverageXml.get().asFile.absolutePath)
        property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.xml")
        property("sonar.sourceEncoding", "UTF-8")
    }
}

tasks.named("sonar") {
    dependsOn("prepareSonarCoverageReport")
}
