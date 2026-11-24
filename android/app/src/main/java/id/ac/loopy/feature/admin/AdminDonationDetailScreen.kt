package id.ac.loopy.feature.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.ac.loopy.R
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.ui.components.LoopyButton
import id.ac.loopy.ui.components.LoopyInfo
import id.ac.loopy.ui.components.LoopyScreen
import id.ac.loopy.ui.components.RejectRequestDialog
import id.ac.loopy.ui.components.ScheduleConfirmDialog
import id.ac.loopy.ui.components.StatusBadge
import id.ac.loopy.ui.components.LoopyCard
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
    review: DonationReview,
    onBack: () -> Unit,
    onSchedule: (Long) -> Unit,
    onReject: (String) -> Unit,
    photo: ImageBitmap? = null,
    onAdvance: ((DonationStatus) -> Unit)? = null,
    busy: Boolean = false,
    completionMessage: String? = null,
    completionError: String? = null,
) {
    var advanceOpen by rememberSaveable(review.id) { mutableStateOf(false) }
    val nextStatus = when (review.status) {
        DonationStatus.PICKUP_SCHEDULED -> DonationStatus.PICKED_UP
        DonationStatus.PICKED_UP -> DonationStatus.COMPLETED
        else -> null
    }
    LaunchedEffect(review.status) { advanceOpen = false }
    var date by rememberSaveable(review.id) { mutableLongStateOf(0) }
    var minutes by rememberSaveable(review.id) { mutableIntStateOf(-1) }
    var scheduleOpen by rememberSaveable(review.id) { mutableStateOf(false) }
    var rejectionOpen by rememberSaveable(review.id) { mutableStateOf(false) }
    var scheduleError by rememberSaveable(review.id) { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val pending = review.status == DonationStatus.PENDING_REVIEW
    val pickupAt = if (date > 0 && minutes >= 0) date + minutes * 60_000L else null

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

                LoopyButton("Setujui & jadwalkan", onClick = {
                    scheduleError = when {
                        date == 0L && minutes < 0 -> "Pilih tanggal dan jam penjemputan terlebih dahulu."
                        date == 0L -> "Pilih tanggal penjemputan terlebih dahulu."
                        pickupAt == null -> "Pilih jam penjemputan terlebih dahulu."
                        pickupAt <= System.currentTimeMillis() -> PastPickupMessage
                        else -> null
                    }
                    scheduleOpen = scheduleError == null
                })

                LoopyButton("Tolak pengajuan", onClick = { rejectionOpen = true }, outlined = true)
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

        ClothingPhoto(review.clothing, photo)
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

        if (pending) {
            Text("Jadwal final", style = MaterialTheme.typography.titleLarge)

            LoopyButton(
                label = if (date == 0L) "Pilih tanggal" else formatPickupDate(date),
                onClick = {
                    selectPickupDate(context, date) {
                        date = it
                        scheduleError = null
                    }
                },
                outlined = true,
            )

            LoopyButton(
                label = if (minutes < 0) "Pilih jam (WIB)" else String.format(Indonesian, "%02d.%02d WIB", minutes / 60, minutes % 60),
                onClick = {
                    val initial = if (minutes >= 0) minutes else 9 * 60

                    TimePickerDialog(context, { _, hour, minute ->
                        minutes = hour * 60 + minute
                        scheduleError = null
                    }, initial / 60, initial % 60, true).show()
                },
                outlined = true,
            )

            Text("Jadwal final wajib diisi untuk menyetujui pengajuan.", style = MaterialTheme.typography.bodySmall)
        }

        if (pending && scheduleOpen && pickupAt != null) {
            ScheduleConfirmDialog(
                summary = "#${review.id} · ${review.quantity} pakaian",
                schedule = formatPickupSchedule(pickupAt),
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
private fun ClothingPhoto(clothing: String, photo: ImageBitmap?) {
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

                Text("$clothing · Ilustrasi", color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
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
