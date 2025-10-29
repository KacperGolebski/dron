package com.example.dron

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(private val userPreferencesRepository: UserPreferencesRepository) : ViewModel() {

    // Przepływ stanu (StateFlow) do obserwowania danych użytkownika w UI
    val uiState: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences("", "", false)
        )

    // Funkcja do logowania
    fun login(username: String, password: String): Boolean {
        return if (username == uiState.value.username && password == uiState.value.password) {
            viewModelScope.launch {
                userPreferencesRepository.updateLoginStatus(true)
            }
            true
        } else {
            false
        }
    }

    // Funkcja do rejestracji
    fun register(username: String, password: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveUser(username, password)
        }
    }

    // Funkcja do wylogowania
    fun logout() {
        viewModelScope.launch {
            userPreferencesRepository.updateLoginStatus(false)
        }
    }
}

// Fabryka do tworzenia instancji AccountViewModel z repozytorium
class AccountViewModelFactory(private val repository: UserPreferencesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}