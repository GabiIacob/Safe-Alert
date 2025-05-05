package com.example.safealert
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.PendingIntent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofences: MutableList<Geofence> = mutableListOf()

    fun createGeofences() {
        val geofence1 = Geofence.Builder()
            .setRequestId("geofence_1")
            .setCircularRegion(46.5468909, 24.569034, 500f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofence2 = Geofence.Builder()
            .setRequestId("geofence_2")
            .setCircularRegion(46.5550472, 24.5733633, 500f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()



        geofences.add(geofence1)
        geofences.add(geofence2)

        addGeofences()
    }

    private fun addGeofences() {
        val geofencingRequest = GeofencingRequest.Builder()
            .addGeofences(geofences)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()

        val geofencePendingIntent: PendingIntent = createGeofencePendingIntent(context)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("Geofence", "Geofences adaugate cu succes")
                }
                .addOnFailureListener {
                    Log.e("Geofence", "Eroare la adaugarea geofence-urilor: ${it.localizedMessage}")
                }
        }
    }

    fun createGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = "com.example.safealert.ACTION_GEOFENCE_EVENT"
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

}

