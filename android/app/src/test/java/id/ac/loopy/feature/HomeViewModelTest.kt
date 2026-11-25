package id.ac.loopy.feature

import id.ac.loopy.data.repository.HomeRepository
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem
import id.ac.loopy.feature.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun refreshPreservesIndependentDataAndRetriesWithoutDuplicatingRequests() = runTest(dispatcher) {
        val donation = DonationSummaryItem(
            donationId = "sample-1", donorName = "Nara", city = "Malang", quantity = 2,
            status = DonationStatus.PENDING_REVIEW, version = 1, createdAt = "", updatedAt = "",
        )
        var points: Int? = null
        var donationToReturn: DonationSummaryItem? = donation
        var calls = 0
        val repository = object : HomeRepository {
            override suspend fun getPoints(): Int {
                calls++
                return points ?: throw IOException("offline")
            }
            override suspend fun getLatestDonation() = donationToReturn
        }
        val model = HomeViewModel(repository)

        model.refresh()
        model.refresh()
        advanceUntilIdle()
        val partial = model.uiState.value
        assertEquals(1, calls)
        assertNull(partial.points)
        assertEquals(donation, partial.latestDonation)
        assertTrue(partial.pointsUnavailable)
        assertFalse(partial.donationUnavailable)

        points = 0
        donationToReturn = null
        model.refresh()
        advanceUntilIdle()
        val recovered = model.uiState.value
        assertEquals(0, recovered.points)
        assertNull(recovered.latestDonation)
        assertFalse(recovered.pointsUnavailable)
        assertFalse(recovered.isLoading)

        points = null
        model.refresh()
        advanceUntilIdle()
        assertEquals(0, model.uiState.value.points)
        assertTrue(model.uiState.value.pointsUnavailable)
    }
}
