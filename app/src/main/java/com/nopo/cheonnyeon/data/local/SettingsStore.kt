package com.nopo.cheonnyeon.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/** 기기에만 저장하는 설정: 온보딩 여부, 카탈로그 버전, 필터 (S-303) */
class SettingsStore(private val context: Context) {
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val catalogVersionKey = longPreferencesKey("catalog_version")
    private val foodOnlyKey = booleanPreferencesKey("filter_food_only")
    private val locationOnlyKey = booleanPreferencesKey("allow_location_only")
    private val demoDataKey = booleanPreferencesKey("demo_data")
    private val aiConsentKey = booleanPreferencesKey("ai_consent")
    private val bookmarksKey = stringSetPreferencesKey("bookmarks")
    private val notifyNewsKey = booleanPreferencesKey("notify_news")
    private val notifyRecoKey = booleanPreferencesKey("notify_reco")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboardingDoneKey] ?: false }
    val foodOnly: Flow<Boolean> = context.dataStore.data.map { it[foodOnlyKey] ?: false }

    /** 시연용 예시 리뷰 표시(해커톤 시연 편의상 기본 켜짐) */
    val demoData: Flow<Boolean> = context.dataStore.data.map { it[demoDataKey] ?: true }

    /** 글자 확인이 3번 실패해도 30m 안이면 위치 확인 도장을 허용 (FR-VER-11) */
    val allowLocationOnly: Flow<Boolean> = context.dataStore.data.map { it[locationOnlyKey] ?: com.nopo.cheonnyeon.domain.rule.VerificationConfig.ALLOW_LOCATION_ONLY_AFTER_OCR_FAIL }

    /** 시험용 가게에 남긴 내 리뷰들(최신순). 시험용 가게는 서버에 없어서 이 기기에만 저장한다. 예전 한 개짜리 기록은 읽을 때 옮겨 담는다 */
    suspend fun testReviews(storeId: String): List<TestReview> {
        val prefs = context.dataStore.data.first()
        val list = prefs[stringPreferencesKey(TEST_REVIEW_LIST_PREFIX + storeId)]?.let { raw ->
            runCatching { testJson.decodeFromString<List<TestReview>>(raw) }.getOrNull()
        }
        if (list != null) return list.sortedByDescending { it.at }
        val legacy = prefs[stringPreferencesKey(TEST_REVIEW_PREFIX + storeId)]?.split("|", limit = 3)
        if (legacy == null || legacy.size < 3) return emptyList()
        val photos = prefs[stringPreferencesKey(TEST_REVIEW_PHOTO_PREFIX + storeId)]?.split("\n")?.filter { it.isNotBlank() }.orEmpty()
        return listOf(TestReview("local_legacy", legacy[0].toIntOrNull() ?: return emptyList(), legacy[1].toLongOrNull() ?: return emptyList(), legacy[2], photos))
    }

    /** 같은 id가 있으면 고치고, 없으면 새로 쌓는다 */
    suspend fun saveTestReview(storeId: String, review: TestReview) {
        val current = (testReviews(storeId).filter { it.id != review.id } + review).sortedByDescending { it.at }.take(MAX_TEST_REVIEWS)
        context.dataStore.edit {
            it[stringPreferencesKey(TEST_REVIEW_LIST_PREFIX + storeId)] = testJson.encodeToString(current)
            it.remove(stringPreferencesKey(TEST_REVIEW_PREFIX + storeId))
            it.remove(stringPreferencesKey(TEST_REVIEW_PHOTO_PREFIX + storeId))
        }
    }

    suspend fun deleteTestReview(storeId: String, reviewId: String) {
        val rest = testReviews(storeId).filter { it.id != reviewId }
        context.dataStore.edit {
            it[stringPreferencesKey(TEST_REVIEW_LIST_PREFIX + storeId)] = testJson.encodeToString(rest)
            it.remove(stringPreferencesKey(TEST_REVIEW_PREFIX + storeId))
            it.remove(stringPreferencesKey(TEST_REVIEW_PHOTO_PREFIX + storeId))
        }
    }

    suspend fun clearTestReviews() {
        context.dataStore.edit { p -> p.asMap().keys.filter { it.name.startsWith(TEST_REVIEW_PREFIX) }.forEach { p.remove(it) } }
    }

    suspend fun catalogVersion(): Long = context.dataStore.data.first()[catalogVersionKey] ?: 0L
    suspend fun setCatalogVersion(v: Long) { context.dataStore.edit { it[catalogVersionKey] = v } }
    suspend fun setOnboardingDone() { context.dataStore.edit { it[onboardingDoneKey] = true } }
    suspend fun setAllowLocationOnly(v: Boolean) { context.dataStore.edit { it[locationOnlyKey] = v } }
    /** AI 기능 안내(리뷰 글·음식 사진이 Google Gemini로 전송됨)에 동의했는지 */
    val aiConsent: Flow<Boolean> = context.dataStore.data.map { it[aiConsentKey] ?: false }
    /** 저장한 노포(북마크). 이 기기에만 저장하고 서버에는 올리지 않는다. */
    /** 알림 설정 (새 소식·오늘의 추천). 기기 안에 저장만 하고, 알림 목록 화면이 이 값을 따른다 */
    val notifyNews: Flow<Boolean> = context.dataStore.data.map { it[notifyNewsKey] ?: true }
    val notifyReco: Flow<Boolean> = context.dataStore.data.map { it[notifyRecoKey] ?: true }
    suspend fun setNotifyNews(v: Boolean) { context.dataStore.edit { it[notifyNewsKey] = v } }
    suspend fun setNotifyReco(v: Boolean) { context.dataStore.edit { it[notifyRecoKey] = v } }

    val bookmarks: Flow<Set<String>> = context.dataStore.data.map { it[bookmarksKey] ?: emptySet() }
    suspend fun toggleBookmark(storeId: String) {
        context.dataStore.edit { p ->
            val cur = p[bookmarksKey] ?: emptySet()
            p[bookmarksKey] = if (storeId in cur) cur - storeId else cur + storeId
        }
    }
    suspend fun clearBookmarks() { context.dataStore.edit { it.remove(bookmarksKey) } }
    suspend fun setAiConsent(v: Boolean) { context.dataStore.edit { it[aiConsentKey] = v } }
    suspend fun setDemoData(v: Boolean) { context.dataStore.edit { it[demoDataKey] = v } }
    suspend fun setFoodOnly(v: Boolean) { context.dataStore.edit { it[foodOnlyKey] = v } }

    private companion object {
        const val TEST_REVIEW_PREFIX = "test_review_"
        const val TEST_REVIEW_PHOTO_PREFIX = "test_review_photo_"
        const val TEST_REVIEW_LIST_PREFIX = "test_review_list_"
        /** 시험용 가게 리뷰는 DataStore에 들어가므로 개수를 제한한다 */
        const val MAX_TEST_REVIEWS = 10
        private val testJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    }
}

/** 시험용 가게에 이 기기에서 남긴 리뷰 한 개 */
@kotlinx.serialization.Serializable
data class TestReview(val id: String, val rating: Int, val at: Long, val text: String, val photos: List<String> = emptyList())
