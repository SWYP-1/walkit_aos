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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

/**
 * 인증 토큰 보관용 DataStore 래퍼
 */
@Singleton
class AuthDataStore @Inject constructor(
    @Named("auth") private val dataStore: DataStore<Preferences>,
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val providerKey = stringPreferencesKey("provider")

    val accessToken: Flow<String?> = dataStore.data.map { prefs -> prefs[accessTokenKey] }
    val refreshToken: Flow<String?> = dataStore.data.map { prefs -> prefs[refreshTokenKey] }
    val provider: Flow<String?> = dataStore.data.map { prefs -> prefs[providerKey] }

    suspend fun saveTokens(accessToken: String, refreshToken: String?, provider: String? = null) {
        dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            if (refreshToken != null) {
                prefs[refreshTokenKey] = refreshToken
            }
            if (provider != null) {
                prefs[providerKey] = provider
            }
        }
    }

    suspend fun saveProvider(provider: String) {
        dataStore.edit { prefs ->
            prefs[providerKey] = provider
        }
    }

    suspend fun getProvider(): String? {
        return dataStore.data.first()[providerKey]
    }

    fun getProviderFlow(): Flow<String?> {
        return dataStore.data.map { prefs -> prefs[providerKey] }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
            prefs.remove(providerKey)
        }
    }
}


