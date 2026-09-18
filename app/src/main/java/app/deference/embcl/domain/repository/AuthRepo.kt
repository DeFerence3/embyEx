package app.deference.embcl.domain.repository

import app.deference.embcl.core.networking.DataState
import app.deference.embcl.domain.model.AuthenticationResult
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUser
import org.koin.core.annotation.Single

@Single
interface AuthRepo {
	
	//suspend fun authenticate(server: String, username: String, password: String): EmbySession
	suspend fun authenticate(discovery: EmbyServerDiscovery, username: String, password: String): DataState<AuthenticationResult>
	suspend fun authenticate(discovery: EmbyServerDiscovery, user: EmbyUser, password: String): DataState<AuthenticationResult>
}