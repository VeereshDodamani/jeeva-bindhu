package com.example.jeeva_binduhealthcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jeeva_binduhealthcare.databinding.ActivityAlertsBinding
import kotlinx.coroutines.launch

class AlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsBinding
    private lateinit var adapter: AlertsAdapter
    private val firebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeAlerts()
    }

    private fun setupRecyclerView() {

        adapter = AlertsAdapter(
            alerts = emptyList(),

            onComingClicked = { alert ->
                respondToAlert(alert)
            },

            onFindDonorsClicked = { alert ->

                val intent = Intent(this, MapActivity::class.java)

                intent.putExtra("target_blood_group", alert.bloodGroupRequired)
                intent.putExtra("target_lat", alert.latitude)
                intent.putExtra("target_lng", alert.longitude)
                intent.putExtra("target_id", alert.id)
                intent.putExtra("target_hospital", alert.hospitalName)

                startActivity(intent)
            }
        )

        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvAlerts.adapter = adapter
    }

    private fun observeAlerts() {

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {

            firebaseHelper.getEmergenciesRealtime().collect { alerts ->

                binding.progressBar.visibility = View.GONE

                binding.tvEmptyState.visibility =
                    if (alerts.isEmpty()) View.VISIBLE
                    else View.GONE

                adapter.updateData(alerts)
            }
        }
    }

    private fun respondToAlert(alert: EmergencyRequest) {

        val donorName = "Registered Volunteer"

        lifecycleScope.launch {

            val result =
                firebaseHelper.respondToEmergency(
                    alert.id,
                    donorName
                )

            if (result.isSuccess) {

                Toast.makeText(
                    this@AlertsActivity,
                    "Response submitted successfully",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this@AlertsActivity,
                    "Error: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}