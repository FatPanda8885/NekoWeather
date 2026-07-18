pluginManagement {
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

rootProject.name = "NekoWeather"
include(":app")

val cubismFramework = file(".cubism-sdk/CubismSdkForJava-5-r.5/Framework/framework")
if (cubismFramework.isDirectory) {
    include(":cubismFramework")
    project(":cubismFramework").projectDir = cubismFramework
}
