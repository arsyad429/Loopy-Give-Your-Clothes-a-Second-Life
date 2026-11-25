package id.ac.loopy.di

import android.content.Context
import androidx.room.Room
import id.ac.loopy.BuildConfig
import id.ac.loopy.data.local.LoopyDatabase
import id.ac.loopy.data.local.CompletionDemoDatabase
import id.ac.loopy.data.repository.CompletionDemoRepository
import id.ac.loopy.data.local.preferences.UserPreferencesDataSource
import id.ac.loopy.data.remote.api.AdminApi
import id.ac.loopy.data.remote.api.AuthApi
import id.ac.loopy.data.remote.api.ClothingCheckApi
import id.ac.loopy.data.remote.api.DonationApi
import id.ac.loopy.data.remote.api.HealthApi
import id.ac.loopy.data.remote.api.HomeApi
import id.ac.loopy.data.repository.AdminRepositoryImpl
import id.ac.loopy.data.repository.AuthRepositoryImpl
import id.ac.loopy.data.repository.ClothingCheckRepositoryImpl
import id.ac.loopy.data.repository.DonationRepositoryImpl
import id.ac.loopy.data.repository.FoundationRepository
import id.ac.loopy.data.repository.RemoteHomeRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency injection sesuai PAPB; satu instance DB/DataStore sepanjang proses. */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context.applicationContext, LoopyDatabase::class.java, "loopy.db").build()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authRepository = AuthRepositoryImpl(retrofit.create(AuthApi::class.java))

    val clothingCheckRepository = ClothingCheckRepositoryImpl(
        context,
        retrofit.create(ClothingCheckApi::class.java),
        authRepository,
    )

    internal val adminRepository = AdminRepositoryImpl(retrofit.create(AdminApi::class.java), authRepository)

    val donationRepository = DonationRepositoryImpl(
        retrofit.create(DonationApi::class.java),
        authRepository,
    )

    internal val homeRepository = RemoteHomeRepository(retrofit.create(HomeApi::class.java), authRepository)

    val foundationRepository = FoundationRepository(
        UserPreferencesDataSource(context),
        database.pickupDraftDao(),
        retrofit.create(HealthApi::class.java),
    )
    val completionDemoRepository by lazy {
        check(BuildConfig.DEBUG) { "Demo data is available only in debug builds." }
        CompletionDemoRepository(Room.databaseBuilder(context.applicationContext, CompletionDemoDatabase::class.java, "loopy-completion-demo.db").build())
    }
}
