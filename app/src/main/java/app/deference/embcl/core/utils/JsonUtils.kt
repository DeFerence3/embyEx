package app.deference.embcl.core.utils

import kotlinx.serialization.json.Json

object JsonUtils {
	val json: Json by lazy {
		Json {
			encodeDefaults = true
			prettyPrint = true
			ignoreUnknownKeys = true
			coerceInputValues = true
			explicitNulls = false
			decodeEnumsCaseInsensitive = true
		}
	}
	
	/***
	 * Pretty prints a Kotlin object
	 */
	inline fun <reified T> prettyPrint(input: T,tag: String = "PrettyPrint") {
		val prettyJsonString = json.encodeToString(input)
		println("$tag--->Start")
		println( prettyJsonString)
		println("$tag--->End")
	}
	
	/***
	 * Prettifies a json string
	 */
	fun prettifyJsonString(input: String): String = json.encodeToString(json.parseToJsonElement(input))
	
	/***
	 * Pretty prints a json string
	 */
	fun prettyPrintString(input: String,tag: String = "PrettyPrintString") {
		val prettyJsonString: String = prettifyJsonString(input)
		println("$tag--->Start")
		println(prettyJsonString)
		println("$tag--->End")
	}
}