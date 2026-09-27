package app.deference.embcl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.deference.embycl.EmbyExApp
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.theme.EmbympvTheme

class MainActivity : ComponentActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			EmbympvTheme {
				val isLoggedIn by Session.isLoggedInState.collectAsState()
				EmbyExApp(isLoggedIn)
			}
		}
	}
}

