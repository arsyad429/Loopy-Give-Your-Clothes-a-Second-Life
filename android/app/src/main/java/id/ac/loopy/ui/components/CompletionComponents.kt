package id.ac.loopy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.loopy.R
import id.ac.loopy.data.local.DemoDonation
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem
import id.ac.loopy.feature.rewards.CompletionUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun LoopyCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    padding: Int = 20,
    spacing: Int = 12,
    radius: Int = 24,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().background(color, RoundedCornerShape(radius.dp)).padding(padding.dp), verticalArrangement = Arrangement.spacedBy(spacing.dp), content = content)
}

@Composable
internal fun LoopyAsset(name: String, size: Int = 24, tint: Color = MaterialTheme.colorScheme.primary) {
    val resource = when (name) {
        "leaf" -> R.drawable.loopy_leaf
        "image" -> R.drawable.loopy_image
        "gift" -> R.drawable.loopy_gift
        "house" -> R.drawable.loopy_house
        "package" -> R.drawable.loopy_package
        "check" -> R.drawable.loopy_check
        "calendar" -> R.drawable.loopy_calendar
        "pin" -> R.drawable.loopy_pin
        "shirt_hero" -> R.drawable.loopy_shirt_hero
        else -> R.drawable.loopy_shirt
    }
    Image(painterResource(resource), contentDescription = null, modifier = Modifier.size(size.dp), colorFilter = ColorFilter.tint(tint))
}

@Composable
internal fun pointsCardColor(): Color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFEEE7FA) else Color(0xFF383044)

@Composable
internal fun PointsCard(balance: Int, onRewards: (() -> Unit)? = null) {
    LoopyCard(color = pointsCardColor(), padding = if (onRewards == null) 20 else 16, spacing = if (onRewards == null) 12 else 8) {
        Text("LOOPY POINTS", style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 16.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        val balanceType = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)
        if (onRewards == null) Text("$balance poin", style = balanceType)
        else Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$balance", style = balanceType)
            Text("poin terkumpul", style = MaterialTheme.typography.bodySmall)
        }
        if (onRewards == null) Text("Dari donasi yang sudah selesai.", style = MaterialTheme.typography.bodySmall)
        else Text("Lihat rewards →", modifier = Modifier.clickable(role = Role.Button, onClick = onRewards).padding(vertical = 4.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun UserBottomBar(selected: String, onNavigate: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("s04", "Beranda", "house"), Triple("s08", "Donasiku", "package"), Triple("s10", "Rewards", "gift")).forEach { (route, label, icon) ->
            Column(
                Modifier.weight(1f).background(if (selected == route) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(16.dp))
                    .selectable(selected = selected == route, role = Role.Tab, onClick = { onNavigate(route) }).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LoopyAsset(icon, 22)
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 16.sp))
            }
        }
    }
}

@Composable
internal fun DonationCard(item: DemoDonation, onOpen: () -> Unit) {
    DonationCard(
        title = "${item.quantity} pakaian · #${item.id}",
        subtitle = if (item.status == "COMPLETED") "Selesai · +10 poin" else item.schedule,
        status = DonationStatus.valueOf(item.status),
        onOpen = onOpen,
    )
}

@Composable
internal fun DonationCard(item: DonationSummaryItem, onOpen: () -> Unit) {
    val shortId = item.donationId.substringBefore('-').uppercase(Locale.ROOT)
    DonationCard("${item.quantity} pakaian · #$shortId", item.city, item.status, onOpen)
}

@Composable
private fun DonationCard(title: String, subtitle: String, status: DonationStatus, onOpen: () -> Unit) {
    LoopyCard(modifier = Modifier.clickable(role = Role.Button, onClick = onOpen), padding = 16, radius = 20) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).padding(12.dp)) { LoopyAsset("shirt", 28) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                StatusBadge(status)
            }
        }
    }
}

@Composable
internal fun CompletionFeedback(state: CompletionUiState, onRetry: () -> Unit) {
    if (state.loading || state.busy) Text(if (state.loading) "Memuat data lokal…" else "Menyimpan…", style = MaterialTheme.typography.bodySmall)
    state.error?.let {
        LoopyInfo("Belum berhasil", it, isError = true)
        LoopyButton("Muat ulang", onRetry, enabled = !state.busy)
    }
    state.message?.let { LoopyInfo("Berhasil", it) }
}

internal fun rewardDate(at: Long): String = SimpleDateFormat("d MMM yyyy · HH.mm 'WIB'", Locale.forLanguageTag("id-ID")).apply {
    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
}.format(Date(at))
