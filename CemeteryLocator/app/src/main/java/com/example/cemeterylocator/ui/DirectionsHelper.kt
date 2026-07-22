package com.example.cemeterylocator.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Turn-by-turn directions are delegated to whatever maps app is already
 * installed on the device (Google Maps, etc.) via a standard geo: intent,
 * rather than reimplementing road routing in-app. This is the same
 * approach most small apps use: zero maintenance, always up to date,
 * and works everywhere the user already has a maps app they trust.
 *
 * The in-app OSM map (MapActivity) is still useful on top of this for
 * "where exactly is the pin inside the cemetery grounds" - external map
 * apps generally don't know about individual grave plots.
 */
object DirectionsHelper {

    fun openExternalDirections(context: Context, lat: Double, lng: Double, label: String) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fall back to a browser-based Google Maps directions link if no
            // geo: handler is installed.
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, "No maps app available", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
