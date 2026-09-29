import dev.nucleusframework.desktop.application.dsl.CompressionLevel
import dev.nucleusframework.desktop.application.dsl.GraalvmDistribution
import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization
import dev.nucleusframework.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
	alias(libs.plugins.nucleus)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
	
	implementation(libs.bundles.nucleus)
}

nucleus.application {
	mainClass = "app.deference.embycl.MainKt"
	
	graalvm{
		isEnabled = true
		imageName = "embyex"
		optimization = NativeImageOptimization.LEVEL_3
		advancedObfuscation = true
		toolchain{
			distribution = GraalvmDistribution.ORACLE
		}
	}
	
	nativeDistributions {
		targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
		packageName = "embyEx"
		packageVersion = libs.versions.version.name.get()
		vendor = "Abhishek Krishnan T R"
		description = "A simple emby client, opens media in mpvEx in android and mpv in desktop."
		homepage = "https://github.com/DeFerence3/embyEx"
		compressionLevel = CompressionLevel.Ultra
		cleanupNativeLibs = true
		windows {
			menuGroup = "embyEx"
			msi {
				oneClick = false
				perMachine = false
				createDesktopShortcut = true
				createStartMenuShortcut = true
				runAfterFinish = true
			}
		}
	}
}