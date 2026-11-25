package id.ac.loopy.feature.clothingcheck

import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType
import id.ac.loopy.domain.repository.ClothingCheckRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CheckResultAndDetectorViewModelTest {
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
    fun clothingDetectorPreventsBlankAndDuplicateAnalyzeCalls() = runTest(dispatcher) {
        val gate = CompletableDeferred<ClothingCheck>()
        var analyzeCalls = 0
        val repo = object : ClothingCheckRepository {
            override suspend fun analyzeClothing(photoUri: String, clothingType: ClothingType): ClothingCheck {
                analyzeCalls++
                return gate.await()
            }
            override suspend fun getCheckById(checkId: String) = error("unused")
            override fun observeCheck(checkId: String) = flowOf(null)
        }
        val vm = ClothingDetectorViewModel(repo)

        vm.analyze("   ", ClothingType.TSHIRT)
        assertEquals(0, analyzeCalls)

        vm.analyze("content://photo/1", ClothingType.TSHIRT)
        runCurrent()
        assertTrue(vm.analysisState.value.isAnalyzing)

        vm.analyze("content://photo/2", ClothingType.SHIRT)
        assertEquals(1, analyzeCalls)

        gate.complete(sampleCheck("chk-10", AiResult.POTENTIALLY_ELIGIBLE))
        advanceUntilIdle()
        assertFalse(vm.analysisState.value.isAnalyzing)
        assertEquals("chk-10", vm.analysisState.value.checkId)

        vm.onResultHandled()
        assertNull(vm.analysisState.value.checkId)
    }

    @Test
    fun checkResultRequiresConfirmationAndEligibilityBeforePickup() = runTest(dispatcher) {
        var failOnce = true
        var resultType = AiResult.NEEDS_RECHECK
        val repo = object : ClothingCheckRepository {
            override suspend fun analyzeClothing(photoUri: String, clothingType: ClothingType) = error("unused")
            override suspend fun getCheckById(checkId: String): ClothingCheck {
                if (failOnce) {
                    failOnce = false
                    throw IOException("network error")
                }
                return sampleCheck(checkId, resultType)
            }
            override fun observeCheck(checkId: String) = flowOf(null)
        }

        val blankVm = CheckResultViewModel(" ", repo)
        advanceUntilIdle()
        assertNotNull(blankVm.uiState.value.error)
        assertFalse(blankVm.uiState.value.canContinueToPickup)

        val recheckVm = CheckResultViewModel("chk-1", repo)
        advanceUntilIdle()
        assertNotNull(recheckVm.uiState.value.error)

        recheckVm.retry()
        advanceUntilIdle()
        assertFalse(recheckVm.uiState.value.isEligibleForPickup)
        recheckVm.setCleanAndSafeConfirmed(true)
        assertFalse(recheckVm.uiState.value.canContinueToPickup)

        resultType = AiResult.POTENTIALLY_ELIGIBLE
        val eligibleVm = CheckResultViewModel("chk-2", repo)
        advanceUntilIdle()
        assertTrue(eligibleVm.uiState.value.isEligibleForPickup)
        assertFalse(eligibleVm.uiState.value.canContinueToPickup)

        eligibleVm.setCleanAndSafeConfirmed(true)
        assertTrue(eligibleVm.uiState.value.canContinueToPickup)
    }

    private fun sampleCheck(id: String, result: AiResult) = ClothingCheck(
        checkId = id,
        userId = "user-1",
        clothingPhoto = "",
        clothingType = ClothingType.TSHIRT,
        aiResult = result,
        aiDescription = "Layak",
        createdAt = "2026-11-25T00:00:00Z",
    )
}
