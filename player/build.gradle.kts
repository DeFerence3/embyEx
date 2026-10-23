plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "app.deference.embycl.player"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // PlayerSource.subtitles exposes MediaMP's Subtitle value type.
            api(libs.mediamp.api)
            // EmbeddedPlayerDialog and PlayerInlayHost expose @Composable APIs.
            api(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.icons)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }

        androidMain.dependencies {
            implementation(libs.mediamp.exoplayer)
            implementation(libs.androidx.activity.compose)
        }

        jvmMain.dependencies {
            implementation(libs.nucleus.decorated.window.tao)
            implementation(libs.nucleus.application)
            implementation(libs.mediamp.mpv)

            val osName = providers.systemProperty("os.name").get().lowercase()
            val architecture = providers.systemProperty("os.arch").get().lowercase()
            val arm64 = architecture == "aarch64" || architecture == "arm64"
            require(arm64 || architecture == "amd64" || architecture == "x86_64") {
                "MediaMP does not publish a desktop runtime for $architecture"
            }
            runtimeOnly(
                when {
                    osName.startsWith("windows") && arm64 -> libs.mediamp.mpv.runtime.windows.arm64
                    osName.startsWith("windows") -> libs.mediamp.mpv.runtime.windows.x64
                    osName.startsWith("mac") && arm64 -> libs.mediamp.mpv.runtime.macos.arm64
                    osName.startsWith("mac") -> libs.mediamp.mpv.runtime.macos.x64
                    osName.startsWith("linux") && !arm64 -> libs.mediamp.mpv.runtime.linux.x64
                    else -> error("MediaMP does not publish a desktop runtime for $osName/$architecture")
                },
            )
        }
    }
}

// Only the Linux host builds the small libmpv software-render bridge. Windows
// and macOS use MediaMP's packaged readback renderers, all inside Tao.
if (providers.systemProperty("os.name").get().startsWith("Linux")) {
    val rendererResources = layout.buildDirectory.dir("generated/playerResources")
    val rendererJdk = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    val buildLinuxPlayer by tasks.registering(Exec::class) {
        inputs.dir("src/jvmMain/cpp")
        inputs.property("mediampVersion", libs.versions.mediamp)
        outputs.file(rendererResources.map { it.file("libembyex-player.so") })
        commandLine(
            "sh",
            layout.projectDirectory.file("src/jvmMain/cpp/build-linux.sh").asFile.absolutePath,
            rendererResources.get().file("libembyex-player.so").asFile.absolutePath,
            rendererJdk.get().metadata.installationPath.asFile.absolutePath,
        )
    }
    kotlin.sourceSets.named("jvmMain") {
        resources.srcDir(files(rendererResources).builtBy(buildLinuxPlayer))
    }
}
