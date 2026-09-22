import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.compose.multiplatform)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.koin.compiler)
	alias(libs.plugins.ksp)
}

android {
	namespace = "app.deference.embcl"
	compileSdk {
		version = release(libs.versions.android.compileSdk.get().toInt())
	}
	
	signingConfigs {
		val properties = Properties().apply {
			val localPropertiesFile = rootProject.file("local.properties")
			if (localPropertiesFile.exists()) {
				localPropertiesFile.inputStream().use { load(it) }
			}
		}
		create("all"){
			storePassword = properties.getProperty("storePassword")
			keyAlias = properties.getProperty("storeKeyAlias")
			keyPassword = properties.getProperty("storeKeyPassword")
			storeFile = file(properties.getProperty("storeFile"))
		}
	}
	
	defaultConfig {
		applicationId = "app.deference.embcl"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionCode = libs.versions.version.code.get().toInt()
		versionName = libs.versions.version.name.get()
		
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		signingConfig = signingConfigs.getByName("all")
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
			signingConfig = signingConfigs.getByName("all")
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

	implementation(libs.koin.androidx.compose)

	implementation(libs.androidx.datastore.preferences)
}
