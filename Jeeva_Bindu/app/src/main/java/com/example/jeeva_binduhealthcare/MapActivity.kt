package com.example.jeeva_binduhealthcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.jeeva_binduhealthcare.databinding.ActivityMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

class MapActivity : AppCompatActivity(), OnMapReadyCallback, OnMapsSdkInitializedCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mMap: GoogleMap
    private val firebaseHelper = FirebaseHelper()
    
    private var targetBloodGroup: String? = null
    private var targetLat: Double = 0.0
    private var targetLng: Double = 0.0
    private var targetId: String? = null
    
    private var currentUserLocation: Location? = null
    private var currentFilter: String = "All"
    
    private var allDonors: List<Donor> = emptyList()
    private var allEmergencies: List<EmergencyRequest> = emptyList()
    
    private var dataJob: Job? = null
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        targetBloodGroup = intent.getStringExtra("target_blood_group")
        targetLat = intent.getDoubleExtra("target_lat", 0.0)
        targetLng = intent.getDoubleExtra("target_lng", 0.0)
        targetId = intent.getStringExtra("target_id")
        
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, PostEmergencyActivity::class.java))
        }

        setupFilters()
        fetchUserLocation()
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipDonors -> "Donors"
                R.id.chipEmergencies -> "Emergencies"
                else -> "All"
            }
            // When user manually changes filter, we want to re-zoom to the new selection
            updateMarkers(shouldZoom = true)
        }
    }

    private fun fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { 
                currentUserLocation = it
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        Log.d("MapActivity", "Maps SDK Initialized: $renderer")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        
        val bangaloreCenter = LatLng(12.9716, 77.5946)
        val startLoc = if (targetLat != 0.0) LatLng(targetLat, targetLng) else bangaloreCenter
        val initialZoom = if (targetLat != 0.0) 15f else 12f
        
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLoc, initialZoom))
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }
        
        startObservingData()
    }

    private fun startObservingData() {
        dataJob?.cancel()
        dataJob = lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            
            launch {
                firebaseHelper.getDonorsRealtime()
                    .catch { e -> Log.e("MapActivity", "Donor error", e) }
                    .collect { donors ->
                        allDonors = donors
                        updateMarkers(shouldZoom = isFirstLoad)
                        if (isFirstLoad) isFirstLoad = false
                        binding.progressBar.visibility = View.GONE
                    }
            }
            
            launch {
                firebaseHelper.getEmergenciesRealtime()
                    .catch { e -> Log.e("MapActivity", "Emergency error", e) }
                    .collect { emergencies ->
                        allEmergencies = emergencies
                        updateMarkers(shouldZoom = isFirstLoad)
                        binding.progressBar.visibility = View.GONE
                    }
            }
        }
    }

    private fun updateMarkers(shouldZoom: Boolean = false) {
        if (!::mMap.isInitialized) return
        mMap.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var markerCount = 0

        // 1. Process Emergencies (Blue Markers)
        if (currentFilter == "All" || currentFilter == "Emergencies") {
            for (req in allEmergencies) {
                // If target search is active, filter strictly. Otherwise show all.
                val matchesTarget = when {
                    targetId != null -> req.id == targetId
                    targetLat != 0.0 -> abs(req.latitude - targetLat) < 0.001 && abs(req.longitude - targetLng) < 0.001
                    else -> true
                }

                if (!matchesTarget) continue

                val loc = if (req.requesterLatitude != 0.0) LatLng(req.requesterLatitude, req.requesterLongitude) 
                          else LatLng(req.latitude, req.longitude)

                if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                    mMap.addMarker(
                        MarkerOptions()
                            .position(loc)
                            .title("EMERGENCY: ${req.bloodGroupRequired}")
                            .snippet("At: ${req.hospitalName} | Dist: ${calculateDistance(loc)}")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .zIndex(1.0f)
                    )
                    boundsBuilder.include(loc)
                    markerCount++
                }
            }
        }

        // 2. Process Donors (Red Markers)
        if (currentFilter == "All" || currentFilter == "Donors") {
            for (donor in allDonors) {
                if (donor.latitude == 0.0 || donor.longitude == 0.0) continue
                
                val bloodMatch = targetBloodGroup == null || 
                                donor.bloodGroup.trim().equals(targetBloodGroup?.trim(), ignoreCase = true)
                
                if (!bloodMatch) continue

                // Jitter to prevent overlapping markers at the same address
                val jitter = 0.0004
                val pos = LatLng(donor.latitude + Random.nextDouble(-jitter, jitter), 
                                donor.longitude + Random.nextDouble(-jitter, jitter))
                
                mMap.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title("DONOR: ${donor.name} (${donor.bloodGroup})")
                        .snippet("Eligible: ${if(donor.isEligible) "Yes" else "No"} | Dist: ${calculateDistance(pos)}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .zIndex(2.0f)
                )
                boundsBuilder.include(pos)
                markerCount++
            }
        }

        // Auto-zoom to show all relevant markers when data loads or filter changes
        if (shouldZoom && markerCount > 0) {
            try {
                if (markerCount == 1) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(boundsBuilder.build().center, 14f))
                } else {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150))
                }
            } catch (e: Exception) {
                Log.e("MapActivity", "Camera bounds error: ${e.message}")
            }
        }
    }

    private fun calculateDistance(target: LatLng): String {
        val userLoc = currentUserLocation ?: return "? km"
        val results = FloatArray(1)
        Location.distanceBetween(userLoc.latitude, userLoc.longitude, target.latitude, target.longitude, results)
        val km = results[0] / 1000
        return if (km < 1) "${results[0].roundToInt()}m" else String.format(Locale.US, "%.1fkm", km)
    }
}
