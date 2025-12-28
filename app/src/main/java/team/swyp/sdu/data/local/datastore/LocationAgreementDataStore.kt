package team.swyp.sdu.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 위치 동의 상태 보관용 DataStore
 *
 * 위치 권한 동의 다이얼로그 표시 여부를 저장하고 관리합니다.
 */
@Singleton
class LocationAgreementDataStore @Inject constructor(
    @Named("location_agreement") private val dataStore: DataStore<Preferences>,
) {
    private val hasShownLocationAgreementDialogKey = booleanPreferencesKey("has_shown_location_agreement_dialog")

    /**
     * 위치 동의 다이얼로그를 이미 표시했는지 확인
     */
    val hasShownLocationAgreementDialog: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[hasShownLocationAgreementDialogKey] ?: false }

    /**
     * 위치 동의 다이얼로그 표시 상태 설정
     */
    suspend fun setHasShownLocationAgreementDialog(hasShown: Boolean) {
        dataStore.edit { prefs -> prefs[hasShownLocationAgreementDialogKey] = hasShown }
    }

    /**
     * 위치 동의 다이얼로그 표시 여부를 확인 (Flow가 아닌 suspend 함수)
     */
    suspend fun hasShownDialog(): Boolean {
        return dataStore.data.first()[hasShownLocationAgreementDialogKey] ?: false
    }
}
