package app.deference.embcl.core.utils

fun <T> T?.or(predicate: () -> T): T = this ?: predicate()

fun <T> T?.or(predicate: () -> String): String = this?.toString() ?: predicate()