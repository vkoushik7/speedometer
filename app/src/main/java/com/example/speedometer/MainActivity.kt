package com.example.speedometer

import android.content.Intent
import android.content.IntentSender
import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var speedTextView: TextView
    private lateinit var locPermView: TextView
    private lateinit var locPermButton: Button
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val REQUEST_ENABLE_GPS = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedTextView = findViewById(R.id.speedTextView)
        locPermView = findViewById(R.id.locPermView)
        locPermButton = findViewById(R.id.locPermButton)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        locPermButton.setOnClickListener {
            requestLocation()
        }

        if (isLocationPermissionGranted()) {
            locPermView.visibility = View.GONE
            locPermButton.visibility = View.GONE
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocation() {
        if (isLocationPermissionGranted()){
            println("permission granted")
            displaypermission(2)
            enableGPS()
        }
        else{
            println("Not granted")
            requestLocationPermission()
        }
    }
    private fun enableGPS() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener(OnSuccessListener<LocationSettingsResponse> {
            // GPS is already enabled
            showToast("GPS is already enabled.")
            locPermView.visibility = View.GONE
            locPermButton.visibility = View.GONE
        })

        task.addOnFailureListener(OnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show dialog to enable GPS
                    exception.startResolutionForResult(this@MainActivity, REQUEST_ENABLE_GPS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }
            } else {
                showToast("Failed to enable GPS.")
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_GPS) {
            if (resultCode == Activity.RESULT_OK) {
                // User enabled GPS
                showToast("GPS is now enabled.")
                locPermView.visibility = View.GONE
                locPermButton.visibility = View.GONE
            } else {
                showToast("GPS is required for using the app.")
            }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    displaypermission(1)
                    locPermButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startLocationUpdates() {
        locPermView.visibility = View.GONE
        locPermButton.visibility = View.GONE
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speed = location.speed
            val speedKmh = speed * 3.6
            displaySpeedOnScreen(speedKmh)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {
            displaypermission(2)
            locPermButton.visibility = View.VISIBLE
            locPermView.visibility = View.VISIBLE
        }
    }

    private fun displaySpeedOnScreen(speed: Double) {
        runOnUiThread {
            speedTextView.text = "Current speed: ${String.format("%.2f", speed)} km/h"
        }
    }
    private fun displaypermission(a: Int){
        var ans = ""
        if (a==1){
            ans = "Location permission must be given in order to use the app"
        }
        else if (a==2){
            ans = "GPS must be turned on in order to use the app"
        }
        runOnUiThread {
            locPermView.text = ans
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

