package com.hipka.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "hipka_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    }

    val currentUserId: Flow<String?> =
        context.sessionDataStore.data.map { it[Keys.CURRENT_USER_ID] }

    val isLoggedIn: Flow<Boolean> =
        currentUserId.map { !it.isNullOrBlank() }

    suspend fun setCurrentUser(userId: String) {
        context.sessionDataStore.edit { it[Keys.CURRENT_USER_ID] = userId }
    }

    suspend fun clearCurrentUser() {
        context.sessionDataStore.edit { it.remove(Keys.CURRENT_USER_ID) }
    }
}