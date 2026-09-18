// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
//	alias(libs.plugins.kotzilla) apply true
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.kotlin.compose) apply false
}

//subprojects {
//	tasks.matching { it.name.startsWith("ksp") }.configureEach {
//		dependsOn(tasks.matching { it.name.startsWith("generateKotzillaConfig") })
//	}
//}
