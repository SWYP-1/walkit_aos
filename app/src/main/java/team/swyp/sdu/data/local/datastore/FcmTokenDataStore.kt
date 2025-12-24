package team.swyp.sdu.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * FCM 토큰 보관용 DataStore
 */
@Singleton
class FcmTokenDataStore @Inject constructor(
    @Named("fcm") private val dataStore: DataStore<Preferences>,
) {
    private val fcmTokenKey = stringPreferencesKey("fcm_token")

    val fcmToken: Flow<String?> = dataStore.data.map { prefs -> prefs[fcmTokenKey] }

    suspend fun saveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[fcmTokenKey] = token
        }
    }

    suspend fun getToken(): String? {
        return dataStore.data.first()[fcmTokenKey]
    }

    suspend fun clearToken() {
        dataStore.edit { prefs ->
            prefs.remove(fcmTokenKey)
        }
    }
}

