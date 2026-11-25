package id.ac.loopy.feature.pickup

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.R
import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.ui.components.LoopyAsset
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyCard
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyIntro
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.LoopyTextField
import id.ac.loopy.ui.components.PickupSuccessDialog
import id.ac.loopy.ui.theme.LoopyTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** S07 | F06 — Validasi Pengajuan Pickup & Simpan ID Unik Pending Review. */
@Composable
fun PickupRequestScreen(
    viewModel: PickupRequestViewModel?,
    onBack: () -> Unit,
    onRecheck: () -> Unit,
    onGoToMyDonations: () -> Unit,
    loadPrivatePhoto: (suspend (String) -> ByteArray)? = null,
) {
    val state = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: PickupRequestUiState(
        checkLoadError = "Layanan pengajuan pickup belum terhubung.",
    )
    PickupRequestContent(
        state = state,
        onBack = onBack,
        onRetryLoadCheck = { viewModel?.retryLoadCheck() },
        onRecheck = onRecheck,
        onPhoneChange = { viewModel?.onPhoneNumberChange(it) },
        onAddressChange = { viewModel?.onPickupAddressChange(it) },
        onCityChange = { viewModel?.onCityChange(it) },
        onIncrementQuantity = { viewModel?.incrementQuantity() },
        onDecrementQuantity = { viewModel?.decrementQuantity() },
        onDateSelected = { viewModel?.onPreferredDateChange(it) },
        onTimeSelected = { viewModel?.onPreferredTimeChange(it) },
        onNotesChange = { viewModel?.onNotesChange(it) },
        onCleanAndSafeChange = { viewModel?.onCleanAndSafeChange(it) },
        onSubmit = { viewModel?.submit() },
        onGoToMyDonations = {
            viewModel?.dismissSuccessDialog()
            onGoToMyDonations()
        },
        loadPrivatePhoto = loadPrivatePhoto,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PickupRequestContent(
    state: PickupRequestUiState,
    onBack: () -> Unit,
    onRetryLoadCheck: () -> Unit,
    onRecheck: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onCleanAndSafeChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoToMyDonations: () -> Unit,
    loadPrivatePhoto: (suspend (String) -> ByteArray)? = null,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LoopyScreen(
        title = "Ajukan penjemputan",
        onBack = onBack,
        contentSpacing = 16,
        footer = if (state.canInteractWithForm) {
            {
                state.submitError?.let { error ->
                    LoopyInfo("Perbaiki input", error, isError = true)
                }
                LoopyButton(
                    label = if (state.isSubmitting) "Mengirim pengajuan…" else "Ajukan Penjemputan",
                    enabled = !state.isSubmitting,
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                )
            }
        } else {
            null
        },
    ) {
        LoopyIntro(
            title = "Atur jadwal\npenjemputanmu.",
            description = "Lengkapi alamat dan preferensi jadwal agar admin dapat menjadwalkan kurir.",
        )

        if (state.isDemoMode) {
            LoopyInfo(
                title = "Mode demo lokal",
                description = "Pengajuan valid akan disimpan ke Room lokal sebagai Pending Review dan muncul di My Donations.",
            )
        }

        when {
            state.isLoadingCheck -> {
                LoopyCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Memuat data hasil pemeriksaan…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            state.checkLoadError != null -> {
                LoopyInfo("Belum dapat mengajukan pickup", state.checkLoadError, isError = true)
                if (state.check == null) {
                    LoopyButton("Muat ulang", onRetryLoadCheck)
                }
                LoopyButton("Kembali ke pemeriksaan AI", onRecheck, secondary = true)
            }
            state.check != null -> {
                EligibleCheckSummaryCard(
                    check = state.check,
                    donorName = state.donorName,
                    isDemoMode = state.isDemoMode,
                    loadPrivatePhoto = loadPrivatePhoto,
                )

                LoopyCard(spacing = 14) {
                    Text("Data penjemputan", style = MaterialTheme.typography.titleLarge)

                    LoopyTextField(
                        label = "Nomor telepon",
                        value = state.phoneNumber,
                        onValueChange = onPhoneChange,
                        placeholder = "Contoh: 081234567890",
                        error = state.fieldErrors["phoneNumber"],
                        keyboardType = KeyboardType.Phone,
                        enabled = !state.isSubmitting,
                    )

                    LoopyTextField(
                        label = "Alamat penjemputan",
                        value = state.pickupAddress,
                        onValueChange = onAddressChange,
                        placeholder = "Nama jalan, nomor rumah, patokan",
                        error = state.fieldErrors["pickupAddress"],
                        singleLine = false,
                        enabled = !state.isSubmitting,
                    )

                    LoopyTextField(
                        label = "Kota / Area",
                        value = state.city,
                        onValueChange = onCityChange,
                        placeholder = "Contoh: Malang",
                        error = state.fieldErrors["city"],
                        enabled = !state.isSubmitting,
                    )
                }

                LoopyCard(spacing = 14) {
                    Text("Jumlah & preferensi jadwal", style = MaterialTheme.typography.titleLarge)

                    QuantityStepper(
                        quantity = state.quantity,
                        error = state.fieldErrors["quantity"],
                        enabled = !state.isSubmitting,
                        onIncrement = onIncrementQuantity,
                        onDecrement = onDecrementQuantity,
                    )

                    PreferredDateSelector(
                        isoDate = state.preferredDate,
                        error = state.fieldErrors["preferredDate"],
                        enabled = !state.isSubmitting,
                        onOpenPicker = {
                            val zone = TimeZone.getTimeZone("Asia/Jakarta")
                            val cal = Calendar.getInstance(zone)
                            if (state.preferredDate.isNotBlank()) {
                                val parts = state.preferredDate.split("-")
                                if (parts.size == 3) {
                                    parts[0].toIntOrNull()?.let { cal.set(Calendar.YEAR, it) }
                                    parts[1].toIntOrNull()?.let { cal.set(Calendar.MONTH, it - 1) }
                                    parts[2].toIntOrNull()?.let { cal.set(Calendar.DAY_OF_MONTH, it) }
                                }
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formatted = String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        year,
                                        month + 1,
                                        dayOfMonth,
                                    )
                                    onDateSelected(formatted)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                            ).apply {
                                datePicker.minDate = Calendar.getInstance(zone).apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }.show()
                        },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Preferensi waktu",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PreferredTimeSlots.forEach { slot ->
                                val selected = state.preferredTime == slot
                                FilterChip(
                                    selected = selected,
                                    enabled = !state.isSubmitting,
                                    onClick = { onTimeSelected(slot) },
                                    label = { Text(slot) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                        state.fieldErrors["preferredTime"]?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            "Jadwal pilihanmu bersifat preferensi. Jadwal final dikonfirmasi oleh admin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    LoopyTextField(
                        label = "Catatan tambahan (opsional)",
                        value = state.notes,
                        onValueChange = onNotesChange,
                        placeholder = "Contoh: Pakaian sudah dicuci dan dilipat rapi",
                        error = state.fieldErrors["notes"],
                        singleLine = false,
                        enabled = !state.isSubmitting,
                    )
                }

                CleanConfirmationCard(
                    confirmed = state.cleanAndSafeConfirmed,
                    error = state.fieldErrors["cleanAndSafeConfirmed"],
                    enabled = !state.isSubmitting,
                    onCheckedChange = onCleanAndSafeChange,
                )
            }
        }
    }

    state.submittedDonation?.let { summary ->
        PickupSuccessDialog(
            summary = summary,
            onGoToMyDonations = onGoToMyDonations,
        )
    }
}

@Composable
private fun EligibleCheckSummaryCard(
    check: ClothingCheck,
    donorName: String,
    isDemoMode: Boolean,
    loadPrivatePhoto: (suspend (String) -> ByteArray)?,
) {
    val isPreview = LocalInspectionMode.current
    val loader = loadPrivatePhoto
    val canFetchPhoto = !isPreview && !isDemoMode && loader != null && check.checkId.isNotBlank()
    val bitmap by key(check.checkId, canFetchPhoto) {
        produceState<ImageBitmap?>(null) {
            if (canFetchPhoto) {
                value = try {
                    val bytes = checkNotNull(loader)(check.checkId)
                    withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    LoopyCard(color = MaterialTheme.colorScheme.primaryContainer, padding = 16, spacing = 10) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                val decoded = bitmap
                if (decoded != null) {
                    Image(
                        bitmap = decoded,
                        contentDescription = "Foto pakaian lolos pemeriksaan",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    LoopyAsset("shirt", size = 28)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check_result_verified),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Berpotensi layak",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "Jenis: ${check.clothingType.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Donatur: $donorName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    error: String?,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Jumlah pakaian",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onDecrement,
                enabled = enabled && quantity > 1,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Kurangi jumlah pakaian" },
                shape = CircleShape,
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = "$quantity pakaian",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onIncrement,
                enabled = enabled && quantity < 100,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Tambah jumlah pakaian" },
                shape = CircleShape,
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PreferredDateSelector(
    isoDate: String,
    error: String?,
    enabled: Boolean,
    onOpenPicker: () -> Unit,
) {
    val borderColor = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Tanggal penjemputan",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onOpenPicker)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoopyAsset("calendar", size = 20)
            Text(
                text = if (isoDate.isBlank()) {
                    "Pilih tanggal penjemputan"
                } else {
                    PickupRequestValidator.formatReadableDate(isoDate)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isoDate.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                "Ubah",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CleanConfirmationCard(
    confirmed: Boolean,
    error: String?,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(20.dp),
                )
                .toggleable(
                    value = confirmed,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (confirmed) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (confirmed) {
                    Icon(
                        painterResource(R.drawable.ic_check_result_confirmation),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                "Saya memastikan pakaian bersih, kering, dan aman digunakan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, showBackground = true)
@Preview(widthDp = 320, heightDp = 640, fontScale = 1.3f, showBackground = true)
@Composable
private fun PickupRequestScreenPreview() {
    LoopyTheme {
        PickupRequestContent(
            state = PickupRequestUiState(
                donorName = "Neva",
                check = ClothingCheck(
                    checkId = "check-1",
                    userId = "user-1",
                    clothingPhoto = "",
                    clothingType = ClothingType.TSHIRT,
                    aiResult = AiResult.POTENTIALLY_ELIGIBLE,
                    aiDescription = "Kaus terlihat bersih dan utuh.",
                    createdAt = "",
                ),
                phoneNumber = "081234567890",
                pickupAddress = "Jl. Veteran No. 12",
                city = "Malang",
                quantity = 3,
                preferredDate = "2026-11-28",
                preferredTime = PreferredTimeSlots.first(),
                cleanAndSafeConfirmed = true,
            ),
            onBack = {},
            onRetryLoadCheck = {},
            onRecheck = {},
            onPhoneChange = {},
            onAddressChange = {},
            onCityChange = {},
            onIncrementQuantity = {},
            onDecrementQuantity = {},
            onDateSelected = {},
            onTimeSelected = {},
            onNotesChange = {},
            onCleanAndSafeChange = {},
            onSubmit = {},
            onGoToMyDonations = {},
        )
    }
}
