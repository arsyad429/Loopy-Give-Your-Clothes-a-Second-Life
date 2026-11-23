package id.ac.loopy.feature.clothingcheck

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.loopy.R
import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ResultCanvas = Color(0xFFFAF8F3)
private val ResultGreen = Color(0xFF246348)
private val ResultInk = Color(0xFF223D31)
private val ResultMuted = Color(0xFF596B60)
private val ResultSage = Color(0xFFE7EFDF)
private val ResultCardShape = RoundedCornerShape(24.dp)
private val ResultFont = FontFamily(
    Font(R.font.plus_jakarta_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold),
)

private fun resultText(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = ResultFont, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight)

/** S06 | F05. Collects the result for the check ID scoped to this navigation entry. */
@Composable
fun CheckResultScreen(
    viewModel: CheckResultViewModel?,
    onBack: () -> Unit,
    onRecheck: () -> Unit,
    onPickup: (String) -> Unit,
    emptyMessage: String = "Layanan pemeriksaan belum terhubung.",
    loadPrivatePhoto: (suspend (String) -> ByteArray)? = null,
) {
    val state = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: CheckResultUiState()
    CheckResultContent(
        state = state,
        onBack = onBack,
        onRecheck = onRecheck,
        onPickup = onPickup,
        onRetry = viewModel?.let { it::retry },
        onConfirmationChange = { viewModel?.setCleanAndSafeConfirmed(it) },
        emptyMessage = emptyMessage,
        loadPrivatePhoto = loadPrivatePhoto,
    )
}

/** Stateless layout for loading, errors, and the three inspection results. */
@Composable
internal fun CheckResultContent(
    state: CheckResultUiState,
    onBack: () -> Unit,
    onRecheck: () -> Unit,
    onPickup: (String) -> Unit,
    onRetry: (() -> Unit)?,
    onConfirmationChange: (Boolean) -> Unit,
    emptyMessage: String = "Lakukan pemeriksaan pakaian untuk melihat hasilnya.",
    loadPrivatePhoto: (suspend (String) -> ByteArray)? = null,
) {
    val check = state.check

    Column(Modifier.fillMaxSize().background(ResultCanvas)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_clothing_back),
                    contentDescription = "Kembali",
                    tint = ResultGreen,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                "Hasil pemeriksaan",
                color = ResultGreen,
                style = resultText(18, 24, FontWeight.Bold),
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when {
                state.isLoading -> ResultMessage("Memuat hasil pemeriksaan…", loading = true)
                state.error != null -> ResultMessage(state.error, error = true)
                check == null -> ResultMessage(emptyMessage)
                else -> {
                    ResultPhoto(check.clothingPhoto, check.checkId, loadPrivatePhoto)
                    ResultDetails(check)
                    Text(
                        "Hasil AI adalah pemeriksaan awal. Penerimaan tetap diverifikasi admin.",
                        color = ResultMuted,
                        style = resultText(12, 18, FontWeight.Medium),
                    )
                    if (state.isEligibleForPickup) {
                        ResultConfirmation(state.cleanAndSafeConfirmed, onConfirmationChange)
                    }
                }
            }
        }

        if (!state.isLoading) {
            when {
                state.error != null && onRetry != null -> ResultButton("Coba lagi", onRetry)
                check == null -> ResultButton("Mulai pemeriksaan", onRecheck)
                state.isEligibleForPickup -> ResultButton(
                    label = "Ajukan penjemputan",
                    enabled = state.canContinueToPickup,
                    onClick = { onPickup(check.checkId) },
                )
                else -> ResultButton("Ambil foto lagi", onRecheck)
            }
        }
    }
}

@Composable
private fun ResultPhoto(
    reference: String,
    checkId: String,
    loadPrivatePhoto: (suspend (String) -> ByteArray)?,
) {
    val context = LocalContext.current
    val isSample = LocalInspectionMode.current
    val canLoadPhoto = !isSample && (loadPrivatePhoto != null || reference.isNotBlank())
    val photo by key(reference, checkId, canLoadPhoto) {
        produceState(PhotoPreview(loading = canLoadPhoto)) {
            if (canLoadPhoto) {
                val bitmap = try {
                    if (loadPrivatePhoto == null) {
                        loadCheckResultPhoto(context, reference)
                    } else {
                        val bytes = loadPrivatePhoto(checkId)
                        withContext(Dispatchers.Default) {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                value = PhotoPreview(bitmap = bitmap)
            }
        }
    }
    val bitmap = photo.bitmap
    Column(
        Modifier.fillMaxWidth().height(180.dp).clip(ResultCardShape).background(ResultSage),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = "Foto pakaian yang diperiksa",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            photo.loading -> {
                CircularProgressIndicator(Modifier.size(28.dp), color = ResultGreen, strokeWidth = 2.dp)
                Text("Memuat foto…", color = ResultMuted, style = resultText(11, 16))
            }
            else -> {
                Icon(
                    painterResource(R.drawable.ic_check_result_shirt),
                    contentDescription = null,
                    tint = ResultGreen,
                    modifier = Modifier.size(90.dp),
                )
                Text(
                    if (isSample) "Contoh pakaian · ilustrasi" else "Foto pakaian tidak tersedia",
                    color = ResultMuted,
                    style = resultText(11, 16, FontWeight.SemiBold),
                )
            }
        }
    }
}

private data class PhotoPreview(val bitmap: ImageBitmap? = null, val loading: Boolean = false)

private data class ResultAppearance(
    val title: String,
    val fallbackDescription: String,
    val background: Color,
    @get:DrawableRes val icon: Int,
    val tint: Color,
)

@Composable
private fun ResultDetails(check: ClothingCheck) {
    val result = check.aiResult
    val eligible = result == AiResult.POTENTIALLY_ELIGIBLE
    val appearance = when (result) {
        AiResult.POTENTIALLY_ELIGIBLE -> ResultAppearance(
            "Berpotensi layak",
            "Kondisi yang terlihat pada foto menunjukkan pakaian berpotensi layak didonasikan.",
            Color(0xFFD5F0DF),
            R.drawable.ic_check_result_verified,
            ResultGreen,
        )
        AiResult.NEEDS_RECHECK -> ResultAppearance(
            "Perlu foto ulang",
            "Ambil ulang foto dengan cahaya cukup dan pastikan seluruh pakaian terlihat.",
            Color(0xFFFFF0CB),
            R.drawable.ic_check_result_alert,
            Color(0xFF76531D),
        )
        AiResult.NOT_RECOMMENDED -> ResultAppearance(
            "Tidak direkomendasikan",
            "Kondisi yang terlihat pada foto membuat pakaian kurang sesuai untuk donasi.",
            Color(0xFFFBE7E3),
            R.drawable.ic_check_result_alert,
            Color(0xFF993F35),
        )
    }
    val recommendation = check.actionRecommendation?.trim()?.takeIf { it.isNotEmpty() }
        ?: if (result == AiResult.NOT_RECOMMENDED) "Pertimbangkan perbaikan atau daur ulang." else null

    Column(
        Modifier.fillMaxWidth().clip(ResultCardShape).background(appearance.background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (eligible) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(appearance.icon),
                    contentDescription = null,
                    tint = appearance.tint,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    appearance.title, color = ResultGreen, style = resultText(18, 24, FontWeight.Bold),
                    modifier = Modifier.semantics { heading() },
                )
            }
        } else {
            Icon(
                painterResource(appearance.icon),
                contentDescription = null,
                tint = appearance.tint,
                modifier = Modifier.size(40.dp),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Keep the long status word intact on narrow displays and at larger text sizes.
                val compact = result == AiResult.NOT_RECOMMENDED &&
                    maxWidth / LocalDensity.current.fontScale < 245.dp
                Text(
                    appearance.title,
                    color = ResultInk,
                    style = if (compact) resultText(19, 26, FontWeight.Bold) else resultText(26, 32, FontWeight.Bold),
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        Text("Jenis pakaian: " + check.clothingType.label, color = ResultMuted, style = resultText(12, 18, FontWeight.Medium))
        Text(check.aiDescription.ifBlank { appearance.fallbackDescription }, color = ResultInk, style = resultText(14, 21))
        if (recommendation != null) {
            Text(recommendation, color = ResultInk, style = resultText(14, 21, FontWeight.Medium))
        }
    }
}

@Composable
private fun ResultConfirmation(confirmed: Boolean, onConfirmationChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(ResultCardShape).background(Color.White)
            .toggleable(value = confirmed, role = Role.Checkbox, onValueChange = onConfirmationChange)
            .padding(20.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .background(if (confirmed) ResultGreen else Color.White)
                .border(2.dp, ResultGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (confirmed) {
                Icon(
                    painterResource(R.drawable.ic_check_result_confirmation),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            "Saya memastikan pakaian bersih dan aman digunakan.",
            color = ResultInk,
            style = resultText(14, 21),
        )
    }
}

@Composable
private fun ResultMessage(message: String, loading: Boolean = false, error: Boolean = false) {
    val background = if (error) Color(0xFFFBE7E3) else ResultSage
    Row(
        Modifier.fillMaxWidth().clip(ResultCardShape).background(background).padding(20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = ResultGreen, strokeWidth = 2.dp)
        }
        Text(message, color = ResultInk, style = resultText(14, 21))
    }
}

@Composable
private fun ResultButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(Modifier.fillMaxWidth().padding(24.dp)) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ResultGreen,
                contentColor = Color.White,
                disabledContainerColor = ResultSage,
                disabledContentColor = ResultMuted,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(label, style = resultText(14, 20, FontWeight.SemiBold))
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, showBackground = true)
@Preview(widthDp = 320, heightDp = 640, fontScale = 1.3f, showBackground = true)
@Composable
private fun CheckResultPreview() {
    val check = ClothingCheck(
        checkId = "preview",
        userId = "preview",
        clothingPhoto = "",
        clothingType = ClothingType.TSHIRT,
        aiResult = AiResult.POTENTIALLY_ELIGIBLE,
        aiDescription = "Tidak tampak sobekan besar pada foto. Periksa kembali kebersihan dan bagian yang tidak terlihat.",
        createdAt = "",
    )
    CheckResultContent(
        state = CheckResultUiState(check = check, cleanAndSafeConfirmed = true),
        onBack = {},
        onRecheck = {},
        onPickup = {},
        onRetry = null,
        onConfirmationChange = {},
    )
}
