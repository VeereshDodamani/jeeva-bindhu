package com.example.jeeva_binduhealthcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.jeeva_binduhealthcare.databinding.ActivityRegisterDonorBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

class RegisterDonorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterDonorBinding
    private val firebaseHelper = FirebaseHelper()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                fetchLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterDonorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBloodGroupSpinner()

        binding.btnFetchLocation.setOnClickListener {
            checkLocationPermissions()
        }

        binding.btnRegister.setOnClickListener {
            registerDonor()
        }
    }

    private fun setupBloodGroupSpinner() {
        val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodGroups)
        val bloodGroupView = binding.spinnerBloodGroup as AutoCompleteTextView
        bloodGroupView.setAdapter(adapter)
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            fetchLocation()
        }
    }

    private fun fetchLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val cts = CancellationTokenSource()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            Toast.makeText(this, "Fetching location...", Toast.LENGTH_SHORT).show()
            
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        binding.etLatitude.setText(location.latitude.toString())
                        binding.etLongitude.setText(location.longitude.toString())
                    } else {
                        // Fallback to last location if getCurrentLocation fails
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                binding.etLatitude.setText(lastLoc.latitude.toString())
                                binding.etLongitude.setText(lastLoc.longitude.toString())
                            } else {
                                Toast.makeText(this, "Unable to get current location. Please ensure GPS is on.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun registerDonor() {
        val name = binding.etName.text.toString()
        val bloodGroup = binding.spinnerBloodGroup.text.toString()
        val ageStr = binding.etAge.text.toString()
        val location = binding.etLocation.text.toString()
        val phone = binding.etPhone.text.toString()
        val lat = binding.etLatitude.text.toString().toDoubleOrNull() ?: 0.0
        val lng = binding.etLongitude.text.toString().toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || bloodGroup.isEmpty() || ageStr.isEmpty() || location.isEmpty() || phone.isEmpty() || lat == 0.0 || lng == 0.0) {
            Toast.makeText(this, "Please fill all fields and provide location", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull() ?: 0
        if (age < 18) {
            Toast.makeText(this, "You must be 18 or older to donate", Toast.LENGTH_SHORT).show()
            return
        }

        val donor = Donor(
            name = name, bloodGroup = bloodGroup, age = age,
            location = location, phone = phone, latitude = lat, longitude = lng,
            lastDonationDate = 0L
        )

        lifecycleScope.launch {
            binding.btnRegister.isEnabled = false
            val result = firebaseHelper.registerDonor(donor)
            binding.btnRegister.isEnabled = true

            if (result.isSuccess) {
                Toast.makeText(this@RegisterDonorActivity, "Successfully registered as donor", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@RegisterDonorActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
