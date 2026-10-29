# mpv-android 1.0.0 resolves these entry points by name from libplayer.
# Missing callbacks cause a native abort in minified release builds.
-keep class is.xyz.mpv.MPVLib {
    native <methods>;
    public static void eventProperty(...);
    public static void event(int, is.xyz.mpv.MPVNode);
    public static void logMessage(java.lang.String, int, java.lang.String);
}

# JNI constructs and reads node variants; unused Kotlin helpers can still be removed.
-keep class is.xyz.mpv.MPVNode
-keep class is.xyz.mpv.MPVNode$* {
    <init>(...);
    *** value;
    *** getValue();
    public static ** INSTANCE;
}
