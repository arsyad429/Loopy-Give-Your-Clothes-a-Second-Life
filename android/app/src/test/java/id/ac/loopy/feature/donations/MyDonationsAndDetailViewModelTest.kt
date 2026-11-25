package id.ac.loopy.feature.donations

import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationCheckReview
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem
import id.ac.loopy.domain.repository.DonationRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MyDonationsAndDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun myDonationsSortsFiltersSearchesAndRecoversFromErrors() = runTest(dispatcher) {
        var shouldFail = true
        val items = listOf(
            summary("don-old", "Malang", DonationStatus.COMPLETED, "2026-11-20T10:00:00Z"),
            summary("don-new", "Surabaya", DonationStatus.PENDING_REVIEW, "2026-11-25T10:00:00Z"),
            summary("don-mid", "Malang", DonationStatus.REJECTED, "2026-11-23T10:00:00Z"),
            summary("don-sched", "Batu", DonationStatus.PICKUP_SCHEDULED, "2026-11-24T10:00:00Z"),
        )
        val repo = object : DonationRepository {
            override suspend fun createDonation(submission: CreateDonationSubmission) = error("unused")
            override suspend fun listMyDonations(status: DonationStatus?, query: String?): List<DonationSummaryItem> {
                if (shouldFail) throw IOException("offline")
                return items
            }
            override suspend fun getDonationDetail(donationId: String) = sampleDetail(donationId)
        }

        val vm = MyDonationsViewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.error)

        shouldFail = false
        vm.refresh()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(
            listOf("don-new", "don-sched", "don-mid", "don-old"),
            vm.uiState.value.filteredItems.map { it.donationId },
        )

        vm.onFilterChange(DonationListFilter.ACTIVE)
        assertEquals(listOf("don-new", "don-sched"), vm.uiState.value.filteredItems.map { it.donationId })

        vm.onFilterChange(DonationListFilter.COMPLETED)
        assertEquals(listOf("don-old"), vm.uiState.value.filteredItems.map { it.donationId })

        vm.onFilterChange(DonationListFilter.REJECTED)
        assertEquals(listOf("don-mid"), vm.uiState.value.filteredItems.map { it.donationId })

        vm.onFilterChange(DonationListFilter.ALL)
        vm.onSearchQueryChange(" malang ")
        assertEquals(listOf("don-mid", "don-old"), vm.uiState.value.filteredItems.map { it.donationId })

        vm.onSearchQueryChange("DON-SCHED")
        assertEquals(listOf("don-sched"), vm.uiState.value.filteredItems.map { it.donationId })
    }

    @Test
    fun donationDetailRejectsBlankIdAndRecoversOnRetry() = runTest(dispatcher) {
        var calls = 0
        val repo = object : DonationRepository {
            override suspend fun createDonation(submission: CreateDonationSubmission) = error("unused")
            override suspend fun listMyDonations(status: DonationStatus?, query: String?) = emptyList<DonationSummaryItem>()
            override suspend fun getDonationDetail(donationId: String): DonationRequest {
                calls++
                if (calls == 1) throw IOException("timeout")
                return sampleDetail(donationId)
            }
        }

        val blankVm = DonationDetailViewModel("", repo)
        advanceUntilIdle()
        assertEquals(0, calls)
        assertNotNull(blankVm.uiState.value.error)

        val vm = DonationDetailViewModel("don-99", repo)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.donation)

        vm.retry()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals("don-99", vm.uiState.value.donation?.donationId)
    }

    private fun summary(id: String, city: String, status: DonationStatus, createdAt: String) = DonationSummaryItem(
        donationId = id,
        donorName = "Neva",
        city = city,
        quantity = 2,
        status = status,
        version = 1,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun sampleDetail(id: String) = DonationRequest(
        donationId = id,
        donorName = "Neva",
        phoneNumber = "081234567890",
        pickupAddress = "Jl. Veteran 12",
        city = "Malang",
        quantity = 2,
        preferredDate = "2026-11-26",
        preferredTime = "Pagi (08.00–11.00)",
        notes = "",
        status = DonationStatus.PENDING_REVIEW,
        version = 1,
        clothingCheck = DonationCheckReview("chk-1", "/p", ClothingType.TSHIRT, AiResult.POTENTIALLY_ELIGIBLE, "Layak"),
        createdAt = "2026-11-25T00:00:00Z",
        updatedAt = "2026-11-25T00:00:00Z",
    )
}
