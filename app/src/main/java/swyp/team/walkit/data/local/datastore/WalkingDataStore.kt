package swyp.team.walkit.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * 산책 관련 데이터 보관용 DataStore
 *
 * 산책 상태, 시작 시간, 걸음 수 등의 데이터를 저장하고 관리합니다.
 */
@Singleton
class WalkingDataStore @Inject constructor(
    @Named("walking") private val dataStore: DataStore<Preferences>,
) {
    private object PreferencesKeys {
        val IS_WALKING_ACTIVE = booleanPreferencesKey("is_walking_active")
        val WALKING_START_TIME = longPreferencesKey("walking_start_time")
        val WALKING_STEP_COUNT = intPreferencesKey("walking_step_count")
        val WALKING_DURATION = longPreferencesKey("walking_duration")
        val WALKING_IS_PAUSED = booleanPreferencesKey("walking_is_paused")
        val PRE_WALKING_EMOTION = stringPreferencesKey("pre_walking_emotion")
        val POST_WALKING_EMOTION = stringPreferencesKey("post_walking_emotion")
    }

    // Flow 프로퍼티들
    val isWalkingActive: Flow<Boolean?> = dataStore.data.map { prefs -> prefs[PreferencesKeys.IS_WALKING_ACTIVE] }
    val walkingStartTime: Flow<Long?> = dataStore.data.map { prefs -> prefs[PreferencesKeys.WALKING_START_TIME] }
    val walkingStepCount: Flow<Int?> = dataStore.data.map { prefs -> prefs[PreferencesKeys.WALKING_STEP_COUNT] }
    val walkingDuration: Flow<Long?> = dataStore.data.map { prefs -> prefs[PreferencesKeys.WALKING_DURATION] }
    val walkingIsPaused: Flow<Boolean?> = dataStore.data.map { prefs -> prefs[PreferencesKeys.WALKING_IS_PAUSED] }
    val preWalkingEmotion: Flow<String?> = dataStore.data.map { prefs -> prefs[PreferencesKeys.PRE_WALKING_EMOTION] }
    val postWalkingEmotion: Flow<String?> = dataStore.data.map { prefs -> prefs[PreferencesKeys.POST_WALKING_EMOTION] }

    // Setter 메소드들
    suspend fun setWalkingActive(active: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.IS_WALKING_ACTIVE] = active }
    }

    suspend fun setWalkingStartTime(time: Long) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.WALKING_START_TIME] = time }
    }

    suspend fun setWalkingStepCount(count: Int) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.WALKING_STEP_COUNT] = count }
    }

    suspend fun setWalkingDuration(duration: Long) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.WALKING_DURATION] = duration }
    }

    suspend fun setWalkingPaused(paused: Boolean) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.WALKING_IS_PAUSED] = paused }
    }

    suspend fun setPreWalkingEmotion(emotion: String) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.PRE_WALKING_EMOTION] = emotion }
    }

    suspend fun setPostWalkingEmotion(emotion: String) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.POST_WALKING_EMOTION] = emotion }
    }

    // Getter 메소드들 (suspend)
    suspend fun getWalkingStartTime(): Long? {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.WALKING_START_TIME)
    }

    suspend fun getWalkingStepCount(): Int? {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.WALKING_STEP_COUNT)
    }

    suspend fun getWalkingDuration(): Long? {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.WALKING_DURATION)
    }

    suspend fun getWalkingIsPaused(): Boolean? {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.WALKING_IS_PAUSED)
    }

    suspend fun getIsWalkingActive(): Boolean? {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.IS_WALKING_ACTIVE)
    }

    suspend fun getPreWalkingEmotion(): String? {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.PRE_WALKING_EMOTION)
    }

    suspend fun getPostWalkingEmotion(): String? {
        return dataStore.data.firstOrNull()?.get(PreferencesKeys.POST_WALKING_EMOTION)
    }

    // 데이터 초기화 메소드
    suspend fun clearWalkingData() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.IS_WALKING_ACTIVE)
            prefs.remove(PreferencesKeys.WALKING_START_TIME)
            prefs.remove(PreferencesKeys.WALKING_STEP_COUNT)
            prefs.remove(PreferencesKeys.WALKING_DURATION)
            prefs.remove(PreferencesKeys.WALKING_IS_PAUSED)
            prefs.remove(PreferencesKeys.PRE_WALKING_EMOTION)
            prefs.remove(PreferencesKeys.POST_WALKING_EMOTION)
        }
    }
}
