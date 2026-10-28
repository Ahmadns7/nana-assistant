pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NaNa"

include(":app")
include(":core:wakeword")
include(":core:stt")
include(":core:tts")
include(":core:llm")
include(":core:accessibility")
include(":core:media")
include(":core:agent")
include(":core:storage")
include(":core:phone")
include(":core:macros")
include(":core:ocr")
include(":core:notifications")
include(":core:settings")
include(":shared:common")
include(":shared:logging")
include(":ui:main")
include(":ui:settings")
// tests:integration excluded temporarily for build compatibility