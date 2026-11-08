
import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.compose.multiplatform)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.koin.compiler)
	alias(libs.plugins.ksp)
}

val signingProperties = Properties().apply {
	val localPropertiesFile = rootProject.file("local.properties")
	if (localPropertiesFile.exists()) {
		localPropertiesFile.inputStream().use { load(it) }
	}
}
fun signingValue(environmentName: String, propertyName: String): String? = (providers.environmentVariable(environmentName).orNull ?: signingProperties.getProperty(propertyName))?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("ANDROID_KEYSTORE_FILE", "storeFile")
val releaseStorePassword = signingValue("STORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("KEY_ALIAS", "storeKeyAlias")
val releaseKeyPassword = signingValue("KEY_PASSWORD", "storeKeyPassword")
val signingValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val hasReleaseSigning = signingValues.all { it != null }

// CI passes -PappVersion parsed from the vMAJOR.MINOR.PATCH[-beta.N] tag; local builds use the catalog.
val appVersion = providers.gradleProperty("appVersion")
	.orElse(libs.versions.version.name)
	.get()

/**
 * A release is MAJOR * 1_000_000 + MINOR * 10_000 + PATCH * 100. `-beta.N` of that release is
 * N below the previous slot (release - 100 + N), so betas sit between the previous release and
 * their own: 1.2.3 = 1020300, 1.2.4-beta.1 = 1020301, 1.2.4-beta.2 = 1020302, 1.2.4 = 1020400.
 */
fun versionCodeOf(version: String): Int {
	val match = requireNotNull(Regex("""(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?""").matchEntire(version)) {
		"App version must be MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-beta.N, was '$version'"
	}
	val (major, minor, patch) = match.groupValues.subList(1, 4).map { it.toInt() }
	val beta = match.groupValues[4].toIntOrNull()
	require(minor < 100 && patch < 100) { "MINOR and PATCH must be below 100, was '$version'" }
	require(beta == null || beta in 1..99) { "The beta number must be 1 to 99, was '$version'" }
	val release = major * 1_000_000 + minor * 10_000 + patch * 100
	return if (beta == null) release else release - 100 + beta
}

android {
	if (providers.gradleProperty("offlineVerification").orNull == "true") {
		// Existing unrelated instrumentation suites may target older APIs; this mode compiles only the offline checks.
		sourceSets.getByName("androidTest").kotlin.directories.clear()
		sourceSets.getByName("androidTest").kotlin.directories.add("src/androidTest/java/app/deference/embcl/downloads")
	}
	namespace = "app.deference.embcl"
	compileSdk {
		version = release(libs.versions.android.compileSdk.get().toInt())
	}
	
	signingConfigs {
		create("all") {
			storePassword = releaseStorePassword
			keyAlias = releaseKeyAlias
			keyPassword = releaseKeyPassword
			storeFile = file(requireNotNull(releaseStoreFile))
		}
	}
	
	defaultConfig {
		applicationId = "app.deference.embcl"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionCode = versionCodeOf(version = appVersion)
		versionName = appVersion
		
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		if (hasReleaseSigning) signingConfig = signingConfigs.getByName("all")
	}
	
	buildTypes {
		release {
			optimization {
				enable = true
			}
			isShrinkResources = true
			isMinifyEnabled = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro",
			)
		}
		debug {
			// Opt-in device checks use a separate sandbox, preserving the normal installation.
			if (providers.gradleProperty("offlineVerification").orNull == "true") applicationIdSuffix = ".offlinecheck"
			if (hasReleaseSigning) signingConfig = signingConfigs.getByName("all")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
}

androidComponents {
	onVariants(selector().withBuildType("release")) { variant ->
		// GitHub distributes APKs directly. Compress native codecs for smaller downloads;
		// Android extracts the device's libraries during installation.
		variant.packaging.jniLibs.useLegacyPackaging.set(true)
	}
}

base {
	archivesName = "${rootProject.name}-${android.defaultConfig.versionName}"
}

dependencies {
	
	implementation(project(":shared"))
	implementation(project(":coreui"))
	
	implementation(libs.androidx.activity.compose)
	implementation(libs.material)
	implementation(libs.ktor.client.core)
	
	debugImplementation(libs.compose.uiToolingPreview)
	
	androidTestImplementation(libs.androidx.testExt.junit)
	androidTestImplementation(libs.androidx.runner)
	androidTestImplementation(libs.ktor.client.mock)
	androidTestImplementation(libs.ktor.client.okhttp)
	androidTestImplementation(libs.ktor.serialization.kotlinx.json)
	androidTestImplementation(libs.room.runtime)
	androidTestImplementation(libs.sqlite.bundled)
}
