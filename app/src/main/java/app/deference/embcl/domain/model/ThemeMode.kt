package app.deference.embcl.domain.model

enum class ThemeMode {
	System, Light, Dark;
	
	companion object {
		
		fun fromStoredValue(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: System
	}
}
