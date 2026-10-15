package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.TutorSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserProfile(
    val studentName: String = "Student",
    val educationLevel: String = "College",
    val studyGoal: String = "Master exam syllabus and prepare revision notes",
    val preferredLanguage: String = "English",
    val dailyTargetMinutes: Int = 45,
    val examDateMillis: Long? = null,
    val isOnboardingCompleted: Boolean = false,
    val isDemoMode: Boolean = true,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val customApiKey: String = "",
    val backendBaseUrl: String = "https://api.studymate.digitech.com"
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val EDUCATION_LEVEL = stringPreferencesKey("education_level")
        val STUDY_GOAL = stringPreferencesKey("study_goal")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val DAILY_TARGET_MINUTES = intPreferencesKey("daily_target_minutes")
        val EXAM_DATE_MILLIS = longPreferencesKey("exam_date_millis")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val IS_DEMO_MODE = booleanPreferencesKey("is_demo_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")

        // Tutor settings
        val TUTOR_LEVEL = stringPreferencesKey("tutor_level")
        val TUTOR_LENGTH = stringPreferencesKey("tutor_length")
        val TUTOR_STYLE = stringPreferencesKey("tutor_style")
        val TUTOR_EXAMPLES = booleanPreferencesKey("tutor_examples")
        val TUTOR_RECAP = booleanPreferencesKey("tutor_recap")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            studentName = prefs[Keys.STUDENT_NAME] ?: "Alex",
            educationLevel = prefs[Keys.EDUCATION_LEVEL] ?: "College",
            studyGoal = prefs[Keys.STUDY_GOAL] ?: "Master STEM & Humanities topics with step-by-step guidance",
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "English",
            dailyTargetMinutes = prefs[Keys.DAILY_TARGET_MINUTES] ?: 45,
            examDateMillis = prefs[Keys.EXAM_DATE_MILLIS],
            isOnboardingCompleted = prefs[Keys.IS_ONBOARDING_COMPLETED] ?: false,
            isDemoMode = prefs[Keys.IS_DEMO_MODE] ?: true,
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            customApiKey = prefs[Keys.CUSTOM_API_KEY] ?: "",
            backendBaseUrl = prefs[Keys.BACKEND_BASE_URL] ?: "https://api.studymate.digitech.com"
        )
    }

    val tutorSettingsFlow: Flow<TutorSettings> = context.dataStore.data.map { prefs ->
        TutorSettings(
            explanationLevel = prefs[Keys.TUTOR_LEVEL] ?: "School",
            responseLength = prefs[Keys.TUTOR_LENGTH] ?: "Balanced",
            teachingStyle = prefs[Keys.TUTOR_STYLE] ?: "Step-by-step",
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "English",
            includeExamples = prefs[Keys.TUTOR_EXAMPLES] ?: true,
            includeRecap = prefs[Keys.TUTOR_RECAP] ?: true
        )
    }

    suspend fun updateOnboarding(
        name: String,
        educationLevel: String,
        studyGoal: String,
        language: String,
        dailyTarget: Int,
        examDate: Long?
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = name
            prefs[Keys.EDUCATION_LEVEL] = educationLevel
            prefs[Keys.STUDY_GOAL] = studyGoal
            prefs[Keys.PREFERRED_LANGUAGE] = language
            prefs[Keys.DAILY_TARGET_MINUTES] = dailyTarget
            if (examDate != null) {
                prefs[Keys.EXAM_DATE_MILLIS] = examDate
            }
            prefs[Keys.IS_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setDemoMode(isDemo: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DEMO_MODE] = isDemo
        }
    }

    suspend fun updateTutorSettings(settings: TutorSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TUTOR_LEVEL] = settings.explanationLevel
            prefs[Keys.TUTOR_LENGTH] = settings.responseLength
            prefs[Keys.TUTOR_STYLE] = settings.teachingStyle
            prefs[Keys.PREFERRED_LANGUAGE] = settings.preferredLanguage
            prefs[Keys.TUTOR_EXAMPLES] = settings.includeExamples
            prefs[Keys.TUTOR_RECAP] = settings.includeRecap
        }
    }

    suspend fun updateProfile(name: String, level: String, goal: String, language: String, target: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = name
            prefs[Keys.EDUCATION_LEVEL] = level
            prefs[Keys.STUDY_GOAL] = goal
            prefs[Keys.PREFERRED_LANGUAGE] = language
            prefs[Keys.DAILY_TARGET_MINUTES] = target
        }
    }

    suspend fun updateCustomApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_API_KEY] = apiKey
        }
    }

    suspend fun updateBackendBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKEND_BASE_URL] = url
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
