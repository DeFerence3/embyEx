package app.deference.embycl

interface Platform {
	val name: String
	val platform: Platforms
}

expect fun getPlatform(): Platform

enum class Platforms{
	JVM,ANDROID,IOS
}