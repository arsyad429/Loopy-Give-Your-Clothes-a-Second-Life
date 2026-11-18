package id.ac.loopy.feature.clothingcheck

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import id.ac.loopy.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

// S05 tokens from the local .fig (2:88). Scoped here until the shared design system is implemented.
private val CanvasColor = Color(0xFFFAF8F3)
private val LeafColor = Color(0xFF246348)
private val InkColor = Color(0xFF223D31)
private val MutedColor = Color(0xFF596B60)
private val SoftColor = Color(0xFFE7EFDF)
private val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold),
)
private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Jakarta, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight,
)

/** Route adapter: native photo picker/camera. AI integration belongs in ViewModel/repository next. */
@Composable
fun ClothingDetectorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var launchError by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val state by produceState(ClothingDetectorUiState(), selectedUri) {
        val uri = selectedUri
        if (uri == null) {
            value = ClothingDetectorUiState()
        } else {
            value = ClothingDetectorUiState(isLoading = true)
            value = try {
                loadClothingPhoto(context, Uri.parse(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ClothingDetectorUiState(error = e.message ?: "Foto tidak dapat dibaca. Silakan pilih ulang.")
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Keep read access when the provider supports persistable grants (e.g. after recreation).
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            launchError = null
            selectedUri = uri.toString()
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val pending = cameraUri
        if (success && pending != null) {
            selectedUri = pending
            launchError = null
        } else if (pending != null) {
            runCatching { context.contentResolver.delete(Uri.parse(pending), null, null) }
        }
        cameraUri = null
    }
    Box(Modifier.fillMaxSize()) {
        ClothingDetectorContent(
            state = state.copy(error = launchError ?: state.error),
            onBack = onBack,
            onTakePhoto = {
                try {
                    val directory = File(context.cacheDir, "clothing_photos").apply { mkdirs() }
                    val file = File.createTempFile("clothing_", ".jpg", directory)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    cameraUri = uri.toString()
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    launchError = "Aplikasi kamera tidak tersedia. Silakan pilih foto dari galeri."
                } catch (_: Exception) {
                    launchError = "Kamera tidak dapat dibuka. Silakan coba lagi atau pilih galeri."
                }
            },
            onPickPhoto = {
                try {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } catch (_: ActivityNotFoundException) {
                    launchError = "Galeri tidak dapat dibuka pada perangkat ini."
                }
            },
            onAnalyze = {
                scope.launch { snackbar.showSnackbar("Analisis AI belum tersedia. Layanan pemeriksaan belum terhubung.") }
            },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(24.dp))
    }
}

/** Stateless S05 layout; platform status/navigation bars are provided by Android, not drawn here. */
@Composable
fun ClothingDetectorContent(
    state: ClothingDetectorUiState,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(CanvasColor)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_clothing_back), "Kembali", tint = LeafColor, modifier = Modifier.size(24.dp))
            }
            Text("Cek pakaian", style = type(18, 24, FontWeight.Bold), color = LeafColor)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Satu foto,\nlangkah pertama.", color = InkColor, style = type(26, 32, FontWeight.Bold))
            Text("Bentangkan satu pakaian dan tampilkan seluruh bagiannya.", color = MutedColor, style = type(14, 21))
            Column(
                Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(24.dp)).background(SoftColor).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                when {
                    state.isLoading -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LeafColor, modifier = Modifier.size(32.dp))
                    }
                    state.photo != null -> Image(
                        bitmap = state.photo, contentDescription = "Pratinjau pakaian yang dipilih",
                        contentScale = ContentScale.Fit, modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    else -> Icon(
                        painterResource(R.drawable.ic_clothing_shirt), "Ilustrasi contoh pakaian",
                        tint = LeafColor, modifier = Modifier.size(106.dp),
                    )
                }
                Text(
                    state.photoName ?: if (state.isLoading) "Memuat foto…" else "kaus-hijau.jpg · contoh foto",
                    color = MutedColor, style = type(12, 18, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoSourceButton("Ambil foto", onTakePhoto, !state.isLoading && !state.isAnalyzing, Modifier.weight(1f))
                PhotoSourceButton("Pilih galeri", onPickPhoto, !state.isLoading && !state.isAnalyzing, Modifier.weight(1f))
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SoftColor).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_clothing_camera), null, tint = LeafColor, modifier = Modifier.size(20.dp))
                    Text("Biar hasilnya lebih jelas", color = InkColor, style = type(14, 20, FontWeight.SemiBold))
                }
                Text("Cahaya cukup · pakaian terlihat utuh · latar sederhana.", color = MutedColor, style = type(12, 18, FontWeight.Medium))
            }
            Text(
                "AI menilai kondisi yang terlihat pada foto. Bau dan kerusakan tersembunyi tetap perlu kamu periksa.",
                color = MutedColor, style = type(12, 18, FontWeight.Medium),
            )
            state.error?.let { Text(it, color = Color(0xFF9B302E), style = type(12, 18, FontWeight.Medium)) }
        }
        Box(Modifier.fillMaxWidth().padding(24.dp)) {
            Button(
                onClick = onAnalyze, enabled = state.canAnalyze,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LeafColor, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isAnalyzing) "Memeriksa pakaian…" else "Periksa pakaian", style = type(14, 20, FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun PhotoSourceButton(label: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SoftColor, contentColor = LeafColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
    ) { Text(label, style = type(14, 20, FontWeight.SemiBold)) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 816)
@Composable
private fun ClothingDetectorPreview() {
    MaterialTheme { ClothingDetectorContent(ClothingDetectorUiState(), {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun ClothingDetectorSmallPreview() {
    MaterialTheme { ClothingDetectorContent(ClothingDetectorUiState(error = "Foto tidak dapat dibaca. Silakan pilih ulang."), {}, {}, {}, {}) }
}
