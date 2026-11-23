pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    // CI runners share an egress IP, and repo.maven.apache.org answers HTTP 429 for the large
    // Robolectric native runtime. Google mirrors the same Maven Central artifacts from another
    // host, so Robolectric resolves there instead of failing the lint and test jobs.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      content { includeGroupByRegex("org\\.robolectric.*") }
    }
    mavenCentral()
  }
}

rootProject.name = "Barakah"

include(":app")
