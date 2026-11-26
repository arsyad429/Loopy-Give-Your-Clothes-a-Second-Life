package id.ac.loopy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.ui.LoopyApp
import id.ac.loopy.ui.theme.LoopyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LoopyApplication).container
        setContent {
            val darkTheme = container.userPreferences.darkTheme.collectAsStateWithLifecycle(initialValue = false)
            LoopyTheme(darkTheme = darkTheme.value) {
                LoopyApp(container = container)
            }
        }
    }

    // PAPB lifecycle: amati Logcat saat rotasi, background, dan kembali dari kamera/galeri.
    override fun onStart() { super.onStart(); trace("onStart") }
    override fun onResume() { super.onResume(); trace("onResume") }
    override fun onPause() { trace("onPause"); super.onPause() }
    override fun onStop() { trace("onStop"); super.onStop() }
    override fun onDestroy() { trace("onDestroy"); super.onDestroy() }
    private fun trace(event: String) { if (BuildConfig.DEBUG) Log.d("LoopyLifecycle", event) }
}
