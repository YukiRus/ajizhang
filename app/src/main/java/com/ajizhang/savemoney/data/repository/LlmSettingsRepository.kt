package com.ajizhang.savemoney.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajizhang.savemoney.data.model.LlmSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "llm_settings")

@Singleton
class LlmSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val apiBaseUrlKey = stringPreferencesKey("api_base_url")
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val modelNameKey = stringPreferencesKey("model_name")

    fun observeSettings(): Flow<LlmSettings> =
        context.settingsDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                LlmSettings(
                    apiBaseUrl = preferences[apiBaseUrlKey].orEmpty(),
                    apiKey = preferences[apiKeyKey].orEmpty(),
                    modelName = preferences[modelNameKey].orEmpty(),
                )
            }

    suspend fun getSettings(): LlmSettings = observeSettings().first()

    suspend fun saveSettings(
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[apiBaseUrlKey] = apiBaseUrl.trim()
            preferences[apiKeyKey] = apiKey.trim()
            preferences[modelNameKey] = modelName.trim()
        }
    }
}
