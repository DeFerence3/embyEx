package app.deference.embycl.data.repository.home

import app.deference.embycl.core.session.Session
import app.deference.embycl.core.utils.NetworkUtils.safeApiCall
import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeLayoutSync
import app.deference.embycl.domain.repository.HomeLayoutRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@OptIn(ExperimentalCoroutinesApi::class)
@Single(binds = [HomeLayoutRepository::class])
class HomeLayoutRepositoryImpl(
    private val httpClient: HttpClient,
) : HomeLayoutRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer().nullable)
    private val mutex = Mutex()
    private val _sync = MutableStateFlow(HomeLayoutSync.Saved)
    override val sync: StateFlow<HomeLayoutSync> = _sync.asStateFlow()

    private val accountKey: Flow<String> = combine(
        Session.offlineAccount,
        Session.browseLocalFiles,
    ) { account, local ->
        if (local || account == null) LOCAL_ACCOUNT else "${account.key.serverId}:${account.key.userId}"
    }.distinctUntilChanged()

    override val layout: Flow<HomeLayoutPrefs> = accountKey.flatMapLatest { account ->
        channelFlow {
            if (account != LOCAL_ACCOUNT) {
                launch { Session.isConnected.filter { it }.collect { synchronize(account = account) } }
            }
            Session.preferences.observeString(key = storeKey(account = account)).collect { stored ->
                send(element = stored?.let(::decodeStored) ?: HomeLayoutPrefs())
            }
        }
    }.distinctUntilChanged()

    override suspend fun update(transform: (HomeLayoutPrefs) -> HomeLayoutPrefs) {
        mutex.withLock {
            val account = accountKey.first()
            val next = transform(read(account = account))
            write(
                account = account,
                layout = next,
            )
            if (account == LOCAL_ACCOUNT) return
            setPending(
                account = account,
                pending = true,
            )
            push(
                account = account,
                layout = next,
            )
        }
    }

    override suspend fun reset() = update { HomeLayoutPrefs() }

    private suspend fun synchronize(account: String) {
        mutex.withLock {
            if (isPending(account = account)) {
                push(
                    account = account,
                    layout = read(account = account),
                )
                return
            }
            val remote = try {
                fetch()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                return
            }
            if (remote != null && remote != read(account = account)) {
                write(
                    account = account,
                    layout = remote,
                )
            }
            _sync.value = HomeLayoutSync.Saved
        }
    }

    private suspend fun push(
        account: String,
        layout: HomeLayoutPrefs,
    ) {
        if (!online()) {
            _sync.value = HomeLayoutSync.LocalOnly
            return
        }
        _sync.value = HomeLayoutSync.Saving
        try {
            send(layout = layout)
            setPending(
                account = account,
                pending = false,
            )
            _sync.value = HomeLayoutSync.Saved
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _sync.value = HomeLayoutSync.LocalOnly
        }
    }

    private suspend fun fetch(): HomeLayoutPrefs? {
        if (!online()) return null
        val stored = safeApiCall {
            httpClient.get("/DisplayPreferences/$PREFERENCES_ID") {
                parameter("UserId", Session.userId)
                parameter("Client", CLIENT)
            }.body<DisplayPreferencesDto>()
        }
        return HomeLayoutCodec.decode(values = stored.customPrefs)
    }

    private suspend fun send(layout: HomeLayoutPrefs) {
        safeApiCall {
            httpClient.post("/DisplayPreferences/$PREFERENCES_ID") {
                parameter("UserId", Session.userId)
                setBody(
                    body = DisplayPreferencesDto(
                        id = PREFERENCES_ID,
                        client = CLIENT,
                        sortBy = "SortName",
                        sortOrder = "Ascending",
                        customPrefs = HomeLayoutCodec.encode(layout = layout),
                    ),
                )
            }
        }
    }

    private suspend fun read(account: String): HomeLayoutPrefs =
        Session.preferences.observeString(key = storeKey(account = account)).first()?.let(::decodeStored) ?: HomeLayoutPrefs()

    private suspend fun write(
        account: String,
        layout: HomeLayoutPrefs,
    ) {
        Session.preferences.setString(
            key = storeKey(account = account),
            value = json.encodeToString(mapSerializer, HomeLayoutCodec.encode(layout = layout)),
        )
    }

    private fun decodeStored(stored: String): HomeLayoutPrefs? = try {
        HomeLayoutCodec.decode(values = json.decodeFromString(mapSerializer, stored))
    } catch (exception: IllegalArgumentException) {
        null
    }

    private suspend fun isPending(account: String): Boolean =
        Session.preferences.observeString(key = pendingKey(account = account)).first() != null

    private suspend fun setPending(
        account: String,
        pending: Boolean,
    ) {
        Session.preferences.setString(
            key = pendingKey(account = account),
            value = if (pending) "1" else null,
        )
    }

    private fun online(): Boolean =
        !Session.browseLocalFiles.value && Session.downloadConnection.value != null && Session.isConnected.value

    private fun storeKey(account: String) = "home_layout:$account"

    private fun pendingKey(account: String) = "home_layout_pending:$account"

    private companion object {
        const val PREFERENCES_ID = "embyex-home"
        const val CLIENT = "EmbyEx"
        const val LOCAL_ACCOUNT = "local"
    }
}
