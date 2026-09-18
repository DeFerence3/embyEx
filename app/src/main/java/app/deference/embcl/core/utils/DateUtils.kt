package app.deference.embcl.core.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlin.time.Instant

val LocalDateTime.Companion.FORMAT: DateTimeFormat<LocalDateTime>
	get() = LocalDateTime.Format {
		day()
		char(' ')
		monthName(MonthNames.ENGLISH_ABBREVIATED)
		char(' ')
		year()
	}
val Instant.Companion.FORMAT: DateTimeFormat<DateTimeComponents>
	get() = DateTimeComponents.Format {
		day()
		char(' ')
		monthName(MonthNames.ENGLISH_ABBREVIATED)
		char(' ')
		year()
	}

fun LocalDateTime.formatToString(
	dateFormat: DateTimeFormat<LocalDateTime> = LocalDateTime.FORMAT,
): String {
	return this.format(dateFormat)
}

fun Instant.formatToString(
	dateFormat: DateTimeFormat<DateTimeComponents> = Instant.FORMAT,
): String {
	return this.format(dateFormat)
}