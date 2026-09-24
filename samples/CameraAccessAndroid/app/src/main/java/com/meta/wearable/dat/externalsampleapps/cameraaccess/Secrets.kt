// Build defaults for CI. The upstream repository gitignores Secrets.kt and
// ships only Secrets.kt.example, so a fresh clone does not compile at all.
// Values here are deliberately public: the app points at the hosted gateway,
// and the personal access token is entered in-app on first launch (see
// SettingsManager.isGatewayConfigured). Nothing secret belongs in this file.

package com.meta.wearable.dat.externalsampleapps.cameraaccess

object Secrets {
    const val gatewayBaseUrl = "https://api.visionagents.app"
    const val gatewayToken = ""
}
