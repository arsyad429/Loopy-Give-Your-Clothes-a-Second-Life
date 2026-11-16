package id.ac.loopy.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.loopyPreferences by preferencesDataStore(name = "loopy_preferences")

class UserPreferencesDataSource(context: Context) {
    private val store = context.applicationContext.loopyPreferences
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    val darkTheme = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { it[darkThemeKey] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        store.edit { it[darkThemeKey] = enabled }
    }
    // TODO: preferensi onboarding/filter. Password, saldo, role otoritatif bukan preferensi lokal.
}
