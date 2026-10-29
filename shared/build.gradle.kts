plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlinx.serialization)
	alias(libs.plugins.android.multiplatform.library)
	alias(libs.plugins.compose.multiplatform)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.koin.compiler)
	alias(libs.plugins.gmazzo.buildconfig)
	alias(libs.plugins.ksp)
	alias(libs.plugins.room)
}

room { schemaDirectory("$projectDir/schemas") }

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
			// Keep PlaybackPosition available through the existing Shared framework.
			export(project(":player"))
		}
	}
	
	jvm()
	// Source set declarations.
	// Declaring a target automatically creates a source set with the same name. By default, the
	// Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
	// common to share sources between related targets.
	// See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
	sourceSets {
		// Share Java IO/FileChannel; Android Os and desktop NIO primitives stay platform-specific.
		jvmMain { kotlin.srcDir("src/jvmAndroidMain/kotlin") }
		androidMain { kotlin.srcDir("src/jvmAndroidMain/kotlin") }
		commonMain.dependencies {
			// The shared playback route reports PlaybackPosition from :player.
			api(project(":player"))
			implementation(project(":coreui"))
			
			implementation(libs.room.runtime)
			implementation(libs.sqlite.bundled)
			implementation(libs.compose.runtime)
			implementation(libs.compose.foundation)
			implementation(libs.compose.material3)
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
			
			implementation(libs.compose.uiToolingPreview)
			
			implementation("io.github.deference3:koasty:0.0.1")
			
			implementation("com.mikepenz:multiplatform-markdown-renderer:0.45.0")
			implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.45.0")
			implementation("com.mikepenz:multiplatform-markdown-renderer-code:0.45.0")
			implementation("com.mikepenz:multiplatform-markdown-renderer-coil3:0.45.0")
		}
		
		commonTest {
			dependencies {
				implementation(libs.kotlin.test)
				implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinx.coroutines.get()}")
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
				implementation(libs.androidx.work)
				implementation(libs.androidx.activity.compose)
				implementation(libs.ktor.client.okhttp)
			}
		}

		getByName("androidHostTest").dependencies {
			implementation(libs.kotlin.testJunit)
			implementation("org.robolectric:robolectric:4.17")
			implementation("androidx.work:work-testing:${libs.versions.work.get()}")
		}
		
		appleMain.dependencies {
			implementation(libs.ktor.client.darwin)
		}
	}
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

// Explicitly invoked live-server check; never part of the ordinary test suite.
tasks.register<JavaExec>("offlineLiveSmoke") {
    group = "verification"
    dependsOn("jvmTestClasses")
    classpath = tasks.named<Test>("jvmTest").get().classpath
    mainClass.set("app.deference.embycl.data.repository.download.OfflineLiveSmoke")
    args(providers.gradleProperty("offlineLiveConnection").orElse("").get(),
        rootProject.layout.buildDirectory.dir("c12-live-desktop").get().asFile.absolutePath)
}
