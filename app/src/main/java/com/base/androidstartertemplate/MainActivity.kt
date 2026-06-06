package com.base.androidstartertemplate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.base.androidstartertemplate.presentation.modules.home.screens.navScreens.MainScreen
import com.base.androidstartertemplate.themes.theme.AppTheme
import com.base.androidstartertemplate.utility.BaseActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val nickname = remember { mutableStateOf("") }
                val accuracy = remember { mutableStateOf("10") } // meters
                val interval = remember { mutableStateOf("5") } // seconds
                
                // Initialize Firebase
                FirebaseApp.initializeApp(context)
                auth = FirebaseAuth.getInstance()
                db = FirebaseFirestore.getInstance()
                
                // Request permissions
                LaunchedEffect(Unit) {
                    requestPermissions()
                }
                
                // UI Components
                Surface(modifier = Modifier.fillMaxSize(), color = AppTheme.colors.secondary) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "Location Tracker Setup")
                        
                        OutlinedTextField(
                            value = nickname.value,
                            onValueChange = { nickname.value = it },
                            label = { Text("Enter Nickname") },
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = accuracy.value,
                            onValueChange = { accuracy.value = it },
                            label = { Text("Accuracy (meters)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = interval.value,
                            onValueChange = { interval.value = it },
                            label = { Text("Update Interval (seconds)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        Button(onClick = {
                            startLocationTracking(nickname.value.toIntOrNull() ?: 0, 
                                accuracy.value.toIntOrNull() ?: 10, 
                                interval.value.toIntOrNull() ?: 5)
                        }) {
                            Text("Start Tracking")
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocationTracking(userId: Int, accuracy: Int, interval: Int) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        // Start location updates with desired parameters
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            interval.toLong(),
            accuracy.toFloat(),
            object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    // Save to Firebase
                    val locationData = mapOf(
                        "userId" to userId,
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "timestamp" to System.currentTimeMillis()
                    )
                    
                    db.collection("locations").add(locationData)
                        .addOnSuccessListener { _ ->
                            Toast.makeText(this@MainActivity, 
                                "Location saved", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@MainActivity, 
                                "Error saving location: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        )
    }
}
