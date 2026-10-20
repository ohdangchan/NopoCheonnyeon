package com.nopo.cheonnyeon.feature.store

import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.data.local.AppDatabase
import com.nopo.cheonnyeon.data.local.MyVisitDao
import com.nopo.cheonnyeon.data.local.MyVisitEntity
import com.nopo.cheonnyeon.data.remote.AuthRepository
import com.nopo.cheonnyeon.data.remote.ReviewItem
import com.nopo.cheonnyeon.data.remote.ReviewPage
import com.nopo.cheonnyeon.data.remote.ReviewRepository
import com.nopo.cheonnyeon.data.remote.ReviewSummary
import com.nopo.cheonnyeon.data.remote.StoreStats
import com.nopo.cheonnyeon.data.remote.StoreStatsRepository
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** S-506·FR-STORE-03·04: 평점 자격(방문 인증 + 서버 동기화)과 평점·재방문 통계 불러오기 */
class ReviewsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val store = Store(id = "BN-1", name = "백년식당")
    private val reviews = mockk<ReviewRepository>()
    private val storeStats = mockk<StoreStatsRepository>()
    private val dao = mockk<MyVisitDao>()

    private fun myReview() = ReviewItem("BN-1_me", "BN-1", "백년식당", "me", "나", "SPROUT", 5, "", 1, 0L)

    private fun demoItem(rating: Int) = ReviewItem("demo_$rating", "BN-1", "", "demo", "예시", "SPROUT", rating, "맛있어요", 1, 0L, demo = true)

    private fun newVm(
        visit: MyVisitEntity?,
        summary: AppResult<ReviewSummary> = AppResult.Success(ReviewSummary(0, null)),
        demo: List<ReviewItem>? = null,
    ): ReviewsViewModel {
        coEvery { dao.byStore("BN-1") } returns visit
        coEvery { dao.demoVisitCount() } returns 0
        coEvery { reviews.summary("BN-1") } returns summary
        coEvery { reviews.page("BN-1", any()) } returns AppResult.Success(ReviewPage(emptyList(), null, false))
        coEvery { reviews.mine("BN-1") } returns AppResult.Success(myReview())
        coEvery { storeStats.get("BN-1") } returns AppResult.Success(StoreStats(visitors = 8, revisitors = 3))
        val db = mockk<AppDatabase>()
        every { db.myVisitDao() } returns dao
        val auth = mockk<AuthRepository>()
        every { auth.currentUid } returns "me"
        val container = mockk<AppContainer>()
        every { container.database } returns db
        every { container.reviews } returns reviews
        every { container.storeStats } returns storeStats
        every { container.auth } returns auth
        val settings = mockk<com.nopo.cheonnyeon.data.local.SettingsStore>()
        every { settings.demoData } returns kotlinx.coroutines.flow.flowOf(demo != null)
        every { container.settings } returns settings
        val demoRepo = mockk<com.nopo.cheonnyeon.data.remote.DemoReviews>()
        coEvery { demoRepo.forStore("BN-1") } returns demo.orEmpty()
        every { container.demoReviews } returns demoRepo
        return ReviewsViewModel(container, store)
    }

    @Test fun `인증하지 않은 가게는 평점을 쓸 수 없고 내 리뷰를 읽지도 않는다`() {
        val vm = newVm(visit = null)
        assertEquals(ReviewEligibility.NOT_VISITED, vm.state.value.eligibility)
        assertNull(vm.state.value.mine)
        coVerify(exactly = 0) { reviews.mine(any()) }
    }

    @Test fun `인증했지만 서버에 아직 안 올라갔으면 올리는 중`() {
        val vm = newVm(visit = MyVisitEntity("BN-1", 76, "RECEIPT", 1, 1, synced = false))
        assertEquals(ReviewEligibility.SYNCING, vm.state.value.eligibility)
        coVerify(exactly = 0) { reviews.mine(any()) }
    }

    @Test fun `서버에 올라간 방문이면 평점을 쓸 수 있고 내 리뷰를 불러온다`() {
        val vm = newVm(visit = MyVisitEntity("BN-1", 76, "RECEIPT", 1, 1, synced = true))
        assertEquals(ReviewEligibility.OK, vm.state.value.eligibility)
        assertEquals(5, vm.state.value.mine?.rating)
    }

    @Test fun `F-01 재방문 통계를 상태에 담는다`() {
        val vm = newVm(visit = null)
        assertEquals(StoreStats(8, 3), vm.state.value.stats)
    }

    @Test fun `평균과 개수를 상태에 담는다`() {
        val vm = newVm(visit = null, summary = AppResult.Success(ReviewSummary(4, 4.5)))
        assertEquals(4, vm.state.value.summary?.count)
        assertEquals(4.5, vm.state.value.summary?.average ?: 0.0, 1e-9)
    }

    @Test fun `서버 오류는 상태의 error로 올라온다`() {
        val vm = newVm(visit = null, summary = AppResult.Failure(AppError.E_REVIEW_OFFLINE))
        assertEquals(AppError.E_REVIEW_OFFLINE, vm.state.value.error)
        assertEquals(false, vm.state.value.loading)
    }

    @Test fun `시연용 예시 리뷰는 평균과 개수에 합쳐지고 목록에는 따로 담긴다`() {
        val vm = newVm(visit = null, summary = AppResult.Success(ReviewSummary(1, 5.0)), demo = listOf(demoItem(3), demoItem(4)))
        assertEquals(3, vm.state.value.summary?.count)
        assertEquals(4.0, vm.state.value.summary!!.average!!, 0.001)
        assertEquals(2, vm.state.value.demoItems.size)
        assertEquals(0, vm.state.value.items.size)
    }

    @Test fun `시연용 예시를 끄면 서버 값만 보인다`() {
        val vm = newVm(visit = null, summary = AppResult.Success(ReviewSummary(1, 5.0)), demo = null)
        assertEquals(1, vm.state.value.summary?.count)
        assertEquals(0, vm.state.value.demoItems.size)
    }
}
