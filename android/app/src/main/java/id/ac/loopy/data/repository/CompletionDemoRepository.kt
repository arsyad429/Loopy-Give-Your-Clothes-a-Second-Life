package id.ac.loopy.data.repository

import androidx.room.withTransaction
import id.ac.loopy.data.local.CompletionDemoDatabase
import id.ac.loopy.data.local.DemoDonation
import id.ac.loopy.data.local.DemoPointEntry
import id.ac.loopy.data.local.DemoRedemption
import id.ac.loopy.domain.model.DonationStatus

data class LoopyReward(val id: String, val name: String, val description: String, val points: Int, val icon: String)

val DemoRewardCatalog = listOf(
    LoopyReward("eco-supporter-badge", "Eco Supporter Badge", "Badge digital untuk langkah baikmu.", 10, "leaf"),
    LoopyReward("loopy-digital-wallpaper", "Loopy Digital Wallpaper", "Warna baru untuk layar ponselmu.", 20, "image"),
    LoopyReward("voucher-demo", "Voucher Demo", "SIMULASI · tidak berlaku di merchant.", 30, "gift"),
)

/** Local simulation of PRD 6.5–6.6. Production decisions and balances belong to the Go API. */
class CompletionDemoRepository(private val db: CompletionDemoDatabase) {
    private val dao = db.completionDao()
    val donations = dao.observeDonations()
    val balance = dao.observeBalance()
    val redemptions = dao.observeRedemptions()

    suspend fun prepare() = db.withTransaction {
        if (dao.donationCount() == 0) {
            val now = System.currentTimeMillis()
            listOf(
                DemoDonation("LP024", 3, "PENDING_REVIEW", "Menunggu konfirmasi admin", updatedAt = now),
                DemoDonation("LP021", 2, "PICKUP_SCHEDULED", "23 Nov · 10.00 WIB", updatedAt = now - 1),
                DemoDonation("LP018", 4, "COMPLETED", "20 Nov · 09.00 WIB", updatedAt = now - 86_400_000),
                DemoDonation("LP017", 1, "COMPLETED", "19 Nov · 09.00 WIB", updatedAt = now - 172_800_000),
            ).forEach { dao.insertDonation(it) }
            dao.insertPoints(DemoPointEntry("earn:LP018", 10))
            dao.insertPoints(DemoPointEntry("earn:LP017", 10))
        }
    }

    suspend fun advance(id: String, target: DonationStatus) = db.withTransaction {
        val item = requireNotNull(dao.donation(id)) { "Pengajuan tidak ditemukan." }
        val current = DonationStatus.valueOf(item.status)
        require(target in listOf(DonationStatus.PICKED_UP, DonationStatus.COMPLETED) && current.canTransitionTo(target)) {
            "Status sudah berubah. Ikuti urutan Dijadwalkan → Dijemput → Selesai."
        }
        check(dao.advance(id, item.status, target.name, System.currentTimeMillis()) == 1) { "Status sudah berubah." }
        if (target == DonationStatus.COMPLETED) dao.insertPoints(DemoPointEntry("earn:$id", 10))
    }

    suspend fun createPendingDonation(quantity: Int, schedule: String, address: String): DemoDonation = db.withTransaction {
        val now = System.currentTimeMillis()
        val id = "LP" + ((now % 900) + 100).toString().padStart(3, '0')
        val uniqueId = if (dao.donation(id) == null) id else "LP${now % 100000}"
        val donation = DemoDonation(
            id = uniqueId,
            quantity = quantity,
            status = DonationStatus.PENDING_REVIEW.name,
            schedule = schedule,
            address = address,
            updatedAt = now,
        )
        dao.insertDonation(donation)
        donation
    }

    suspend fun redeem(rewardId: String, requestKey: String) = db.withTransaction {
        val previous = dao.redemption(requestKey)
        if (previous != null) {
            require(previous.rewardId == rewardId) { "Permintaan sudah digunakan untuk reward lain." }
        } else {
            val reward = requireNotNull(DemoRewardCatalog.find { it.id == rewardId }) { "Reward tidak tersedia." }
            require(dao.balance() >= reward.points) { "Poin belum cukup. Saldo terbaru sudah diperiksa kembali." }
            dao.insertRedemption(DemoRedemption(requestKey, reward.id, reward.name, reward.description, reward.icon, reward.points, System.currentTimeMillis()))
            dao.insertPoints(DemoPointEntry("redeem:$requestKey", -reward.points))
        }
    }
}
