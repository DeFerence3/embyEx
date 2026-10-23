
import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.GraalvmDistribution
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
	alias(libs.plugins.nucleus)
}

kotlin{
	jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.compose.uiTest)
    testImplementation(libs.compose.nav3)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinx.coroutines.get()}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${libs.versions.kotlinx.coroutines.get()}")
	
	implementation(libs.bundles.nucleus)
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

nucleus.application {
	mainClass = "app.deference.embycl.MainKt"
	
	graalvm{
		isEnabled = true
		imageName = "embyex"
		/** Enabling advancedObfuscation and optimization somehow breaks serialization
		 * although nucleus docs states it works ootb */
//		optimization = NativeImageOptimization.LEVEL_3
//		advancedObfuscation = true
		// Keep the per-build mapping for diagnosing obfuscated native crash logs.
//		buildArgs.addAll("-H:AdvancedObfuscation=export-mapping","-H:+UnlockExperimentalVMOptions")
		toolchain{
			distribution = GraalvmDistribution.ORACLE
		}
	}
	
	nativeDistributions {
		targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
		packageName = "embyEx"
		packageVersion = libs.versions.version.name.get()
		vendor = "Abhishek Krishnan T R"
		description = "A simple emby client, uses built in mpv lib based player to play media or open in your desired player. Follows material3 design."
		homepage = "https://github.com/DeFerence3/embyEx"
		compressionLevel = CompressionLevel.Ultra
		cleanupNativeLibs = true
		windows {
			menuGroup = "embyEx"
			iconFile.set(file("meta/images/icon.ico"))
			msi {
				oneClick = false
				perMachine = false
				createDesktopShortcut = true
				createStartMenuShortcut = true
				runAfterFinish = true
			}
			
			val properties = Properties().apply {
				val localPropertiesFile = rootProject.file("local.properties")
				if (localPropertiesFile.exists()) {
					localPropertiesFile.inputStream().use { load(it) }
				}
			}
			
			val certificatePath = providers.environmentVariable("WINDOWS_CERTIFICATE_FILE").orNull ?: properties.getProperty("pfxCert")
			val signingPassword = providers.environmentVariable("WINDOWS_CERTIFICATE_PASSWORD").orNull ?: properties.getProperty("pfxPassword")

			signing {
				enabled = !certificatePath.isNullOrBlank()
				if (!certificatePath.isNullOrBlank()) {
					certificateFile.set(file(certificatePath))
					certificatePassword = signingPassword
				}
				timestampServer = "http://timestamp.digicert.com"
			}
		}
	}
}
