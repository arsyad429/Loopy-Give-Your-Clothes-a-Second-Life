package id.ac.loopy.feature.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.R
import id.ac.loopy.domain.model.DonationStatusHistory
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.RejectRequestDialog
import id.ac.loopy.ui.components.ScheduleConfirmDialog
import id.ac.loopy.ui.components.StatusBadge
import id.ac.loopy.ui.components.LoopyCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val PickupZone = TimeZone.getTimeZone("Asia/Jakarta")
private val Indonesian = Locale.forLanguageTag("id-ID")
private const val PastPickupMessage = "Jadwal ini sudah lewat. Pilih tanggal atau jam lain."

@Composable
internal fun AdminDonationDetailScreen(
    model: AdminDonationDetailViewModel,
    loadPhoto: suspend (String) -> ByteArray,
    onBack: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val donation = state.donation
    if (donation == null) {
        LoopyScreen(title = "Verifikasi donasi", onBack = onBack) {
            if (state.busy) CircularProgressIndicator()
            state.error?.let {
                LoopyInfo("Detail belum tersedia", it, isError = true)
                LoopyButton("Muat ulang detail", model::refresh, outlined = true)
            }
        }
        return
    }

    val checkId = donation.clothingCheck.checkId
    val photo by produceState<ImageBitmap?>(null, checkId) {
        value = try {
            val bytes = loadPhoto(checkId)
            withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    val review = remember(donation) { donation.toReview() }
    AdminDonationDetailScreen(
        review = review,
        onBack = onBack,
        onSchedule = model::schedule,
        onReject = model::reject,
        photo = photo,
        onAdvance = model::advance,
        busy = state.busy,
        completionMessage = state.message,
        completionError = state.error,
        history = donation.history,
        live = true,
        scheduleDraft = state.scheduleDraft,
        savingDraft = state.savingDraft,
        draftError = state.draftError,
        onDraftChange = model::updateScheduleDraft,
        onDraftRetry = {
            if (state.draftError == PickupScheduleDraftError.LOAD) model.refresh()
            else model.updateScheduleDraft(state.scheduleDraft)
        },
    )
}

@Composable
internal fun AdminDonationDetailScreen(
    review: DonationReview,
    onBack: () -> Unit,
    onSchedule: (Long) -> Unit,
    onReject: (String) -> Unit,
    photo: ImageBitmap? = null,
    onAdvance: ((DonationStatus) -> Unit)? = null,
    busy: Boolean = false,
    completionMessage: String? = null,
    completionError: String? = null,
    history: List<DonationStatusHistory> = emptyList(),
    live: Boolean = false,
    scheduleDraft: PickupScheduleSelection? = null,
    savingDraft: Boolean = false,
    draftError: PickupScheduleDraftError? = null,
    onDraftChange: ((PickupScheduleSelection) -> Unit)? = null,
    onDraftRetry: (() -> Unit)? = null,
) {
    var advanceOpen by rememberSaveable(review.id) { mutableStateOf(false) }
    val nextStatus = when (review.status) {
        DonationStatus.PICKUP_SCHEDULED -> DonationStatus.PICKED_UP
        DonationStatus.PICKED_UP -> DonationStatus.COMPLETED
        else -> null
    }
    LaunchedEffect(review.status) { advanceOpen = false }
    var demoDate by rememberSaveable(review.id) { mutableStateOf<Long?>(null) }
    var demoMinutes by rememberSaveable(review.id) { mutableStateOf<Int?>(null) }
    val selection = scheduleDraft ?: PickupScheduleSelection(demoDate, demoMinutes)
    val date = selection.dateMillis
    val minutes = selection.minutesOfDay
    var scheduleOpen by rememberSaveable(review.id) { mutableStateOf(false) }
    var rejectionOpen by rememberSaveable(review.id) { mutableStateOf(false) }
    var scheduleError by rememberSaveable(review.id) { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val pending = review.status == DonationStatus.PENDING_REVIEW
    val pickupAt = if (date != null && minutes != null) date + minutes * 60_000L else null
    val scheduleEnabled = !busy && !savingDraft

    fun updateSelection(updated: PickupScheduleSelection) {
        if (onDraftChange == null) {
            demoDate = updated.dateMillis
            demoMinutes = updated.minutesOfDay
        } else {
            onDraftChange(updated)
        }
        scheduleError = null
    }

    LoopyScreen(
        title = "Verifikasi donasi",
        onBack = onBack,
        footer = if (pending) {
            {
                if (scheduleError != null) {
                    Text(
                        text = scheduleError.orEmpty(),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                LoopyButton("Setujui & jadwalkan", enabled = scheduleEnabled, onClick = {
                    scheduleError = when {
                        date == null && minutes == null -> "Pilih tanggal dan jam penjemputan terlebih dahulu."
                        date == null -> "Pilih tanggal penjemputan terlebih dahulu."
                        pickupAt == null -> "Pilih jam penjemputan terlebih dahulu."
                        pickupAt <= System.currentTimeMillis() -> PastPickupMessage
                        else -> null
                    }
                    scheduleOpen = scheduleError == null
                })

                LoopyButton("Tolak pengajuan", onClick = { rejectionOpen = true }, enabled = scheduleEnabled, outlined = true)
            }
        } else if (onAdvance != null && nextStatus != null) {
            {
                LoopyButton(
                    if (nextStatus == DonationStatus.COMPLETED) "Selesaikan donasi (+10 poin)" else "Konfirmasi sudah dijemput",
                    onClick = { advanceOpen = true }, enabled = !busy,
                )
            }
        } else {
            null
        },
    ) {
        Text("#${review.id} · Diajukan ${review.submittedAt}", style = MaterialTheme.typography.bodySmall)
        StatusBadge(review.status)
        completionMessage?.let { LoopyInfo("Berhasil", it) }
        completionError?.let { LoopyInfo("Belum berhasil", it, isError = true) }
        if (review.status == DonationStatus.COMPLETED && onAdvance != null) {
            LoopyInfo("Donasi selesai", "10 poin telah ditambahkan satu kali untuk pengajuan ini.")
        }

        if (busy && live) Text("Menyimpan perubahan…", style = MaterialTheme.typography.bodySmall)
        ClothingPhoto(review.clothing, photo, live)
        LoopyInfo(review.assessment, review.assessmentDescription)

        Text("Data pengajuan", style = MaterialTheme.typography.titleLarge)

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReviewValue("Donatur", review.donor)
            ReviewValue("Jumlah", "${review.quantity} pakaian")
            ReviewValue("Telepon", review.phone)
            ReviewValue("Alamat", "${review.address}, ${review.city}")
            ReviewValue("Preferensi penjemputan", review.preference)
            ReviewValue("Catatan", review.notes.ifBlank { "Tidak ada catatan" })
        }

        review.schedule?.let { LoopyInfo("Jadwal final", it) }
        review.rejectionReason?.let { LoopyInfo("Alasan penolakan", it, isError = true) }

        if (history.isNotEmpty()) {
            Text("Riwayat status", style = MaterialTheme.typography.titleLarge)
            history.forEach { event ->
                StatusBadge(event.newStatus)
                Text(formatAdminTimestamp(event.updatedAt), style = MaterialTheme.typography.bodySmall)
            }
        }

        if (pending) {
            Text("Jadwal final", style = MaterialTheme.typography.titleLarge)

            LoopyButton(
                label = if (date == null) "Pilih tanggal" else formatPickupDate(date),
                enabled = scheduleEnabled,
                onClick = {
                    selectPickupDate(context, date ?: 0L) {
                        updateSelection(selection.copy(dateMillis = it))
                    }
                },
                outlined = true,
            )

            LoopyButton(
                label = if (minutes == null) "Pilih jam (WIB)" else String.format(Indonesian, "%02d.%02d WIB", minutes / 60, minutes % 60),
                enabled = scheduleEnabled,
                onClick = {
                    val initial = minutes ?: 9 * 60

                    TimePickerDialog(context, { _, hour, minute ->
                        updateSelection(selection.copy(minutesOfDay = hour * 60 + minute))
                    }, initial / 60, initial % 60, true).show()
                },
                outlined = true,
            )

            if (onDraftChange != null) {
                when {
                    savingDraft -> Text("Menyimpan draft…", style = MaterialTheme.typography.bodySmall)
                    draftError != null -> {
                        LoopyInfo("Draft jadwal", draftError.message, isError = true)
                        if (onDraftRetry != null) {
                            LoopyButton("Coba lagi", onDraftRetry, enabled = scheduleEnabled, outlined = true)
                        }
                    }
                    date != null || minutes != null -> Text(
                        "Draft tersimpan di perangkat ini.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (date != null || minutes != null) {
                    LoopyButton("Hapus draft", { updateSelection(PickupScheduleSelection()) }, enabled = scheduleEnabled, outlined = true)
                }
            }
            Text("Jadwal final wajib diisi untuk menyetujui pengajuan.", style = MaterialTheme.typography.bodySmall)
        }

        if (pending && scheduleOpen && pickupAt != null) {
            ScheduleConfirmDialog(
                summary = "#${review.id} · ${review.quantity} pakaian",
                schedule = formatPickupSchedule(pickupAt),
                address = "${review.address}, ${review.city}",
                onDismiss = { scheduleOpen = false },
                onConfirm = {
                    scheduleOpen = false
                    if (pickupAt > System.currentTimeMillis()) {
                        onSchedule(pickupAt)
                    } else {
                        scheduleError = PastPickupMessage
                    }
                },
            )
        }

        if (pending && rejectionOpen) {
            RejectRequestDialog(
                onDismiss = { rejectionOpen = false },
                onConfirm = {
                    rejectionOpen = false
                    onReject(it)
                },
            )
        }
        if (advanceOpen && nextStatus != null && onAdvance != null) {
            Dialog(onDismissRequest = { if (!busy) advanceOpen = false }) {
                LoopyCard {
                    Text(if (nextStatus == DonationStatus.COMPLETED) "Pakaian sudah diterima?" else "Pakaian sudah dijemput?", style = MaterialTheme.typography.headlineMedium)
                    Text(if (nextStatus == DonationStatus.COMPLETED) "Konfirmasikan penerimaan #${review.id}. Donasi menjadi Selesai dan donatur mendapat 10 poin satu kali." else "Konfirmasikan penjemputan #${review.id} hanya setelah pakaian benar-benar dijemput.")
                    completionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    LoopyButton(if (busy) "Menyimpan…" else "Ya, konfirmasi", { onAdvance(nextStatus) }, enabled = !busy)
                    LoopyButton("Batal", { advanceOpen = false }, enabled = !busy, secondary = true)
                }
            }
        }
    }
}

@Composable
private fun ClothingPhoto(clothing: String, photo: ImageBitmap?, live: Boolean) {
    val colors = MaterialTheme.colorScheme

    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = "Foto pakaian: $clothing",
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)
                .background(colors.primaryContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clothing_shirt),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(48.dp),
                )

                Text(if (live) "$clothing · Foto belum tersedia" else "$clothing · Ilustrasi", color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ReviewValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun selectPickupDate(context: Context, current: Long, onSelected: (Long) -> Unit) {
    val today = Calendar.getInstance(PickupZone)
    val selected = Calendar.getInstance(PickupZone).apply {
        if (current > 0) timeInMillis = current
    }

    val picker = DatePickerDialog(
        context,
        { _, year, month, day ->
            val jakartaDate = Calendar.getInstance(PickupZone).apply {
                clear()
                set(year, month, day)
            }
            onSelected(jakartaDate.timeInMillis)
        },
        selected.get(Calendar.YEAR),
        selected.get(Calendar.MONTH),
        selected.get(Calendar.DAY_OF_MONTH),
    )

    picker.datePicker.minDate = Calendar.getInstance().apply {
        clear()
        set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis

    picker.show()
}

internal fun formatPickupSchedule(at: Long): String = "${formatPickupDate(at)} · ${formatPickupTime(at)}"

private fun formatPickupDate(at: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy", Indonesian).apply { timeZone = PickupZone }.format(Date(at))

private fun formatPickupTime(at: Long): String =
    SimpleDateFormat("HH.mm 'WIB'", Indonesian).apply { timeZone = PickupZone }.format(Date(at))
