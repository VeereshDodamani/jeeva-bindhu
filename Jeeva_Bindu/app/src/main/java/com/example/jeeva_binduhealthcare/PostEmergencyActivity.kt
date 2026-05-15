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
import com.example.jeeva_binduhealthcare.databinding.ActivityPostEmergencyBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

class PostEmergencyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostEmergencyBinding

    private val firebaseHelper = FirebaseHelper()

    private var requesterLat: Double = 0.0
    private var requesterLng: Double = 0.0

    private val bangaloreHospitals = mapOf(
        "Victoria Hospital" to LatLng(12.9634, 77.5752),
        "St. John's Medical College Hospital" to LatLng(12.9343, 77.6133),
        "Manipal Hospital" to LatLng(12.9622, 77.6483),
        "Narayana Health City" to LatLng(12.8258, 77.6917),
        "NIMHANS" to LatLng(12.9429, 77.5912)
    )

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                fetchRequesterLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityPostEmergencyBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setupHospitalSpinner()

        setupBloodGroupSpinner()

        checkLocationPermissions()

        binding.btnFetchLocation.setOnClickListener {
            fetchCurrentLocation()
        }

        binding.btnPost.setOnClickListener {
            postEmergency()
        }
    }

    private fun setupHospitalSpinner() {

        val hospitalNames =
            bangaloreHospitals.keys.toList()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            hospitalNames
        )

        val hospitalView =
            binding.etHospitalName as AutoCompleteTextView

        hospitalView.setAdapter(adapter)

        hospitalView.setOnItemClickListener { parent, _, position, _ ->

            val selectedHospital =
                parent.getItemAtPosition(position).toString()

            val location =
                bangaloreHospitals[selectedHospital]

            if (location != null) {

                binding.etLat.setText(location.latitude.toString())

                binding.etLng.setText(location.longitude.toString())
            }
        }
    }

    private fun setupBloodGroupSpinner() {

        val bloodGroups = arrayOf(
            "A+",
            "A-",
            "B+",
            "B-",
            "O+",
            "O-",
            "AB+",
            "AB-"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            bloodGroups
        )

        val bloodGroupView =
            binding.spinnerBloodGroup as AutoCompleteTextView

        bloodGroupView.setAdapter(adapter)
    }

    private fun checkLocationPermissions() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

        } else {

            fetchRequesterLocation()
        }
    }

    private fun fetchRequesterLocation() {

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        val cts = CancellationTokenSource()

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->

                    if (location != null) {
                        requesterLat = location.latitude
                        requesterLng = location.longitude
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                requesterLat = lastLoc.latitude
                                requesterLng = lastLoc.longitude
                            }
                        }
                    }
                }
        }
    }

    private fun fetchCurrentLocation() {

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        val cts = CancellationTokenSource()

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            
            Toast.makeText(this, "Fetching location...", Toast.LENGTH_SHORT).show()

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->

                    if (location != null) {

                        binding.etLat.setText(location.latitude.toString())

                        binding.etLng.setText(location.longitude.toString())

                        if (requesterLat == 0.0) {

                            requesterLat = location.latitude

                            requesterLng = location.longitude
                        }

                        binding.etHospitalName.setText("Current Location")
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                binding.etLat.setText(lastLoc.latitude.toString())
                                binding.etLng.setText(lastLoc.longitude.toString())
                                if (requesterLat == 0.0) {
                                    requesterLat = lastLoc.latitude
                                    requesterLng = lastLoc.longitude
                                }
                                binding.etHospitalName.setText("Current Location (Last Known)")
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

    private fun postEmergency() {

        val hospital =
            binding.etHospitalName.text.toString()

        val location =
            binding.etLocation.text.toString()

        val bloodGroup =
            binding.spinnerBloodGroup.text.toString()

        val phone =
            binding.etContactPhone.text.toString()

        val lat =
            binding.etLat.text.toString().toDoubleOrNull()

        val lng =
            binding.etLng.text.toString().toDoubleOrNull()

        if (
            hospital.isEmpty() ||
            location.isEmpty() ||
            bloodGroup.isEmpty() ||
            phone.isEmpty() ||
            lat == null ||
            lng == null
        ) {

            Toast.makeText(
                this,
                "Please fill all fields",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val request = EmergencyRequest(
            hospitalName = hospital,
            location = location,
            bloodGroupRequired = bloodGroup,
            contactPhone = phone,
            latitude = lat,
            longitude = lng,
            requesterLatitude = requesterLat,
            requesterLongitude = requesterLng,
            status = "ACTIVE"
        )

        lifecycleScope.launch {

            binding.btnPost.isEnabled = false

            val result =
                firebaseHelper.postEmergency(request)

            binding.btnPost.isEnabled = true

            if (result.isSuccess) {

                Toast.makeText(
                    this@PostEmergencyActivity,
                    "Emergency Alert Sent!",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            } else {

                Toast.makeText(
                    this@PostEmergencyActivity,
                    "Error: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
