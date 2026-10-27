package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val SENSORY_SENSITIVITY = stringPreferencesKey("sensory_sensitivity")
        val COMM_FAMILY = stringPreferencesKey("comm_family")
        val COMM_FRIENDS = stringPreferencesKey("comm_friends")
        val COMM_STRANGERS = stringPreferencesKey("comm_strangers")
        val CAREGIVER_NAME = stringPreferencesKey("caregiver_name")
        val CAREGIVER_PHONE = stringPreferencesKey("caregiver_phone")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[IS_ONBOARDING_COMPLETED] ?: false }
    val sensorySensitivity: Flow<String> = context.dataStore.data.map { it[SENSORY_SENSITIVITY] ?: "1" }
    
    val commFamily: Flow<String> = context.dataStore.data.map { it[COMM_FAMILY] ?: "Easily" }
    val commFriends: Flow<String> = context.dataStore.data.map { it[COMM_FRIENDS] ?: "Easily" }
    val commStrangers: Flow<String> = context.dataStore.data.map { it[COMM_STRANGERS] ?: "Easily" }

    val caregiverName: Flow<String> = context.dataStore.data.map { it[CAREGIVER_NAME] ?: "sanjiv k" }
    val caregiverPhone: Flow<String> = context.dataStore.data.map { it[CAREGIVER_PHONE] ?: "8925081353" }

    suspend fun saveOnboardingData(sensitivity: String, family: String, friends: String, strangers: String, caregiverPhoneStr: String) {
        context.dataStore.edit { prefs ->
            prefs[SENSORY_SENSITIVITY] = sensitivity
            prefs[COMM_FAMILY] = family
            prefs[COMM_FRIENDS] = friends
            prefs[COMM_STRANGERS] = strangers
            prefs[CAREGIVER_PHONE] = caregiverPhoneStr
            prefs[IS_ONBOARDING_COMPLETED] = true
        }
    }
    
    suspend fun clearOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[IS_ONBOARDING_COMPLETED] = false
        }
    }
}
