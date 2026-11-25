package id.ac.loopy.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.loopy.data.repository.AdminRepositoryImpl
import id.ac.loopy.domain.model.DonationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

internal class AdminDashboardViewModel(private val repository: AdminRepositoryImpl) : ViewModel() {
    private val _state = MutableStateFlow(AdminDashboardUiState())
    val state = _state.asStateFlow()
    private var request: Job? = null

    fun refresh() {
        if (request?.isActive == true) return
        _state.update { it.copy(loading = true, error = null) }
        request = viewModelScope.launch {
            try {
                val summary = repository.summary()
                _state.update { it.copy(summary = summary) }
                val pending = repository.list(DonationStatus.PENDING_REVIEW, "", null, limit = 3).items
                _state.value = AdminDashboardUiState(summary = summary, pending = pending, loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.adminMessage("Data dashboard gagal dimuat.")) }
            }
        }
    }
}

internal fun Exception.adminMessage(fallback: String): String = when (this) {
    is IOException -> "Tidak bisa terhubung. Periksa koneksi, lalu coba lagi."
    is HttpException -> when (code()) {
        401 -> "Sesi telah berakhir. Silakan masuk kembali."
        403 -> "Akun ini tidak memiliki akses admin."
        404 -> "Pengajuan tidak ditemukan."
        422 -> "Periksa kembali tanggal, jam, atau alasan yang diisi."
        429 -> "Layanan sedang sibuk. Coba lagi sebentar."
        else -> fallback
    }
    else -> fallback
}
