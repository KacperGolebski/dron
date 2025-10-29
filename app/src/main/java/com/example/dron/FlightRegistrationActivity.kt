package com.example.dron

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FlightRegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_registration)

        // Inicjalizacja widoków z layoutu
        val startTimeEditText: EditText = findViewById(R.id.startTimeEditText)
        val endTimeEditText: EditText = findViewById(R.id.endTimeEditText)
        val droneNameEditText: EditText = findViewById(R.id.droneNameEditText)
        val confirmButton: Button = findViewById(R.id.confirmRegistrationButton)

        // Obsługa kliknięcia przycisku potwierdzenia
        confirmButton.setOnClickListener {
            // Pobranie danych z pól tekstowych
            val startTime = startTimeEditText.text.toString()
            val endTime = endTimeEditText.text.toString()
            val droneName = droneNameEditText.text.toString()

            // Sprawdzenie, czy pola nie są puste
            if (startTime.isNotBlank() && endTime.isNotBlank() && droneName.isNotBlank()) {
                // Utworzenie wiadomości podsumowującej
                val summary = "Lot zarejestrowany:\nNazwa drona: $droneName\nStart: $startTime\nKoniec: $endTime"

                // Wyświetlenie komunikatu Toast
                Toast.makeText(this, summary, Toast.LENGTH_LONG).show()

                // Zakończenie aktywności i powrót do ekranu głównego
                finish()
            } else {
                // Informacja dla użytkownika o konieczności wypełnienia wszystkich pól
                Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            }
        }
    }
}