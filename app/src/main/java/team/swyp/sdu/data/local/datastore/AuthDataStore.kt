package team.swyp.sdu.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 인증 토큰 보관용 DataStore 래퍼
 */
@Singleton
class AuthDataStore @Inject constructor(
    @Named("auth") private val dataStore: DataStore<Preferences>,
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val accessToken: Flow<String?> = dataStore.data.map { prefs -> prefs[accessTokenKey] }
    val refreshToken: Flow<String?> = dataStore.data.map { prefs -> prefs[refreshTokenKey] }

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            if (refreshToken != null) {
                prefs[refreshTokenKey] = refreshToken
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
        }
    }
}


