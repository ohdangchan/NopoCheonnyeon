package com.nopo.cheonnyeon.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.nopo.cheonnyeon.core.common.AppError
import com.nopo.cheonnyeon.core.common.AppResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Firestore 호출 결과가 [AppError]로 바뀌는지 (S-801, 가짜 Firestore 사용) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ReviewRepositoryTest {
    private val db = mockk<FirebaseFirestore>()
    private val reports = mockk<CollectionReference>()
    private val reviews = mockk<CollectionReference>()

    private fun repo(uid: String? = "me") = ReviewRepository({ db }, { uid })

    private fun <T> fail(code: Code) = Tasks.forException<T>(FirebaseFirestoreException("x", code))

    @Test fun `로그인하지 않으면 내 리뷰 조회는 인증 필요`() = runBlocking {
        assertEquals(AppError.E_AUTH_REQUIRED, repo(uid = null).mine("S1").errorOrNull)
    }

    @Test fun `내 리뷰가 없으면 null 성공`() = runBlocking {
        val doc = mockk<DocumentReference>()
        val snap = mockk<DocumentSnapshot>()
        every { db.collection("reviews") } returns reviews
        every { reviews.document("S1_me") } returns doc
        every { doc.get() } returns Tasks.forResult(snap)
        every { snap.exists() } returns false
        val r = repo().mine("S1")
        assertTrue(r is AppResult.Success)
        assertNull(r.valueOrNull)
    }

    @Test fun `서버에 닿지 못하면 오프라인 오류`() = runBlocking {
        val doc = mockk<DocumentReference>()
        every { db.collection("reviews") } returns reviews
        every { reviews.document("S1_me") } returns doc
        every { doc.get() } returns fail(Code.UNAVAILABLE)
        assertEquals(AppError.E_REVIEW_OFFLINE, repo().mine("S1").errorOrNull)
    }

    @Test fun `규칙이 거부하면 확인 중 오류`() = runBlocking {
        val doc = mockk<DocumentReference>()
        every { db.collection("reviews") } returns reviews
        every { reviews.document("S1_me") } returns doc
        every { doc.get() } returns fail(Code.PERMISSION_DENIED)
        assertEquals(AppError.E_VISIT_SYNCING, repo().mine("S1").errorOrNull)
    }

    @Test fun `그 밖의 서버 오류는 네트워크 오류`() = runBlocking {
        val doc = mockk<DocumentReference>()
        every { db.collection("reviews") } returns reviews
        every { reviews.document("S1_me") } returns doc
        every { doc.get() } returns fail(Code.INTERNAL)
        assertEquals(AppError.E_NETWORK, repo().mine("S1").errorOrNull)
    }

    @Test fun `신고는 reports에 작성자와 사유를 쓴다`() = runBlocking {
        val ref = mockk<DocumentReference>()
        val slot = slot<Map<String, Any>>()
        every { db.collection("reports") } returns reports
        every { reports.add(capture(slot)) } returns Tasks.forResult(ref)
        assertTrue(repo().report("R1", "욕설") is AppResult.Success)
        assertEquals("R1", slot.captured["reviewId"])
        assertEquals("욕설", slot.captured["reason"])
        assertEquals("me", slot.captured["reporterUid"])
    }

    @Test fun `로그인하지 않으면 신고할 수 없다`() = runBlocking {
        assertEquals(AppError.E_AUTH_REQUIRED, repo(uid = null).report("R1", "욕설").errorOrNull)
    }

    @Test fun `가게 정보 제보는 종류와 가게 ID를 함께 쓴다`() = runBlocking {
        val ref = mockk<DocumentReference>()
        val slot = slot<Map<String, Any>>()
        every { db.collection("reports") } returns reports
        every { reports.add(capture(slot)) } returns Tasks.forResult(ref)
        assertTrue(repo().reportStoreInfo("S9", "폐업").valueOrNull != null)
        assertEquals("STORE_INFO", slot.captured["kind"])
        assertEquals("S9", slot.captured["storeId"])
    }

    @Test fun `별점이 범위 밖이면 서버를 부르지 않는다`() = runBlocking {
        val r = repo().save("S1", "가게", rating = 0, text = "맛있어요", visitCount = 1, titleId = "t", existing = false)
        assertTrue(r is AppResult.Failure)
        verify(exactly = 0) { db.collection(any()) }
    }
}
