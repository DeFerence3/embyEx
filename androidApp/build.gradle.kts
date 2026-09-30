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

android {
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
		versionCode = libs.versions.version.code.get().toInt()
		versionName = libs.versions.version.name.get()
		
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
			if (hasReleaseSigning) signingConfig = signingConfigs.getByName("all")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	buildFeatures {
		compose = true
		buildConfig = true
	}
}

base {
	archivesName = "${rootProject.name}-${android.defaultConfig.versionName}"
}

dependencies {
	
	implementation(project(":shared"))
	
	implementation(libs.androidx.activity.compose)
	debugImplementation(libs.compose.uiToolingPreview)
}
