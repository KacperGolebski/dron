package com.example.dron

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class FlightViewModel(application: Application) : AndroidViewModel(application) {

    private val flightDao = AppDatabase.getDatabase(application).flightDao()

    // Przepływ stanu z listą wszystkich lotów
    val allFlights: StateFlow<List<Flight>> = flightDao.getAllFlights()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Stan formularza rejestracji lotu ---
    private val _startTime = MutableStateFlow("")
    val startTime: StateFlow<String> = _startTime.asStateFlow()

    private val _endTime = MutableStateFlow("")
    val endTime: StateFlow<String> = _endTime.asStateFlow()

    private val _selectedDrone = MutableStateFlow("")
    val selectedDrone: StateFlow<String> = _selectedDrone.asStateFlow()

    private val _selectedFlightType = MutableStateFlow("Dydaktyczny")
    val selectedFlightType: StateFlow<String> = _selectedFlightType.asStateFlow()

    private val _additionalInfo = MutableStateFlow("")
    val additionalInfo: StateFlow<String> = _additionalInfo.asStateFlow()

    private val _location = MutableStateFlow<GeoPoint?>(null)
    val location: StateFlow<GeoPoint?> = _location.asStateFlow()

    // Stan informujący, czy formularz jest kompletny
    val isFormComplete: StateFlow<Boolean> = combine(
        _selectedDrone, _startTime, _endTime, _location
    ) { drone, start, end, loc ->
        drone.isNotBlank() && start.isNotBlank() && end.isNotBlank() && loc != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Funkcje do aktualizacji stanu formularza ---
    fun onStartTimeChange(time: String) { _startTime.value = time }
    fun onEndTimeChange(time: String) { _endTime.value = time }
    fun onDroneChange(drone: String) { _selectedDrone.value = drone }
    fun onFlightTypeChange(type: String) { _selectedFlightType.value = type }
    fun onAdditionalInfoChange(info: String) { _additionalInfo.value = info }
    fun onLocationChange(geoPoint: GeoPoint?) { _location.value = geoPoint }

    // Funkcja do dodawania lotu
    fun addFlight() {
        if (isFormComplete.value) {
            viewModelScope.launch {
                val newFlight = Flight(
                    droneModel = _selectedDrone.value,
                    startTime = _startTime.value,
                    endTime = _endTime.value,
                    latitude = _location.value!!.latitude,
                    longitude = _location.value!!.longitude,
                    flightType = _selectedFlightType.value,
                    additionalInfo = _additionalInfo.value
                )
                flightDao.insertFlight(newFlight)
                resetFormState()
            }
        }
    }

    // Funkcja do usuwania lotu
    fun deleteFlight(flight: Flight) {
        viewModelScope.launch {
            flightDao.deleteFlight(flight)
        }
    }

    // Funkcja do resetowania stanu formularza
    private fun resetFormState() {
        _startTime.value = ""
        _endTime.value = ""
        _selectedDrone.value = ""
        _selectedFlightType.value = "Dydaktyczny"
        _additionalInfo.value = ""
        _location.value = null
    }
}