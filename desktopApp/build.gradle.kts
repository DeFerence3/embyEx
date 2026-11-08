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

val appVersion = libs.versions.version.name.get()

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
		packageVersion = appVersion
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
 * The in-app updater (Nucleus updater runtime) reads `latest.yml` from the latest GitHub
 * release: the version, plus each artifact's file name, base64 SHA-512 and size in bytes.
 * Publish this file next to the MSI it describes.
 */
tasks.register("generateMsiUpdateMetadata") {
	group = "compose desktop"
	description = "Writes latest.yml for the packaged MSI so the in-app updater can find it"
	dependsOn("packageMsi")
	val msiDirectory = layout.buildDirectory.dir("compose/binaries/main/msi")
	val metadataFile = layout.buildDirectory.file("compose/binaries/main/update-metadata/latest.yml")
	inputs.dir(msiDirectory)
	inputs.property("version", appVersion)
	outputs.file(metadataFile)
	doLast {
		val msi = msiDirectory.get().asFile
			.listFiles { file -> file.extension == "msi" }
			?.singleOrNull()
			?: error("Expected exactly one MSI in ${msiDirectory.get().asFile}")
		val digest = MessageDigest.getInstance("SHA-512")
		msi.inputStream().use { input ->
			val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
			while (true) {
				val read = input.read(buffer)
				if (read < 0) break
				digest.update(buffer, 0, read)
			}
		}
		val sha512 = Base64.getEncoder().encodeToString(digest.digest())
		metadataFile.get().asFile.apply { parentFile.mkdirs() }.writeText(
			"""
			|version: $appVersion
			|files:
			|  - url: ${msi.name}
			|    sha512: $sha512
			|    size: ${msi.length()}
			|path: ${msi.name}
			|sha512: $sha512
			|releaseDate: '${Instant.now()}'
			|""".trimMargin(),
		)
	}
}
