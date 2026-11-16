package id.ac.loopy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.ac.loopy.data.repository.FoundationRepository
import id.ac.loopy.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplateUiState(
    val darkTheme: Boolean = false,
    val draftCount: Int = 0,
    val health: String = "Belum diperiksa",
    val busy: Boolean = false,
)

class TemplateViewModel(private val repository: FoundationRepository) : ViewModel() {
    private val message = MutableStateFlow("Belum diperiksa")
    private val busy = MutableStateFlow(false)
    val state = combine(repository.darkTheme, repository.localDrafts, message, busy) { dark, drafts, health, working ->
        TemplateUiState(dark, drafts.size, health, working)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplateUiState())

    fun setDarkTheme(enabled: Boolean) = launchAction { repository.setDarkTheme(enabled) }
    fun saveDemoDraft() = launchAction { repository.saveDemoDraft() }
    fun clearDemoDrafts() = launchAction { repository.clearDemoDrafts() }
    fun refreshHealth() {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            message.value = "Menghubungkan…"
            try { message.value = repository.checkHealth() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message.value = "Backend belum terhubung. Periksa Docker dan URL API, lalu coba lagi." }
            finally { busy.value = false }
        }
    }
    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message.value = "Penyimpanan lokal gagal. Coba lagi." }
        }
    }
    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TemplateViewModel(container.foundationRepository) }
        }
    }
}
