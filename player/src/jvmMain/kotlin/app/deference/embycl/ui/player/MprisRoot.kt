package app.deference.embycl.ui.player

import org.freedesktop.dbus.TypeRef
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusProperty.Access
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2")
internal interface MprisRoot : DBusInterface {
    fun raise()

    fun quit()

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanQuit(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getCanRaise(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = Boolean::class)
    fun getHasTrackList(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = String::class)
    fun getIdentity(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = String::class)
    fun getDesktopEntry(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = StringList::class)
    fun getSupportedUriSchemes(): Variant<*>

    @DBusBoundProperty(access = Access.READ, type = StringList::class)
    fun getSupportedMimeTypes(): Variant<*>

    interface StringList : TypeRef<MutableList<String>>
}
