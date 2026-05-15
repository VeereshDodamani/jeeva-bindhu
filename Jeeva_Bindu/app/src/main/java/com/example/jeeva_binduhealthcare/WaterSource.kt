package com.example.jeeva_binduhealthcare

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WaterSource(
    var id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val type: String = "",
    val status: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
