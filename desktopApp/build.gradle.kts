import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
//	alias(libs.plugins.nucleus)
}

kotlin{
	jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
	implementation(project(":coreui"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
	implementation(libs.ktor.client.core)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.compose.uiTest)
    testImplementation(libs.compose.nav3)
	implementation(libs.compose.icons)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinx.coroutines.get()}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${libs.versions.kotlinx.coroutines.get()}")
	
//	implementation(libs.bundles.nucleus)
}

tasks.test {
    // Register renders so a clean checkout or cache restore also recreates the gallery images.
    outputs.dir(rootProject.file("build/ui-verification/screenshots"))
}

// A separate review window with test fixtures; no sample downloads enter the production app.
tasks.register<JavaExec>("reviewDownloads") {
    group = "verification"
    description = "Open the interactive Downloads UI checkpoint with sample data"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.deference.embycl.downloads.DownloadsReviewKt")
}

// CI passes -PappVersion parsed from the vMAJOR.MINOR.PATCH[-beta.N] tag; local builds use the catalog.
val appVersion = providers.gradleProperty("appVersion").orElse(libs.versions.version.name).get()

// MSI accepts only numeric MAJOR.MINOR.BUILD, so installers carry 1.2.4 for 1.2.4-beta.1.
// The updater compares appVersion (written to latest-<platform>.yml and BuildConfig) instead.
val installerVersion = appVersion.substringBefore('-')

compose.desktop.application {
	mainClass = "app.deference.embycl.MainKt"
	
	/*graalvm{
		isEnabled = true
		imageName = "embyex"
		*//** Enabling advancedObfuscation and optimization somehow breaks serialization
		 * although nucleus docs states it works ootb *//*
//		optimization = NativeImageOptimization.LEVEL_3
//		advancedObfuscation = true
		// Keep the per-build mapping for diagnosing obfuscated native crash logs.
//		buildArgs.addAll("-H:AdvancedObfuscation=export-mapping","-H:+UnlockExperimentalVMOptions")
		toolchain{
			distribution = GraalvmDistribution.ORACLE
		}
	}*/
	
	nativeDistributions {
		targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
		packageName = "embyEx"
		packageVersion = installerVersion
		vendor = "Abhishek Krishnan T R"
		description = "A simple emby client, uses built in mpv lib based player to play media or open in your desired player. Follows material3 design."
		/*homepage = "https://github.com/DeFerence3/embyEx"
		compressionLevel = CompressionLevel.Ultra
		cleanupNativeLibs = true*/
		// From :desktopApp:suggestRuntimeModules; jdk.crypto.ec backs TLS ECDHE on JDK 21.
		modules(
			"java.instrument",
			"java.management",
			"java.net.http",
			"jdk.crypto.ec",
			"jdk.unsupported",
		)
		linux {
			iconFile.set(file("meta/images/icon.png"))
			shortcut = true
			menuGroup = "AudioVideo;Video;Player"
		}
		windows {
			menuGroup = "embyEx"
			iconFile.set(file("meta/images/icon.ico"))
			perUserInstall = true
			shortcut = true
			menu = true
			/*msi {
				oneClick = false
				perMachine = false
				createDesktopShortcut = true
				createStartMenuShortcut = true
				runAfterFinish = true
			}*/

			val properties = Properties().apply {
				val localPropertiesFile = rootProject.file("local.properties")
				if (localPropertiesFile.exists()) {
					localPropertiesFile.inputStream().use { load(it) }
				}
			}

			val certificatePath = providers.environmentVariable("WINDOWS_CERTIFICATE_FILE").orNull ?: properties.getProperty("pfxCert")
			val signingPassword = providers.environmentVariable("WINDOWS_CERTIFICATE_PASSWORD").orNull ?: properties.getProperty("pfxPassword")

			/*signing {
				enabled = !certificatePath.isNullOrBlank()
				if (!certificatePath.isNullOrBlank()) {
					certificateFile.set(file(certificatePath))
					certificatePassword = signingPassword
				}
				timestampServer = "http://timestamp.digicert.com"
			}*/
		}
	}
}

/**
 * The in-app updater (Nucleus updater runtime) reads `latest-<platform>.yml` from the latest
 * GitHub release: the version, plus each artifact's file name, base64 SHA-512 and size in bytes.
 * jpackage only packages for the host OS, so this writes the host's file. The Windows build also
 * writes the generic `latest.yml` (same content) for installs that predate the per-platform files.
 * Publish these files next to the installer they describe.
 */
tasks.register("generateUpdateMetadata") {
	group = "compose desktop"
	description = "Writes latest-<platform>.yml for the host's installer so the in-app updater can find it"
	val osName = providers.systemProperty("os.name").get().lowercase()
	val (platform, format) = when {
		osName.startsWith("windows") -> "windows" to "msi"
		osName.startsWith("linux") -> "linux" to "deb"
		else -> error("Update metadata is only generated for Windows and Linux, not $osName")
	}
	dependsOn("package${format.replaceFirstChar { it.uppercase() }}")
	val installerDirectory = layout.buildDirectory.dir("compose/binaries/main/$format")
	val metadataDirectory = layout.buildDirectory.dir("compose/binaries/main/update-metadata")
	val metadataNames = buildList {
		add("latest-$platform.yml")
		if (platform == "windows") add("latest.yml")
	}
	inputs.dir(installerDirectory)
	inputs.property("version", appVersion)
	metadataNames.forEach { name -> outputs.file(metadataDirectory.map { it.file(name) }) }
	doLast {
		val installer = installerDirectory.get().asFile
			.listFiles { file -> file.extension == format }
			?.singleOrNull()
			?: error("Expected exactly one .$format in ${installerDirectory.get().asFile}")
		val digest = MessageDigest.getInstance("SHA-512")
		installer.inputStream().use { input ->
			val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
			while (true) {
				val read = input.read(buffer)
				if (read < 0) break
				digest.update(buffer, 0, read)
			}
		}
		val sha512 = Base64.getEncoder().encodeToString(digest.digest())
		val metadata = """
			|version: $appVersion
			|files:
			|  - url: ${installer.name}
			|    sha512: $sha512
			|    size: ${installer.length()}
			|path: ${installer.name}
			|sha512: $sha512
			|releaseDate: '${Instant.now()}'
			|""".trimMargin()
		val directory = metadataDirectory.get().asFile.apply { mkdirs() }
		metadataNames.forEach { name -> directory.resolve(name).writeText(metadata) }
	}
}
