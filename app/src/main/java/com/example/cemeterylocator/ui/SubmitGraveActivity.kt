package com.example.cemeterylocator.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cemeterylocator.R
import com.example.cemeterylocator.api.ApiClient
import com.example.cemeterylocator.databinding.ActivitySubmitGraveBinding
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class SubmitGraveActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubmitGraveBinding
    private var marker: Marker? = null
    private var pickedLat: Double? = null
    private var pickedLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitGraveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_submit_grave)

        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(18.0)
        // Starting view - same idea as MapActivity's default center. Adjust
        // to the real cemetery's coordinates if you know them ahead of time.
        binding.mapView.controller.setCenter(GeoPoint(0.0, 0.0))

        binding.mapView.setOnTouchListener { _, _ -> false } // let the map handle its own gestures

        binding.mapView.overlays.add(
            object : org.osmdroid.views.overlay.Overlay() {
                override fun onSingleTapConfirmed(
                    e: android.view.MotionEvent,
                    mapView: org.osmdroid.views.MapView
                ): Boolean {
                    val point = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                    placeMarker(point.latitude, point.longitude)
                    return true
                }
            }
        )

        binding.buttonSubmit.setOnClickListener { doSubmit() }
    }

    private fun placeMarker(lat: Double, lng: Double) {
        pickedLat = lat
        pickedLng = lng
        val point = GeoPoint(lat, lng)
        if (marker == null) {
            marker = Marker(binding.mapView).apply {
                position = point
                isDraggable = true
                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(m: Marker) {}
                    override fun onMarkerDragEnd(m: Marker) {
                        pickedLat = m.position.latitude
                        pickedLng = m.position.longitude
                        updateCoordsLabel()
                    }
                    override fun onMarkerDragStart(m: Marker) {}
                })
            }
            binding.mapView.overlays.add(marker)
        } else {
            marker?.position = point
        }
        binding.mapView.invalidate()
        updateCoordsLabel()
    }

    private fun updateCoordsLabel() {
        binding.textCoords.text = if (pickedLat != null && pickedLng != null) {
            getString(R.string.format_coords, pickedLat, pickedLng)
        } else {
            getString(R.string.hint_tap_map)
        }
    }

    private fun doSubmit() {
        val name = binding.editName.text.toString().trim()
        val location = binding.editLocation.text.toString().trim().ifBlank { null }
        val notes = binding.editNotes.text.toString().trim().ifBlank { null }
        val lat = pickedLat
        val lng = pickedLng

        if (name.isBlank()) {
            Toast.makeText(this, "Enter the deceased's name", Toast.LENGTH_SHORT).show()
            return
        }
        if (lat == null || lng == null) {
            Toast.makeText(this, "Tap the map to place a pin at the grave", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSubmit.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = ApiClient.create(this@SubmitGraveActivity)
                val response = api.submitGrave(
                    name = name,
                    lat = lat,
                    lng = lng,
                    location = location,
                    notes = notes
                )
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@SubmitGraveActivity,
                        response.body()!!.message,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else if (response.code() == 401) {
                    Toast.makeText(
                        this@SubmitGraveActivity,
                        "Please sign in again",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@SubmitGraveActivity,
                        "Submission failed (${response.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SubmitGraveActivity,
                    "Couldn't reach server: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.buttonSubmit.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
