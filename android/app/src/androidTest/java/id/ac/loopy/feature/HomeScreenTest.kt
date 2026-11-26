package id.ac.loopy.feature

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem
import id.ac.loopy.feature.home.HomeContent
import id.ac.loopy.feature.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signedInDashboardShowsAccountDataAndRoutesActions() {
        val item = DonationSummaryItem(
            donationId = "sample-1", donorName = "Nara", city = "Malang", quantity = 2,
            status = DonationStatus.PENDING_REVIEW, version = 1, createdAt = "", updatedAt = "",
        )
        val actions = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                HomeContent(
                    state = HomeUiState(points = 10, latestDonation = item, isLoading = false),
                    name = "Nara",
                    onCheckClothing = { actions.add("check") },
                    onMyDonations = { actions.add("donations") },
                    onRewards = { actions.add("rewards") },
                    onDonation = { actions.add(it) },
                    onRefresh = {},
                    onProfile = { actions.add("profile") },
                )
            }
        }
        compose.onNodeWithText("Hai, Nara!").assertExists()
        compose.onNodeWithText("10").assertExists()
        compose.onNodeWithText("Cek pakaian").performScrollTo().performClick()
        compose.onNodeWithText("2 pakaian · #SAMPLE").performScrollTo().performClick()
        compose.onNodeWithText("Donasiku").performClick()
        compose.onNodeWithText("Rewards").performClick()
        compose.onNodeWithContentDescription("Akun").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("check", "sample-1", "donations", "rewards", "profile"), actions)
        }
    }

    @Test fun unavailableDataDoesNotLookLikeAnEmptyAccount() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                HomeContent(
                    state = HomeUiState(isLoading = false, pointsUnavailable = true, donationUnavailable = true),
                    name = "Nara",
                    onCheckClothing = {}, onMyDonations = {}, onRewards = {}, onDonation = {},
                    onRefresh = { retries++ }, onProfile = {},
                )
            }
        }
        compose.onNodeWithText("Saldo gagal dimuat.").assertExists()
        compose.onNodeWithText("Donasi gagal dimuat").assertExists()
        compose.onAllNodesWithText("Belum ada donasi").assertCountEquals(0)
        compose.onNodeWithText("Coba lagi").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
