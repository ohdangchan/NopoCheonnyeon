package com.nopo.cheonnyeon.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
    private val demoDataKey = booleanPreferencesKey("demo_data")

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboardingDoneKey] ?: false }
    val foodOnly: Flow<Boolean> = context.dataStore.data.map { it[foodOnlyKey] ?: false }

    /** 시연용 예시 리뷰 표시(해커톤 시연 편의상 기본 켜짐) */
    val demoData: Flow<Boolean> = context.dataStore.data.map { it[demoDataKey] ?: true }

    suspend fun catalogVersion(): Long = context.dataStore.data.first()[catalogVersionKey] ?: 0L
    suspend fun setCatalogVersion(v: Long) { context.dataStore.edit { it[catalogVersionKey] = v } }
    suspend fun setOnboardingDone() { context.dataStore.edit { it[onboardingDoneKey] = true } }
    suspend fun setDemoData(v: Boolean) { context.dataStore.edit { it[demoDataKey] = v } }
    suspend fun setFoodOnly(v: Boolean) { context.dataStore.edit { it[foodOnlyKey] = v } }
}
