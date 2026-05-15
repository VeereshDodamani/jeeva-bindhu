package com.example.jeeva_binduhealthcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.jeeva_binduhealthcare.databinding.ActivityMainBinding
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firebaseHelper = FirebaseHelper()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchFcmToken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupAvailabilitySwitch()
        checkEligibility()
        askNotificationPermission()
    }

    private fun setupClickListeners() {
        binding.btnRegisterDonor.setOnClickListener {
            startActivity(Intent(this, RegisterDonorActivity::class.java))
        }

        binding.btnViewMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.btnViewAlerts.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        binding.btnPostEmergency.setOnClickListener {
            startActivity(Intent(this, PostEmergencyActivity::class.java))
        }
    }

    private fun setupAvailabilitySwitch() {
        binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            // In a real app, you'd use the actual logged-in user's ID
            val prefs = getSharedPreferences("JeevaBindu", MODE_PRIVATE)
            val donorId = prefs.getString("current_donor_id", null)
            
            if (donorId != null) {
                lifecycleScope.launch {
                    val result = firebaseHelper.updateDonorAvailability(donorId, isChecked)
                    if (result.isSuccess) {
                        val status = if (isChecked) "Available" else "Unavailable"
                        Toast.makeText(this@MainActivity, "Status updated to $status", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // If not registered yet, just show a message
                if (isChecked) {
                    Toast.makeText(this, "Please register as a donor first", Toast.LENGTH_SHORT).show()
                    binding.switchAvailability.isChecked = false
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                fetchFcmToken()
            }
        } else {
            fetchFcmToken()
        }
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            // Store or update token in Firestore if user is registered
        }
    }

    private fun checkEligibility() {
        val prefs = getSharedPreferences("JeevaBindu", MODE_PRIVATE)
        val lastDonation = prefs.getLong("last_donation_date", 0L)
        
        val donor = Donor(lastDonationDate = lastDonation)
        if (donor.isEligible) {
            binding.tvEligibilityStatus.text = "Eligibility: READY TO DONATE"
            binding.tvEligibilityStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(donor.nextEligibilityDate!!)
            binding.tvEligibilityStatus.text = "Eligibility: Not yet (Wait until $dateStr)"
            binding.tvEligibilityStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        }
    }
}
