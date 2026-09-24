/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

import java.util.Properties
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.inputStream

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

val localProperties =
    Properties().apply {
      val localPropertiesPath = rootDir.toPath() / "local.properties"
      if (localPropertiesPath.exists()) {
        load(localPropertiesPath.inputStream())
      }
    }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // The three Meta AARs are vendored into app/libs/repo as a flat local
    // repository: CI and local builds never reach GitHub Packages.
    maven {
      url = uri(rootDir.toPath() / "app" / "libs")
    }
    // LiveKit's audioswitch fork is published on JitPack only.
    maven {
      url = uri("https://jitpack.io")
      content { includeGroupByRegex("com\\.github\\..*") }
    }
  }
}

rootProject.name = "CameraAccess"

include(":app")
