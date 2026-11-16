package id.ac.loopy.data.repository

import id.ac.loopy.data.local.dao.PickupDraftDao
import id.ac.loopy.data.local.entity.PickupDraftEntity
import id.ac.loopy.data.local.preferences.UserPreferencesDataSource
import id.ac.loopy.data.remote.api.HealthApi

class FoundationRepository(
    private val preferences: UserPreferencesDataSource,
    private val drafts: PickupDraftDao,
    private val api: HealthApi,
) {
    val darkTheme = preferences.darkTheme
    val localDrafts = drafts.observeDrafts()
    suspend fun setDarkTheme(enabled: Boolean) = preferences.setDarkTheme(enabled)
    suspend fun checkHealth(): String = api.health().let { "${it.status} (${it.mode})" }
    suspend fun saveDemoDraft() = drafts.upsert(PickupDraftEntity("foundation-demo", "Draft contoh tanpa data pribadi", System.currentTimeMillis()))
    suspend fun clearDemoDrafts() = drafts.clear()
}
