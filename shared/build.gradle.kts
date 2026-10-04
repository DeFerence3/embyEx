plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.android.multiplatform.library)
	alias(libs.plugins.compose.multiplatform)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.koin.compiler)
	alias(libs.plugins.gmazzo.buildconfig)
}

buildConfig {
	packageName("app.deference.embycl")
	buildConfigField("APP_NAME", rootProject.name)
	buildConfigField("VERSION_NAME", libs.versions.version.name)
	buildConfigField("APP_STORE_ID", providers.gradleProperty("APP_STORE_ID").orElse("").get())
}

compose.resources {
	publicResClass = true
}

kotlin {
	
	compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
	
	android {
		namespace = "app.deference.embycl.shared"
		compileSdk = libs.versions.android.compileSdk.get().toInt()
		minSdk = libs.versions.android.minSdk.get().toInt()
		
		androidResources {
			enable = true
		}
		withHostTest {
			isIncludeAndroidResources = true
		}
		withDeviceTestBuilder {
			sourceSetTreeName = "test"
		}.configure {
			instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		}
	}
	
	listOf(
		iosArm64(),
		iosSimulatorArm64()
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "Shared"
			isStatic = true
		}
	}
	
	jvm()
	// Source set declarations.
	// Declaring a target automatically creates a source set with the same name. By default, the
	// Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
	// common to share sources between related targets.
	// See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
	sourceSets {
		commonMain.dependencies {
			implementation(libs.compose.runtime)
			implementation(libs.compose.foundation)
			api(libs.compose.material3)
			implementation(libs.compose.ui)
			api(libs.compose.components.resources)
			implementation(libs.androidx.lifecycle.viewmodelCompose)
			implementation(libs.androidx.lifecycle.runtimeCompose)
			
			api(project.dependencies.platform(libs.koin.bom))
			api(libs.bundles.koin)
			
			implementation(libs.bundles.ktor)
			
			implementation(libs.kotlinx.datetime)
			
			implementation(libs.coil.compose)
			implementation(libs.coil.network.ktor)
			
			implementation(libs.compose.nav3)
			implementation(libs.lifecycle.viewmodel.navigation3)
			
			implementation(libs.compose.icons)
			
			api(libs.androidx.datastore.preferences)
			
			implementation(libs.material3.adaptive)
			implementation(libs.material3.adaptive.navigation.suite)
			
			implementation("io.github.deference3:koasty:0.0.1")
			
			implementation("com.mikepenz:multiplatform-markdown-renderer:0.45.0")
			implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.45.0")
			implementation("com.mikepenz:multiplatform-markdown-renderer-code:0.45.0")
			implementation("com.mikepenz:multiplatform-markdown-renderer-coil3:0.45.0")
		}
		
		commonTest {
			dependencies {
				implementation(libs.kotlin.test)
			}
		}
		
		jvmMain.dependencies {
			implementation(libs.jmdns)
			implementation(libs.ktor.client.okhttp)
			implementation(libs.nucleus.updater)
		}

		jvmTest.dependencies {
			implementation(libs.kotlin.testJunit)
		}
		
		androidMain {
			dependencies {
				implementation(libs.ktor.client.okhttp)
			}
		}

		getByName("androidHostTest").dependencies {
			implementation(libs.kotlin.testJunit)
			implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
		}
		
		appleMain.dependencies {
			implementation(libs.ktor.client.darwin)
		}
	}
}

