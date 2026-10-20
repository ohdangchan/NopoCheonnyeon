package com.nopo.cheonnyeon.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboardingDoneKey] ?: false }
    val foodOnly: Flow<Boolean> = context.dataStore.data.map { it[foodOnlyKey] ?: false }

    /** 시연용 예시 리뷰 표시(해커톤 시연 편의상 기본 켜짐) */
    val demoData: Flow<Boolean> = context.dataStore.data.map { it[demoDataKey] ?: true }

    /** 글자 확인이 3번 실패해도 30m 안이면 위치 확인 도장을 허용 (FR-VER-11) */
    val allowLocationOnly: Flow<Boolean> = context.dataStore.data.map { it[locationOnlyKey] ?: com.nopo.cheonnyeon.domain.rule.VerificationConfig.ALLOW_LOCATION_ONLY_AFTER_OCR_FAIL }

    /** 시험용 가게에 남긴 내 리뷰(별점|시각|글). 시험용 가게는 서버에 없어서 이 기기에만 저장한다 */
    suspend fun testReview(storeId: String): Triple<Int, Long, String>? {
        val raw = context.dataStore.data.first()[stringPreferencesKey(TEST_REVIEW_PREFIX + storeId)] ?: return null
        val parts = raw.split("|", limit = 3)
        if (parts.size < 3) return null
        return Triple(parts[0].toIntOrNull() ?: return null, parts[1].toLongOrNull() ?: return null, parts[2])
    }

    suspend fun setTestReview(storeId: String, rating: Int, text: String, atMillis: Long, photos: List<String> = emptyList()) {
        context.dataStore.edit {
            it[stringPreferencesKey(TEST_REVIEW_PREFIX + storeId)] = "$rating|$atMillis|$text"
            if (photos.isNotEmpty()) it[stringPreferencesKey(TEST_REVIEW_PHOTO_PREFIX + storeId)] = photos.joinToString("\n") else it.remove(stringPreferencesKey(TEST_REVIEW_PHOTO_PREFIX + storeId))
        }
    }

    /** 시험용 가게 내 리뷰의 사진들(base64 WebP). 없으면 빈 목록 */
    suspend fun testReviewPhotos(storeId: String): List<String> =
        context.dataStore.data.first()[stringPreferencesKey(TEST_REVIEW_PHOTO_PREFIX + storeId)]?.split("\n")?.filter { it.isNotBlank() }.orEmpty()

    suspend fun deleteTestReview(storeId: String) {
        context.dataStore.edit {
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
    suspend fun setDemoData(v: Boolean) { context.dataStore.edit { it[demoDataKey] = v } }
    suspend fun setFoodOnly(v: Boolean) { context.dataStore.edit { it[foodOnlyKey] = v } }

    private companion object {
        const val TEST_REVIEW_PREFIX = "test_review_"
        const val TEST_REVIEW_PHOTO_PREFIX = "test_review_photo_"
    }
}
