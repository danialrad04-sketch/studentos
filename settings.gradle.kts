pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "com.android.application" && requested.version != null) {
        useModule("com.android.tools.build:gradle:" + requested.version)
      }
    }
  }
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Student OS"

include(":app")
