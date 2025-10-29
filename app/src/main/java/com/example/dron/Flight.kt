package com.example.dron

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class Flight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val droneModel: String,
    val startTime: String,
    val endTime: String,
    val latitude: Double,
    val longitude: Double,
    val flightType: String = "",
    val additionalInfo: String = ""
)