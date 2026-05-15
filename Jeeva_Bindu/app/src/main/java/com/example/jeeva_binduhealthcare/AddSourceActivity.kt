package com.example.jeeva_binduhealthcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.jeeva_binduhealthcare.databinding.ActivityAddSourceBinding
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class AddSourceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSourceBinding
    private val firebaseHelper = FirebaseHelper()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSourceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        binding.btnFetchLocation.setOnClickListener {
            checkLocationPermissions()
        }

        binding.btnSave.setOnClickListener {
            saveSource()
        }
    }

    private fun setupSpinners() {
        val types = arrayOf("Well", "Borewell", "Lake", "Tank")
        val statusList = arrayOf("Available", "Low", "Dry", "Contaminated")

        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        binding.spinnerType.setAdapter(typeAdapter)

        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusList)
        binding.spinnerStatus.setAdapter(statusAdapter)
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    binding.etLatitude.setText(it.latitude.toString())
                    binding.etLongitude.setText(it.longitude.toString())
                } ?: Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSource() {
        val lat = binding.etLatitude.text.toString().toDoubleOrNull()
        val lng = binding.etLongitude.text.toString().toDoubleOrNull()
        val type = binding.spinnerType.text.toString()
        val status = binding.spinnerStatus.text.toString()

        if (lat == null || lng == null || type.isEmpty() || status.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val source = WaterSource(
            latitude = lat,
            longitude = lng,
            type = type,
            status = status
        )

        lifecycleScope.launch {
            binding.btnSave.isEnabled = false
            val result = firebaseHelper.addWaterSource(source)
            binding.btnSave.isEnabled = true
            
            if (result.isSuccess) {
                Toast.makeText(this@AddSourceActivity, "Source added successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AddSourceActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
