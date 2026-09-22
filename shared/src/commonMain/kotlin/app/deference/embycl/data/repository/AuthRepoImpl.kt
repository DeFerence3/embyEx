package app.deference.embycl.data.repository

import app.deference.embycl.core.networking.DataState
import app.deference.embycl.core.networking.dontIntercept
import app.deference.embycl.core.utils.NetworkUtils.safeDataState
import app.deference.embycl.domain.model.AuthenticateRequest
import app.deference.embycl.domain.model.AuthenticateUserRequest
import app.deference.embycl.domain.model.AuthenticationResult
import app.deference.embycl.domain.model.EmbyServerDiscovery
import app.deference.embycl.domain.model.EmbyUser
import app.deference.embycl.domain.repository.AuthRepo
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.koin.core.annotation.Single

@Single
class AuthRepoImpl(
	private val httpClient: HttpClient,
) : AuthRepo {
	
	override suspend fun authenticate(
		discovery: EmbyServerDiscovery,
		username: String,
		password: String,
	): DataState<AuthenticationResult> = safeDataState {
		httpClient.post("/Users/AuthenticateByName") {
			dontIntercept(
				host = discovery.server.host,
				port = discovery.server.port
			)
			setBody(AuthenticateRequest(username.trim(), password))
		}
	}
	
	override suspend fun authenticate(
		discovery: EmbyServerDiscovery,
		user: EmbyUser,
		password: String,
	): DataState<AuthenticationResult> = safeDataState {
		httpClient.post("/Users/${user.id}/Authenticate") {
			dontIntercept(
				host = discovery.server.host,
				port = discovery.server.port
			)
			setBody(AuthenticateUserRequest(password))
		}
	}
}