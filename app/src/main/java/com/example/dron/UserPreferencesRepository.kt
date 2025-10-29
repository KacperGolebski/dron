package com.example.dron

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Rozszerzenie do Context, aby zapewnić jedną instancję DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.dataStore

    // Obiekt do przechowywania kluczy
    private object PreferencesKeys {
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    // Przepływ (Flow) do obserwowania danych użytkownika
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map {
        preferences ->
            val username = preferences[PreferencesKeys.USERNAME] ?: ""
            val password = preferences[PreferencesKeys.PASSWORD] ?: ""
            val isLoggedIn = preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
            UserPreferences(username, password, isLoggedIn)
    }

    // Funkcja do aktualizacji statusu zalogowania
    suspend fun updateLoginStatus(isLoggedIn: Boolean) {
        dataStore.edit {
            preferences -> preferences[PreferencesKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

    // Funkcja do zapisania danych użytkownika (rejestracja)
    suspend fun saveUser(username: String, password: String) {
        dataStore.edit {
            preferences ->
                preferences[PreferencesKeys.USERNAME] = username
                preferences[PreferencesKeys.PASSWORD] = password
                preferences[PreferencesKeys.IS_LOGGED_IN] = true // Automatycznie zaloguj po rejestracji
        }
    }
}

// Klasa danych reprezentująca preferencje użytkownika
data class UserPreferences(val username: String, val password: String, val isLoggedIn: Boolean)