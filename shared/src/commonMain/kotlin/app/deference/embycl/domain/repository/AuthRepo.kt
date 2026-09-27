package app.deference.embycl.domain.repository

import app.deference.embycl.core.networking.DataState
import app.deference.embycl.domain.model.AuthenticationResult
import app.deference.embycl.domain.model.EmbyServerDiscovery
import app.deference.embycl.domain.model.EmbyUser
import org.koin.core.annotation.Single

@Single
interface AuthRepo {
	suspend fun authenticate(discovery: EmbyServerDiscovery, username: String, password: String): DataState<AuthenticationResult>
	suspend fun authenticate(discovery: EmbyServerDiscovery, user: EmbyUser, password: String): DataState<AuthenticationResult>
}