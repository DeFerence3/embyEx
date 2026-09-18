package app.deference.embcl.core.session

import app.deference.embcl.core.utils.HttpScheme
import kotlinx.serialization.Serializable

@Serializable
data class Server(
	val host: String,
	val port: Int,
	val scheme: HttpScheme
){
	fun toUrl(): String = "${scheme.value}://$host:$port"
}