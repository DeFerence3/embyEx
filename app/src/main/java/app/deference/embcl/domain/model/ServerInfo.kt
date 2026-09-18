package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
	@SerialName("ServerName") val serverName: String? = null,
	@SerialName("Id") val id: String? = null,
	@SerialName("Version") val version: String? = null,
	@SerialName("OperatingSystemDisplayName") val operatingSystemDisplayName: String? = null,
	@SerialName("OperatingSystem") val operatingSystem: String? = null,
	@SerialName("PackageName") val packageName: String? = null,
	@SerialName("SystemUpdateLevel") val updateLevel: String? = null,
	@SerialName("LocalAddress") val localAddress: String? = null,
	@SerialName("LocalAddresses") val localAddresses: List<String> = emptyList(),
	@SerialName("WanAddress") val wanAddress: String? = null,
	@SerialName("RemoteAddresses") val remoteAddresses: List<String> = emptyList(),
	@SerialName("HttpServerPortNumber") val httpPort: Int? = null,
	@SerialName("HttpsPortNumber") val httpsPort: Int? = null,
	@SerialName("WebSocketPortNumber") val webSocketPort: Int? = null,
	@SerialName("SupportsHttps") val supportsHttps: Boolean? = null,
	@SerialName("SupportsLibraryMonitor") val supportsLibraryMonitor: Boolean? = null,
	@SerialName("HasPendingRestart") val hasPendingRestart: Boolean? = null,
	@SerialName("HasUpdateAvailable") val hasUpdateAvailable: Boolean? = null,
	@SerialName("IsShuttingDown") val isShuttingDown: Boolean? = null,
	@SerialName("IsInMaintenanceMode") val isInMaintenanceMode: Boolean? = null,
	@SerialName("ProgramDataPath") val programDataPath: String? = null,
	@SerialName("CachePath") val cachePath: String? = null,
	@SerialName("LogPath") val logPath: String? = null,
	@SerialName("InternalMetadataPath") val metadataPath: String? = null,
	@SerialName("TranscodingTempPath") val transcodingTempPath: String? = null,
)

data class ServerDetails(val info: ServerInfo, val isLimited: Boolean = false)
