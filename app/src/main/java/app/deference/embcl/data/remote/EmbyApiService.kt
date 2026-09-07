package app.deference.embcl.data.remote

import app.deference.embcl.domain.model.AuthenticateRequest
import app.deference.embcl.domain.model.AuthenticateUserRequest
import app.deference.embcl.domain.model.AuthenticationResult
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbyItemsResult
import app.deference.embcl.domain.model.EmbyPlaybackReport
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.model.PublicSystemInfo
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface EmbyApiService {
	
	@GET("/System/Info/Public")
	suspend fun publicSystemInfo(
	): PublicSystemInfo
	
	@GET("/Users/Public")
	suspend fun publicUsers(
	): List<EmbyUser>
	
	@POST("/Users/AuthenticateByName")
	suspend fun authenticateByName(
		@Body request: AuthenticateRequest,
	): AuthenticationResult
	
	@POST("/Users/{userId}/Authenticate")
	suspend fun authenticateUser(
		@Path("userId") userId: String,
		@Body request: AuthenticateUserRequest,
	): AuthenticationResult
	
	@GET("/Users/{userId}/Views")
	suspend fun userViews(
		@Path("userId") userId: String,
		@QueryMap options: Map<String, String>,
	): EmbyItemsResult
	
	@GET("/Users/{userId}/Items")
	suspend fun userItems(
		@Path("userId") userId: String,
		@QueryMap options: Map<String, String>,
	): EmbyItemsResult
	
	@GET("/Users/{userId}/Items/Resume")
	suspend fun resumeItems(
		@Path("userId") userId: String,
		@QueryMap options: Map<String, String>,
	): EmbyItemsResult
	
	@GET("/Users/{userId}/Items/Latest")
	suspend fun latestItems(
		@Path("userId") userId: String,
		@QueryMap options: Map<String, String>
	): List<EmbyItem>
	
	@GET("/Users/{userId}/Items/{itemId}")
	suspend fun item(
		@Path("userId") userId: String,
		@Path("itemId") itemId: String
	): EmbyItem
	
	@POST("/Sessions/Playing")
	fun reportPlayback(
		@Body report: EmbyPlaybackReport
	): Call<Void>
	
	@POST("/Sessions/Playing/Progress")
	fun reportPlaybackProgress(
		@Body report: EmbyPlaybackReport
	): Call<Void>
	
	@POST("/Sessions/Playing/Stopped")
	fun reportPlaybackStopped(
		@Body report: EmbyPlaybackReport
	): Call<Void>
}
