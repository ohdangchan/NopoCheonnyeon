package com.nopo.cheonnyeon.data.visit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.nopo.cheonnyeon.data.local.AppDatabase
import com.nopo.cheonnyeon.domain.model.Store
import com.nopo.cheonnyeon.domain.model.VerifyMethod
import com.nopo.cheonnyeon.testutil.FixedClock
import com.nopo.cheonnyeon.testutil.UnconfinedDispatchers
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S-307: 대기열을 Firestore로 올리는 `sync()`/`commit()` 흐름 (가짜 Firestore 사용) */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class VisitSyncTest {
    private lateinit var room: AppDatabase
    private val fs = mockk<FirebaseFirestore>(relaxed = true)
    private val batch = mockk<WriteBatch>(relaxed = true)
    private lateinit var repo: VisitRepository

    private val store = Store(id = "BN-1", name = "가게BN-1", sigungu = "종로구", foundedYear = 1990)

    @Before fun setUp() {
        room = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        repo = VisitRepository(room.myVisitDao(), room.pendingOpDao(), { fs }, { "me" }, FixedClock(), UnconfinedDispatchers)
        every { fs.batch() } returns batch
        every { fs.collection("storeStats").document(any()).set(any<Map<String, Any>>(), any()) } returns Tasks.forResult(null)
    }

    @After fun tearDown() = room.close()

    private fun serverVisitDoc(exists: Boolean, count: Long = 1L) {
        val snap = mockk<DocumentSnapshot>()
        every { snap.exists() } returns exists
        every { snap.getLong("visitCount") } returns count
        every { fs.collection("users").document("me").collection("visits").document("BN-1").get(Source.SERVER) } returns Tasks.forResult(snap)
    }

    private fun denied() = Tasks.forException<Void>(FirebaseFirestoreException("x", Code.PERMISSION_DENIED))

    @Test fun `전송에 성공하면 대기열이 비고 도장이 확인됨으로 바뀐다`() = runBlocking {
        every { batch.commit() } returns Tasks.forResult(null)
        repo.record(store, VerifyMethod.RECEIPT, true, "h1")
        assertEquals(0, repo.sync())
        assertEquals(true, room.myVisitDao().byStore("BN-1")!!.synced)
        verify { batch.commit() }
    }

    @Test fun `서버에 닿지 못하면 대기열에 남고 시도 횟수만 늘어난다`() = runBlocking {
        every { batch.commit() } returns Tasks.forException(FirebaseFirestoreException("x", Code.UNAVAILABLE))
        repo.record(store, VerifyMethod.RECEIPT, true, "h1")
        assertEquals(1, repo.sync())
        assertEquals(1, room.pendingOpDao().all().single().attempts)
        assertEquals(false, room.myVisitDao().byStore("BN-1")!!.synced)
    }

    @Test fun `거부됐지만 서버에 이미 첫 방문이 있으면 성공으로 본다`() = runBlocking {
        every { batch.commit() } returns denied()
        serverVisitDoc(exists = true, count = 1)
        repo.record(store, VerifyMethod.RECEIPT, true, "h1")
        assertEquals(0, repo.sync())
        assertEquals(true, room.myVisitDao().byStore("BN-1")!!.synced)
        assertTrue(repo.duplicateNotices.value.isEmpty())
    }

    @Test fun `서버에 방문이 없는데 거부되면 영수증 중복이라 기록을 되돌리고 알린다`() = runBlocking {
        every { batch.commit() } returns denied()
        serverVisitDoc(exists = false)
        repo.record(store, VerifyMethod.RECEIPT, true, "h1")
        assertEquals(0, repo.sync())
        assertNull(room.myVisitDao().byStore("BN-1"))
        assertEquals(listOf("가게BN-1"), repo.duplicateNotices.value)
    }

    @Test fun `거부됐는데 서버 확인도 못 하면 나중에 다시 시도한다`() = runBlocking {
        every { batch.commit() } returns denied()
        every { fs.collection("users").document("me").collection("visits").document("BN-1").get(Source.SERVER) } returns
            Tasks.forException(FirebaseFirestoreException("x", Code.UNAVAILABLE))
        repo.record(store, VerifyMethod.RECEIPT, true, "h1")
        assertEquals(1, repo.sync())
        assertNotNull(room.myVisitDao().byStore("BN-1"))
        assertEquals(1, room.pendingOpDao().all().single().attempts)
    }

    @Test fun `영수증 없는 기록이 거부되면 건너뛰고 시도 횟수를 늘린다`() = runBlocking {
        every { batch.commit() } returns denied()
        repo.record(store, VerifyMethod.PHOTO, true, null)
        assertEquals(1, repo.sync())
        assertEquals(1, room.pendingOpDao().all().single().attempts)
    }

    @Test fun `집계 전송이 실패하면 집계 작업이 대기열에 남는다`() = runBlocking {
        every { batch.commit() } returns Tasks.forResult(null)
        every { fs.collection("storeStats").document(any()).set(any<Map<String, Any>>(), any()) } returns
            Tasks.forException(FirebaseFirestoreException("x", Code.UNAVAILABLE))
        repo.record(store, VerifyMethod.RECEIPT, true, "h1")
        assertEquals(1, repo.sync())
        assertEquals(true, room.myVisitDao().byStore("BN-1")!!.synced)
        assertEquals("STATS", room.pendingOpDao().all().single().type)
    }
}
