package app.deference.embcl.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

interface Screen : NavKey {
	
	@Composable
	fun Content()
}
