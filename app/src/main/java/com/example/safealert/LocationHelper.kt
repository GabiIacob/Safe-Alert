    package com.example.safealert

    import android.annotation.SuppressLint
    import android.content.Context
    import android.location.Location
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import com.google.android.gms.location.*

    class LocationHelper(context: Context) {
        private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)
        private var locationCallback: LocationCallback? = null

        @SuppressLint("MissingPermission")
        fun getCurrentLocation(callback: (Location?) -> Unit) {
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                callback(null)
                removeLocationUpdates()
            }

            handler.postDelayed(timeoutRunnable, 30000)

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                handler.removeCallbacks(timeoutRunnable)
                location?.let { callback(it) } ?: requestNewLocation(handler, callback)
            }.addOnFailureListener { e ->
                handler.removeCallbacks(timeoutRunnable)
                Log.e("LocationHelper", "Eroare: ${e.message}")
                callback(null)
            }
        }

        private fun requestNewLocation(handler: Handler, callback: (Location?) -> Unit) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    handler.removeCallbacksAndMessages(null)
                    callback(result.lastLocation)
                    removeLocationUpdates()
                }
            }

            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 5000
                fastestInterval = 2000
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        }

        private fun removeLocationUpdates() {
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
                locationCallback = null
            }
        }
    }