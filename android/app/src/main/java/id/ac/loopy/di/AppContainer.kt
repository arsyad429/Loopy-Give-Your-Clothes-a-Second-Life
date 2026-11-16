package id.ac.loopy.di

import android.content.Context
import androidx.room.Room
import id.ac.loopy.BuildConfig
import id.ac.loopy.data.local.LoopyDatabase
import id.ac.loopy.data.local.preferences.UserPreferencesDataSource
import id.ac.loopy.data.remote.api.HealthApi
import id.ac.loopy.data.repository.FoundationRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency injection sesuai PAPB; satu instance DB/DataStore sepanjang proses. */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context.applicationContext, LoopyDatabase::class.java, "loopy.db").build()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(http)
        .addConverterFactory(GsonConverterFactory.create()).build().create(HealthApi::class.java)
    val foundationRepository = FoundationRepository(UserPreferencesDataSource(context), database.pickupDraftDao(), api)
    // TODO: wiring repository auth/check/donation/admin/reward setelah kontrak diimplementasikan.
}
