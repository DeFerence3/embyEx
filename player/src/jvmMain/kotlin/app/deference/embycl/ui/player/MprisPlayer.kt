package app.deference.embycl.ui.player

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.TypeRef
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusProperty.Access
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
internal interface MprisPlayer : DBusInterface {
    fun next()

    fun previous()

    fun pause()

    fun playPause()

    fun stop()

    fun play()

    fun seek(offset: Long)

    fun setPosition(trackId: DBusPath, position: Long)

    fun openUri(uri: String)

    @DBusBoundProperty(access = Access.READ, type = String::class)
    fun getPlaybackStatus(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Metadata::class)
    fun getMetadata(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Long::class)
    fun getPosition(): Variant<*>

    @DBusBoundProperty(type = Double::class)
    fun getRate(): Variant<*>

    @DBusBoundProperty(type = Double::class)
    fun setRate(rate: Double)

    @DBusBoundProperty(access = Access.READ, type = Double::class)
    fun getVolume(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Double::class)
    fun getMinimumRate(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Double::class)
    fun getMaximumRate(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanGoNext(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanGoPrevious(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanPlay(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanPause(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanSeek(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanControl(): Variant<*>

    interface Metadata : TypeRef<MutableMap<String, Variant<*>>>

    class Seeked(
        path: String,
        position: Long,
    ) : DBusSignal(path, position)
}
