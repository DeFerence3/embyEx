package app.deference.embycl


class IosPlatform : Platform {
	override val name: String = "iOS"
	override val platform: Platforms = Platforms.IOS
}

actual fun getPlatform(): Platform = IosPlatform()