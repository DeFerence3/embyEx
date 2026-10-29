package app.deference.embycl.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.Surface
import androidx.core.net.toUri
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvEvent
import `is`.xyz.mpv.MPVLib.MpvFormat
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Android libmpv backend implementing the app-owned playback contract.
 *
 * MPVLib owns a process-wide native handle. The mutex remains held until asynchronous
 * destruction finishes, so rotation/retry cannot create a second native instance.
 * Native callbacks are posted to the UI thread and attributed to their media session.
 */
internal class AndroidMpvPlayer(
    context: Context,
    private val ownerScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EmbeddedPlayer {
    private val mutableState = MutableStateFlow(value = PlayerState())
    private val mutablePosition = MutableStateFlow(value = 0L)
    private val mutableProperties = MutableStateFlow<PlayerMediaProperties?>(value = null)
    // Low-volume facts only: position updates stay in StateFlow.
    private val eventChannel = Channel<PlayerEvent>(capacity = Channel.UNLIMITED)
    override val state = mutableState.asStateFlow()
    override val currentPositionMillis = mutablePosition.asStateFlow()
    override val mediaProperties = mutableProperties.asStateFlow()
    override val events = eventChannel.receiveAsFlow()
    private var openingJob: Job? = null
    private var mediaDescriptor: ParcelFileDescriptor? = null
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var nativeReady = false
    private var nativeLifetime: Job? = null
    private var released = false
    private val surface = MutableStateFlow<Surface?>(value = null)
    private var attachedSurface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    @Volatile
    private var activeSession: Session? = null
    @Volatile
    private var eventEntryId = -1L

    private val audioFocus = AndroidMpvAudioFocus(
        context = appContext,
        onPause = { if (!released) pause() },
    )
    private val metadata = AndroidMpvMetadata(
        isReady = { nativeReady && activeSession?.loaded == true },
    )
    private val aspectMode = MutableStateFlow(value = PlayerAspectRatio.FIT)
    override val aspectRatio = object : PlayerAspect {
        override val mode = aspectMode.asStateFlow()

        override fun setMode(
            mode: PlayerAspectRatio,
        ) {
            if (released) return
            aspectMode.value = mode
            applyAspect()
        }
    }
    override val audioTracks get() = metadata.audioTracks
    override val subtitleTracks get() = metadata.subtitleTracks
    private val speed = MutableStateFlow(value = 1f)
    override val playbackSpeed = object : PlayerSpeed {
        override val valueFlow = speed.asStateFlow()
        override val value get() = speed.value

        override fun set(
            speed: Float,
        ) {
            if (released || !speed.isFinite() || speed <= 0f) return
            this@AndroidMpvPlayer.speed.value = speed
            if (nativeReady) MPVLib.setPropertyDouble(property = "speed", value = speed.toDouble())
        }
    }

    private class Session {
        val opened = CompletableDeferred<Unit>()
        var entryId = -1L
        var loaded = false
        var seekGeneration = 0
        var seeking = false
        var seekObserved = false
        var queuedSeek: Long? = null
        var seekTimeout: Job? = null
    }

    private val observer = object : MPVLib.EventObserver {
        override fun eventProperty(
            property: String,
        ) = dispatch { session ->
            when (property) {
                "track-list", "aid", "sid" -> metadata.refresh()
                "video-params", "duration" -> if (session.loaded) {
                    mutableProperties.value = readProperties()
                }
            }
        }

        override fun eventProperty(
            property: String,
            value: Long,
        ) = Unit
        override fun eventProperty(
            property: String,
            value: String,
        ) = Unit
        override fun eventProperty(
            property: String,
            value: MPVNode,
        ) = Unit

        override fun eventProperty(
            property: String,
            value: Double,
        ) = dispatch { session ->
            if (session.loaded && !session.seeking && property == "time-pos" && value.isFinite()) {
                mutablePosition.value = (value * 1_000).toLong().coerceAtLeast(minimumValue = 0)
            }
        }

        override fun eventProperty(
            property: String,
            value: Boolean,
        ) = dispatch { session ->
            if (session.loaded) {
                when (property) {
                    "eof-reached" -> if (value) {
                        audioFocus.release()
                        notifyEnded()
                    }
                    "pause", "paused-for-cache" -> if (MPVLib.getPropertyBoolean(property = "eof-reached") != true) {
                        refreshTransport()
                    }
                }
            }
        }

        override fun event(
            eventId: Int,
            data: MPVNode,
        ) {
            if (eventId == MpvEvent.MPV_EVENT_START_FILE) {
                eventEntryId = data["playlist_entry_id"]?.asInt() ?: -1L
            }
            val entryId = if (eventId == MpvEvent.MPV_EVENT_END_FILE) {
                data["playlist_entry_id"]?.asInt() ?: eventEntryId
            } else eventEntryId
            dispatch(entryId = entryId) { session ->
                when (eventId) {
                    MpvEvent.MPV_EVENT_FILE_LOADED -> {
                        session.loaded = true
                        metadata.refresh()
                        session.opened.complete(value = Unit)
                    }
                    MpvEvent.MPV_EVENT_SEEK -> session.seekObserved = session.seeking
                    MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                        if (session.seekObserved) completeSeek(session = session)
                    }
                    MpvEvent.MPV_EVENT_END_FILE -> {
                        val reason = data["reason"]?.asString()
                        when {
                            reason == "redirect" -> {
                                session.entryId = data["playlist_insert_id"]?.asInt() ?: -1L
                            }
                            reason == "eof" && session.loaded -> {
                                audioFocus.release()
                                notifyEnded()
                            }
                            reason == "error" || !session.loaded -> {
                                val error = IllegalStateException("libmpv could not play this media.")
                                if (!session.opened.completeExceptionally(exception = error)) {
                                    notifyError()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dispatch(
        entryId: Long = eventEntryId,
        action: (Session) -> Unit,
    ) {
        val session = activeSession ?: return
        handler.post {
            if (nativeReady && activeSession === session &&
                (entryId < 0 || session.entryId < 0 || session.entryId == entryId)
            ) {
                action(session)
            }
        }
    }

    @SuppressLint("Recycle") // Held until native teardown finishes, including cancellation and failed loads.
    override suspend fun open(
        source: PlayerSource,
        playWhenReady: Boolean,
        startPositionMillis: Long,
    ) = coroutineScope {
        check(value = !released && state.value.mediaStatus == PlayerMediaStatus.Idle)
        openingJob = coroutineContext.job
        var pendingDescriptor: ParcelFileDescriptor? = null
        mutableState.value = PlayerState(
            mediaStatus = PlayerMediaStatus.Opening,
            playWhenReady = playWhenReady,
        )
        mutablePosition.value = startPositionMillis.coerceAtLeast(minimumValue = 0)
        try {
            initialize()
            // Loading without a surface can cause mpv to disable the video track.
            surface.first { it?.isValid == true }
            attachAvailableSurface()
            if (source.uri.toUri().scheme == "content") {
                withContext(context = ioDispatcher) {
                    // Assign before dispatching back so cancellation cannot leak the descriptor.
                    pendingDescriptor = appContext.contentResolver.openFileDescriptor(source.uri.toUri(), "r")
                        ?: throw IllegalStateException("The local video could not be opened.")
                }
            }
            mediaDescriptor = pendingDescriptor
            pendingDescriptor = null
            val current = Session()
            activeSession = current
            metadata.clear()
            metadata.setExternalSubtitles(subtitles = source.subtitles)
            setStringList(
                property = "http-header-fields",
                values = source.headers.map { (name, value) ->
                    require(value = '\r' !in name && '\n' !in name && '\r' !in value && '\n' !in value)
                    "$name: $value"
                },
            )
            setStringList(
                property = "sub-files",
                values = source.subtitles.map { it.uri },
            )
            val mayPlay = state.value.playWhenReady && audioFocus.acquire()
            mutableState.update { it.copy(playWhenReady = mayPlay) }
            MPVLib.setPropertyBoolean(property = "pause", value = !mayPlay)
            MPVLib.setPropertyDouble(property = "speed", value = speed.value.toDouble())
            val target = mediaDescriptor?.let { "fd://${it.fd}" } ?: source.uri
            val result = MPVLib.commandNode(
                "loadfile", target, "replace", "-1",
                "start=${startPositionMillis.coerceAtLeast(minimumValue = 0) / 1000.0}",
            )
            check(value = result != null) { "libmpv rejected the media source." }
            current.entryId = MPVLib.getPropertyNode(property = "playlist/0/id")?.asInt() ?: -1L
            current.opened.await()
            mutableProperties.value = readProperties()
            if (state.value.mediaStatus != PlayerMediaStatus.Ended) {
                val resume = state.value.playWhenReady && audioFocus.acquire()
                MPVLib.setPropertyBoolean(property = "pause", value = !resume)
                mutableState.update { it.copy(mediaStatus = PlayerMediaStatus.Ready) }
                refreshTransport()
                if (MPVLib.getPropertyBoolean(property = "eof-reached") == true) notifyEnded()
            }
        } catch (cancellation: CancellationException) {
            close()
            throw cancellation
        } catch (failure: Exception) {
            notifyError()
            throw failure
        } finally {
            openingJob = null
            withContext(context = NonCancellable + ioDispatcher) { pendingDescriptor?.close() }
        }
    }

    private suspend fun initialize() {
        if (nativeReady) return
        nativeOwner.lock(owner = this)
        var created = false
        try {
            withContext(context = ioDispatcher) {
                val options = androidMpvOptions(context = appContext)
                MPVLib.create(appctx = appContext)
                created = true
                options.forEach { (name, value) -> MPVLib.setOptionString(name = name, value = value) }
                MPVLib.init()
            }
            currentCoroutineContext().ensureActive()
            MPVLib.addObserver(o = observer)
            observeProperties()
            nativeReady = true
            applyAspect()
            attachAvailableSurface()
            nativeLifetime = ownerScope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    // Native destruction joins its event thread; never block the UI for that join.
                    withContext(context = NonCancellable + Dispatchers.Main.immediate) { releaseNative() }
                    withContext(context = NonCancellable + ioDispatcher) {
                        try {
                            MPVLib.destroy()
                        } finally {
                            try {
                                mediaDescriptor?.close()
                                mediaDescriptor = null
                            } finally {
                                nativeOwner.unlock(owner = this@AndroidMpvPlayer)
                            }
                        }
                    }
                }
            }
        } catch (failure: Throwable) {
            withContext(context = NonCancellable + Dispatchers.Main.immediate) { releaseNative() }
            // Cancellation must finish native teardown before another player can acquire it.
            withContext(context = NonCancellable + ioDispatcher) {
                try {
                    if (created) MPVLib.destroy()
                } finally {
                    nativeOwner.unlock(owner = this@AndroidMpvPlayer)
                }
            }
            if (failure is LinkageError) {
                throw IllegalStateException("The Android libmpv runtime could not be loaded.")
            }
            throw failure
        }
    }

    private fun observeProperties() {
        listOf("pause", "paused-for-cache", "eof-reached").forEach { property ->
            MPVLib.observeProperty(property = property, format = MpvFormat.MPV_FORMAT_FLAG)
        }
        MPVLib.observeProperty(property = "time-pos", format = MpvFormat.MPV_FORMAT_DOUBLE)
        listOf("duration", "video-params", "track-list", "aid", "sid").forEach { property ->
            MPVLib.observeProperty(property = property, format = MpvFormat.MPV_FORMAT_NONE)
        }
    }

    override fun play() {
        if (released) return
        if (state.value.mediaStatus == PlayerMediaStatus.Opening) {
            mutableState.update { it.copy(playWhenReady = true) }
            return
        }
        if (state.value.mediaStatus != PlayerMediaStatus.Ready) return
        val allowed = audioFocus.acquire()
        MPVLib.setPropertyBoolean(property = "pause", value = !allowed)
        refreshTransport()
    }

    override fun pause() {
        if (released) return
        mutableState.update { it.copy(playWhenReady = false) }
        if (nativeReady) MPVLib.setPropertyBoolean(property = "pause", value = true)
        audioFocus.release()
    }

    override fun seekTo(
        positionMillis: Long,
    ) {
        if (released || state.value.mediaStatus != PlayerMediaStatus.Ready) return
        val session = activeSession ?: return
        val target = positionMillis.coerceIn(
            minimumValue = 0,
            maximumValue = mediaProperties.value?.durationMillis ?: Long.MAX_VALUE,
        )
        mutablePosition.value = target
        if (session.seeking) {
            session.queuedSeek = target
        } else {
            startSeek(session = session, positionMillis = target)
        }
    }

    private fun startSeek(
        session: Session,
        positionMillis: Long,
    ) {
        session.seekGeneration++
        val generation = session.seekGeneration
        session.seeking = true
        session.seekObserved = false
        val result = MPVLib.commandNode("seek", (positionMillis / 1000.0).toString(), "absolute+exact")
        if (result == null || MPVLib.getPropertyBoolean(property = "seekable") == false) {
            completeSeek(session = session)
        } else {
            session.seekTimeout = ownerScope.launch {
                delay(timeMillis = 5_000)
                if (nativeReady && activeSession === session && session.seekGeneration == generation) {
                    completeSeek(session = session)
                }
            }
        }
    }

    private fun completeSeek(
        session: Session,
    ) {
        if (!session.seeking) return
        session.seeking = false
        session.seekObserved = false
        session.seekTimeout?.cancel()
        val next = session.queuedSeek
        session.queuedSeek = null
        if (next != null) {
            startSeek(session = session, positionMillis = next)
            return
        }
        mutablePosition.value = ((MPVLib.getPropertyDouble(property = "time-pos") ?: 0.0) * 1000)
            .toLong().coerceAtLeast(minimumValue = 0)
        refreshTransport()
        eventChannel.trySend(element = PlayerEvent.SeekCompleted(positionMillis = mutablePosition.value))
    }

    private fun stopNative() {
        activeSession?.seekTimeout?.cancel()
        activeSession = null
        audioFocus.release()
        metadata.clear()
        if (nativeReady) MPVLib.command("stop")
    }

    override fun close() {
        if (released) return
        released = true
        mutableState.value = PlayerState(mediaStatus = PlayerMediaStatus.Released)
        openingJob?.cancel()
        releaseNative()
        nativeLifetime?.cancel()
        eventChannel.close()
    }

    private fun releaseNative() {
        stopNative()
        if (!nativeReady) return
        detachNativeSurface()
        nativeReady = false
        MPVLib.removeObserver(o = observer)
    }

    fun attachSurface(
        value: Surface,
        width: Int,
        height: Int,
    ) {
        if (released) return
        surfaceWidth = width
        surfaceHeight = height
        surface.value = value
        attachAvailableSurface()
    }

    fun detachSurface(
        value: Surface,
    ) {
        if (surface.value !== value) return
        surface.value = null
        if (nativeReady) detachNativeSurface()
    }

    private fun attachAvailableSurface() {
        val available = surface.value ?: return
        if (!nativeReady || !available.isValid) return
        if (attachedSurface !== available) {
            detachNativeSurface()
            MPVLib.attachSurface(surface = available)
            attachedSurface = available
            MPVLib.setPropertyString(property = "force-window", value = "yes")
            MPVLib.setPropertyString(property = "vo", value = "gpu")
        }
        MPVLib.setPropertyString(property = "android-surface-size", value = "${surfaceWidth}x$surfaceHeight")
        applyAspect()
    }

    private fun detachNativeSurface() {
        if (attachedSurface == null) return
        MPVLib.setPropertyString(property = "vo", value = "null")
        MPVLib.setPropertyString(property = "force-window", value = "no")
        MPVLib.detachSurface()
        attachedSurface = null
    }

    private fun applyAspect() {
        if (!nativeReady) return
        MPVLib.setPropertyDouble(
            property = "panscan",
            value = if (aspectMode.value == PlayerAspectRatio.CROP) 1.0 else 0.0,
        )
        MPVLib.setPropertyBoolean(
            property = "keepaspect",
            value = aspectMode.value != PlayerAspectRatio.STRETCH,
        )
    }

    private fun refreshTransport() {
        if (!nativeReady || state.value.mediaStatus != PlayerMediaStatus.Ready) return
        val playing = MPVLib.getPropertyBoolean(property = "pause") == false
        val buffering = MPVLib.getPropertyBoolean(property = "paused-for-cache") == true
        mutableState.update { it.copy(playWhenReady = playing, isBuffering = buffering) }
    }

    private fun notifyEnded() {
        if (released || state.value.mediaStatus == PlayerMediaStatus.Ended) return
        mutableState.value = PlayerState(mediaStatus = PlayerMediaStatus.Ended)
        audioFocus.release()
        eventChannel.trySend(
            element = PlayerEvent.MediaEnded(
                finalPositionMillis = currentPositionMillis.value,
                durationMillis = mediaProperties.value?.durationMillis,
            ),
        )
    }

    private fun notifyError() {
        if (released || state.value.mediaStatus == PlayerMediaStatus.Error) return
        mutableState.value = PlayerState(mediaStatus = PlayerMediaStatus.Error)
        stopNative()
        eventChannel.trySend(element = PlayerEvent.ErrorOccurred)
    }

    private fun readProperties() = PlayerMediaProperties(
        durationMillis = MPVLib.getPropertyDouble(property = "duration")
            ?.takeIf { it.isFinite() && it > 0 }?.let { (it * 1000).toLong() },
        videoWidth = MPVLib.getPropertyInt(property = "dwidth")?.takeIf { it > 0 },
        videoHeight = MPVLib.getPropertyInt(property = "dheight")?.takeIf { it > 0 },
    )

    private fun setStringList(
        property: String,
        values: List<String>,
    ) {
        MPVLib.setPropertyNode(
            property = property,
            node = MPVNode.ArrayNode(value = values.map { MPVNode.StringNode(value = it) }.toTypedArray()),
        )
    }

    private companion object {
        val nativeOwner = Mutex()
    }
}
