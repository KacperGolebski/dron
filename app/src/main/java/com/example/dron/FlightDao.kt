package com.example.dron

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Query("SELECT * FROM flights ORDER BY id DESC")
    fun getAllFlights(): Flow<List<Flight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: Flight)

    @Delete
    suspend fun deleteFlight(flight: Flight)
}