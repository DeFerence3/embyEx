plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.ksp)
}

android {
	namespace = "app.deference.embcl"
	compileSdk {
		version = release(37)
	}
	
	defaultConfig {
		applicationId = "app.deference.embcl"
		minSdk = 24
		targetSdk = 37
		versionCode = 1
		versionName = "1.0"
		
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	
	buildTypes {
		release {
			optimization {
				enable = true
			}
			isShrinkResources = true
			isMinifyEnabled = true
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
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.compose.material3)
	implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
	implementation(libs.androidx.compose.ui)
	implementation(libs.androidx.compose.ui.graphics)
	implementation(libs.androidx.compose.ui.tooling.preview)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	testImplementation(libs.junit)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.compose.ui.test.junit4)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.androidx.junit)
	debugImplementation(libs.androidx.compose.ui.test.manifest)
	debugImplementation(libs.androidx.compose.ui.tooling)
	
	implementation(libs.coil.compose)
	implementation(libs.coil.network.okhttp)
	implementation(libs.kotlinx.serialization.json)
	
	// Retrofit
	implementation(libs.bundles.retrofit)
	
	implementation(platform(libs.koin.bom))
	implementation(libs.bundles.koin)
	
	implementation(libs.compose.nav3)
	implementation(libs.lifecycle.viewmodel.navigation3)
	
	implementation(libs.androidx.material3.icons.extended)
	
	implementation(libs.koasty)
	
	implementation(libs.kotlinx.datetime)
}