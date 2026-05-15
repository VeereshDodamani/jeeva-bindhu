package com.example.jeeva_binduhealthcare

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.concurrent.TimeUnit

@IgnoreExtraProperties
data class Donor(
    var id: String = "",
    val name: String = "",
    val bloodGroup: String = "",
    val age: Int = 0,
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastDonationDate: Long = 0L,
    val phone: String = "",
    val isAvailable: Boolean = true, // For DFD Process 3.0
    val fcmToken: String? = null     // For DFD Process 4.0
) {
    @get:Exclude
    val isEligible: Boolean
        get() {
            if (lastDonationDate == 0L) return true
            val diff = System.currentTimeMillis() - lastDonationDate
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return days >= 90
        }

    @get:Exclude
    val nextEligibilityDate: Date?
        get() {
            if (lastDonationDate == 0L) return null
            return Date(lastDonationDate + TimeUnit.DAYS.toMillis(90))
        }
}

@IgnoreExtraProperties
data class EmergencyRequest(
    var id: String = "",
    val hospitalName: String = "",
    val location: String = "",
    val bloodGroupRequired: String = "",
    val contactPhone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val requesterLatitude: Double = 0.0,
    val requesterLongitude: Double = 0.0,
    var status: String = "ACTIVE", 
    val responders: List<String> = emptyList(),
    @ServerTimestamp val timestamp: Date? = null
)
