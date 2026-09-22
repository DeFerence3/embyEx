package app.deference.embycl

class JVMPlatform: Platform {
	override val name: String = "${System.getProperty("os.name")} - Java ${System.getProperty("java.version")}"
	override val platform: Platforms = Platforms.JVM
}

actual fun getPlatform(): Platform = JVMPlatform()