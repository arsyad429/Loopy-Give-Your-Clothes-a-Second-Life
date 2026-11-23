package id.ac.loopy.domain.repository

import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import kotlinx.coroutines.flow.Flow

interface ClothingCheckRepository {

    /**
     * Menganalisis foto dengan jenis pakaian yang dipilih.
     * Kegagalan diteruskan sebagai exception kepada pemanggil.
     */
    suspend fun analyzeClothing(
        photoUri: String,
        clothingType: ClothingType
    ): ClothingCheck

    /**
     * Mengambil pemeriksaan milik pengguna yang sedang login.
     * Kegagalan atau data tidak ditemukan diteruskan sebagai exception.
     */
    suspend fun getCheckById(
        checkId: String
    ): ClothingCheck

    /**
     * Mengamati hasil pemeriksaan dari cache lokal.
     * null berarti belum ada data di cache, bukan kegagalan request.
     */
    fun observeCheck(
        checkId: String
    ): Flow<ClothingCheck?>
}
