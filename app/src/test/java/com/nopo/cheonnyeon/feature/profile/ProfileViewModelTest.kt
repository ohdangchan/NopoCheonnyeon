package com.nopo.cheonnyeon.feature.profile

import com.nopo.cheonnyeon.app.AppContainer
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import com.nopo.cheonnyeon.data.remote.AuthRepository
import com.nopo.cheonnyeon.data.remote.ProfileRepository
import com.nopo.cheonnyeon.data.remote.UserProfile
import com.nopo.cheonnyeon.data.remote.VisitedStore
import com.nopo.cheonnyeon.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** S-804·FR-PRF-07: 프로필 불러오기와 방문 기록 공개 범위 */
class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val profiles = mockk<ProfileRepository>()
    private val visits = listOf(VisitedStore("BN-1", "백년식당", "종로구", 76, 2, 1_000L))

    private fun profile(uid: String, visibility: String) = UserProfile(uid, "닉네임", "SPROUT", 76, 2, 0, createdAtMillis = 5L, visibility = visibility)

    private fun newVm(target: String, me: String?, visibility: String): ProfileViewModel {
        coEvery { profiles.profile(target) } returns AppResult.Success(profile(target, visibility))
        coEvery { profiles.visits(target, any()) } returns AppResult.Success(visits)
        coEvery { profiles.reviewsOf(target) } returns AppResult.Success(emptyList())
        val auth = mockk<AuthRepository>()
        every { auth.currentUid } returns me
        val container = mockk<AppContainer>()
        every { container.profiles } returns profiles
        every { container.auth } returns auth
        return ProfileViewModel(container, target)
    }

    @Test fun `전체 공개 프로필은 다녀온 노포를 보여 준다`() {
        val state = newVm("other", me = "me", visibility = "ALL").state.value
        assertEquals(visits, state.visits)
        assertFalse(state.visitsHidden)
        assertEquals(5L, state.profile?.createdAtMillis)
    }

    @Test fun `FR-PRF-07 횟수만 공개한 사용자의 방문 목록은 요청하지 않는다`() {
        val state = newVm("other", me = "me", visibility = "COUNT").state.value
        assertTrue(state.visitsHidden)
        assertTrue(state.visits.isEmpty())
        coVerify(exactly = 0) { profiles.visits(any(), any()) }
    }

    @Test fun `나만 보기 사용자의 목록도 숨긴다`() {
        assertTrue(newVm("other", me = "me", visibility = "PRIVATE").state.value.visitsHidden)
    }

    @Test fun `내 프로필은 공개 범위와 상관없이 목록이 보인다`() {
        val state = newVm("me", me = "me", visibility = "PRIVATE").state.value
        assertFalse(state.visitsHidden)
        assertEquals(visits, state.visits)
    }

    @Test fun `없는 사용자는 프로필이 null이다`() {
        coEvery { profiles.profile("ghost") } returns AppResult.Success(null)
        coEvery { profiles.visits("ghost", any()) } returns AppResult.Success(emptyList())
        coEvery { profiles.reviewsOf("ghost") } returns AppResult.Success(emptyList())
        val auth = mockk<AuthRepository>()
        every { auth.currentUid } returns "me"
        val container = mockk<AppContainer>()
        every { container.profiles } returns profiles
        every { container.auth } returns auth
        val state = ProfileViewModel(container, "ghost").state.value
        assertNull(state.profile)
        assertNull(state.error)
        assertFalse(state.loading)
    }

    @Test fun `서버 오류는 error로 올라온다`() {
        coEvery { profiles.profile("x") } returns AppResult.Failure(AppError.E_NETWORK)
        coEvery { profiles.visits("x", any()) } returns AppResult.Success(emptyList())
        coEvery { profiles.reviewsOf("x") } returns AppResult.Success(emptyList())
        val auth = mockk<AuthRepository>()
        every { auth.currentUid } returns "me"
        val container = mockk<AppContainer>()
        every { container.profiles } returns profiles
        every { container.auth } returns auth
        assertEquals(AppError.E_NETWORK, ProfileViewModel(container, "x").state.value.error)
    }
}
