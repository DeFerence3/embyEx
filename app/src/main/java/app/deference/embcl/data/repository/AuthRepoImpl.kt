package app.deference.embcl.data.repository

import app.deference.embcl.core.networking.ApiResponseHandler.safeDataState
import app.deference.embcl.core.networking.DataState
import app.deference.embcl.domain.model.AuthenticateRequest
import app.deference.embcl.domain.model.AuthenticateUserRequest
import app.deference.embcl.domain.model.AuthenticationResult
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.repository.AuthRepo
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
			url {
				host = discovery.serverUrl
			}
			setBody(AuthenticateRequest(username.trim(), password))
		}
	}
	
	override suspend fun authenticate(
		discovery: EmbyServerDiscovery,
		user: EmbyUser,
		password: String,
	): DataState<AuthenticationResult> = safeDataState {
		httpClient.post("/Users/${user.id}/Authenticate") {
			url {
				host = discovery.serverUrl
			}
			setBody(AuthenticateUserRequest(password))
		}
	}
}