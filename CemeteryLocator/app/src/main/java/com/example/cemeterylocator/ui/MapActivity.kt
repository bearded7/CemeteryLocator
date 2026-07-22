package com.example.cemeterylocator.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cemeterylocator.R
import com.example.cemeterylocator.api.ApiClient
import com.example.cemeterylocator.databinding.ActivityMapBinding
import com.example.cemeterylocator.model.GeoGrave
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private var locationOverlay: MyLocationNewOverlay? = null
    private var hasCenteredOnUser = false

    private var focusPoint: GeoPoint? = null
    private var guidanceLine: Polyline? = null
    private val guidanceHandler = Handler(Looper.getMainLooper())
    private val guidanceRunnable = object : Runnable {
        override fun run() {
            updateGuidance()
            guidanceHandler.postDelayed(this, 2000)
        }
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableMyLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied - showing graves without GPS tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be set before MapView is inflated.
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_map)

        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(18.0)

        binding.fabMyLocation.setOnClickListener { centerOnUserLocation() }

        ensureLocationPermission()
        loadGraveMarkers()
    }

    private fun ensureLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            enableMyLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableMyLocation() {
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView)
        overlay.enableMyLocation()
        overlay.runOnFirstFix {
            runOnUiThread {
                if (!hasCenteredOnUser) {
                    hasCenteredOnUser = true
                    overlay.myLocation?.let { binding.mapView.controller.animateTo(it) }
                }
            }
        }
        binding.mapView.overlays.add(overlay)
        locationOverlay = overlay
    }

    private fun updateGuidance() {
        val focus = focusPoint ?: return
        val userLoc = locationOverlay?.myLocation ?: return

        if (guidanceLine == null) {
            guidanceLine = Polyline(binding.mapView).apply {
                outlinePaint.color = android.graphics.Color.parseColor("#2E7D32")
                outlinePaint.strokeWidth = 6f
            }
            binding.mapView.overlays.add(guidanceLine)
        }
        guidanceLine?.setPoints(listOf(userLoc, focus))

        val distanceMeters = userLoc.distanceToAsDouble(focus)
        val bearing = userLoc.bearingTo(focus).toFloat()
        val compassDirection = bearingToCompass(bearing)

        val distanceText = if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000)
        } else {
            String.format("%.0f m", distanceMeters)
        }

        binding.textDistance.visibility = android.view.View.VISIBLE
        binding.textDistance.text = "$distanceText • head $compassDirection"

        binding.mapView.invalidate()
    }

    private fun bearingToCompass(bearing: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((bearing + 22.5) % 360) / 45).toInt()
        return directions[if (index in directions.indices) index else 0]
    }

    private fun centerOnUserLocation() {
        val loc = locationOverlay?.myLocation
        if (loc != null) {
            binding.mapView.controller.animateTo(loc)
            binding.mapView.controller.setZoom(19.0)
        } else {
            Toast.makeText(this, "Still finding your location…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGraveMarkers() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.create(this@MapActivity)
                val response = api.getGeoGraves(null)
                if (response.isSuccessful) {
                    val graves = response.body()?.graves.orEmpty()
                    if (graves.isEmpty()) {
                        Toast.makeText(
                            this@MapActivity,
                            "No graves have GPS coordinates yet",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    addMarkers(graves)

                    val focusLat = intent.getDoubleExtra(EXTRA_FOCUS_LAT, Double.NaN)
                    val focusLng = intent.getDoubleExtra(EXTRA_FOCUS_LNG, Double.NaN)
                    when {
                        !focusLat.isNaN() && !focusLng.isNaN() -> {
                            // Launched from a specific person's detail screen -
                            // center directly on their grave and start live
                            // distance/bearing guidance from wherever the user is.
                            focusPoint = GeoPoint(focusLat, focusLng)
                            binding.mapView.controller.setCenter(focusPoint)
                            binding.mapView.controller.setZoom(19.5)
                            guidanceHandler.post(guidanceRunnable)
                        }
                        !hasCenteredOnUser -> {
                            // Otherwise start centered on the average of all
                            // grave coordinates, in case GPS fix is slow.
                            val avgLat = graves.map { it.lat }.average()
                            val avgLng = graves.map { it.lng }.average()
                            binding.mapView.controller.setCenter(GeoPoint(avgLat, avgLng))
                        }
                    }
                } else {
                    Toast.makeText(
                        this@MapActivity,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MapActivity,
                    "Couldn't reach server: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addMarkers(graves: List<GeoGrave>) {
        for (grave in graves) {
            val marker = Marker(binding.mapView)
            marker.position = GeoPoint(grave.lat, grave.lng)
            marker.title = grave.name
            marker.snippet = "Grave #${grave.graveNo}" +
                (grave.location?.let { " • $it" } ?: "")
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.relatedObject = grave
            marker.setOnMarkerClickListener { m, mapView ->
                m.showInfoWindow()
                mapView.controller.animateTo(m.position)
                true
            }
            binding.mapView.overlays.add(marker)
        }
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        guidanceHandler.removeCallbacks(guidanceRunnable)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_FOCUS_LAT = "extra_focus_lat"
        const val EXTRA_FOCUS_LNG = "extra_focus_lng"
    }
}
