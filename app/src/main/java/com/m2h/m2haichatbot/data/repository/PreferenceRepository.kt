package com.m2h.m2haichatbot.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val DEFAULT_MODEL_KEY = stringPreferencesKey("default_model")
    private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback")

    private val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
    private val OPENAI_KEY = stringPreferencesKey("openai_api_key")
    private val GROQ_KEY = stringPreferencesKey("groq_api_key")

    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "system" }
    val defaultModel: Flow<String> = context.dataStore.data.map { it[DEFAULT_MODEL_KEY] ?: "meta/llama-3.1-70b-instruct" }
    val isHapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_FEEDBACK_KEY] ?: true }
    
    val geminiApiKey: Flow<String> = context.dataStore.data.map { it[GEMINI_KEY] ?: "" }
    val openaiApiKey: Flow<String> = context.dataStore.data.map { it[OPENAI_KEY] ?: "" }
    val groqApiKey: Flow<String> = context.dataStore.data.map { it[GROQ_KEY] ?: "" }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_KEY] = mode }
    }

    suspend fun setDefaultModel(modelId: String) {
        context.dataStore.edit { it[DEFAULT_MODEL_KEY] = modelId }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HAPTIC_FEEDBACK_KEY] = enabled }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { it[GEMINI_KEY] = key }
    }

    suspend fun setOpenaiApiKey(key: String) {
        context.dataStore.edit { it[OPENAI_KEY] = key }
    }

    suspend fun setGroqApiKey(key: String) {
        context.dataStore.edit { it[GROQ_KEY] = key }
    }
}
