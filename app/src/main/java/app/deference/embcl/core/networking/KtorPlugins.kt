package app.deference.embcl.core.networking

import android.os.Build
import app.deference.embcl.BuildConfig
import app.deference.embcl.core.session.Session
import app.deference.embcl.core.session.Session.deviceId
import app.deference.embcl.core.utils.JsonUtils
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import okhttp3.HttpUrl.Companion.toHttpUrl

val DONT_INTERCEPT = AttributeKey<Boolean>("DONT_INTERCEPT")

/**
 * Don't intercept this request for setting server url,
 * instead uses provided host and port
 */
fun HttpRequestBuilder.dontIntercept(
	host: String,
	port: Int
) {
	url {
		this.host = host
		this.port = port
	}
	attributes[DONT_INTERCEPT] = true
}

fun HttpClient.invalidateAuthTokens() {
	authProvider<BearerAuthProvider>()?.clearToken()
}

fun HttpClient.urlInterceptor(){
	plugin(HttpSend).intercept { request ->
		val dontIntercept = request.attributes.getOrNull(DONT_INTERCEPT)
		val proccessedRequest = if (dontIntercept == true){
			request
		}else{
			val dynamicBase = Session.serverUrl.toHttpUrl()
			request.url.host = dynamicBase.host
			request.url.port = dynamicBase.port
			request
		}
		execute(proccessedRequest)
	}
}

fun HttpClientConfig<*>.configureLogging() {
	install(DFLogger) {
		logRequest = false
		logOnError = true
	}
}

fun HttpClientConfig<*>.configureDefaultRequest(userAgent: UserAgentProvider) {
	install(DefaultRequest) {
		contentType(ContentType.Application.Json)
		accept(ContentType.Application.Json)
		userAgent(userAgent.getUserAgent())
		headers {
			val authHeader = buildString {
				append("Emby Client=\"mpvEx\", Device=\"Android\", DeviceId=\"")
				append(deviceId)
				append("\", Version=\"")
				append(BuildConfig.VERSION_NAME)
				append('"')
				append(", UserId=\"${Session.userId}\"")
				Session.accessToken?.let { append(", Token=\"$it\"") }
			}
			val singleString =
				"Emby UserId=\"${Session.userId}\", Client=\"Mpv-Android\", Device=\"${Build.DEVICE}\", DeviceId=\"${deviceId}\", Version=\"${BuildConfig.VERSION_NAME}\""
			
			append("Emby-Client", "mpvEx")
			append("Device", "Android")
			append("DeviceId", deviceId)
			append("Version", BuildConfig.VERSION_NAME)
			append("UserId", Session.userId)
			append("X-Emby-Authorization", singleString)
			Session.accessToken?.let { append("X-Emby-Token", it) }
		}
	}
	
	install(HttpTimeout) {
		socketTimeoutMillis = 50000
	}
}

fun HttpClientConfig<*>.configureContentNegotiation() {
	install(ContentNegotiation) {
		json(
			json = JsonUtils.json,
			contentType = ContentType.Application.Json
		)
	}
}

fun HttpClientConfig<*>.configureValidation() {
	HttpResponseValidator {
		validateResponse { response: HttpResponse ->
			if (response.status.value == 401) {
				Session.logout()
				response.call.client.invalidateAuthTokens()
			}
		}
	}
}

fun HttpClientConfig<*>.configureAuth() {
	install(Auth) {
		bearer {
			loadTokens {
				val token = Session.accessToken
				token?.let {
					BearerTokens(it, null)
				}
			}
		}
		
		headers {
			val authHeader = buildString {
				append("Emby Client=\"mpvEx\", Device=\"Android\", DeviceId=\"")
				append(deviceId)
				append("\", Version=\"")
				append(BuildConfig.VERSION_NAME)
				append('"')
				append(", UserId=\"${Session.userId}\"")
				Session.accessToken?.let { append(", Token=\"$it\"") }
			}
			val singleString =
				"Emby UserId=\"${Session.userId}\", Client=\"Mpv-Android\", Device=\"${Build.DEVICE}\", DeviceId=\"${deviceId}\", Version=\"${BuildConfig.VERSION_NAME}\""
			
			HeadersBuilder().apply {
				append("Emby-Client", "mpvEx")
				append("Device", "Android")
				append("DeviceId", deviceId)
				append("Version", BuildConfig.VERSION_NAME)
				append("UserId", Session.userId)
				append("X-Emby-Authorization", singleString)
				Session.accessToken?.let { append("X-Emby-Token", it) }
			}.build()
		}
	}
}
