    package com.example.safealert

    import android.content.BroadcastReceiver
    import android.content.Context
    import android.content.Intent
    import android.telephony.SmsManager
    import android.util.Log
    import com.example.safealert.Contact
    import com.google.android.gms.location.Geofence
    import com.google.android.gms.location.GeofencingEvent
    import com.google.gson.Gson
    import java.io.BufferedReader
    import java.io.InputStreamReader

    class GeofenceReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    val errorCode = geofencingEvent.errorCode
                    Log.e("GeofenceReceiver", "Error: $errorCode")
                    return
                }
            }

            val geofenceTransition = geofencingEvent?.geofenceTransition

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                val message = "Ai parasit zona monitorizata!"
                sendSmsToContacts(context, message)
            }
        }

        private fun sendSmsToContacts(context: Context, message: String) {
            val contactList: MutableList<Contact> = loadContacts(context)
            contactList.forEach { contact ->
                try {
                    val phoneNumber = contact.phone
                    SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
                    Log.d("GeofenceReceiver", "SMS trimis catre: $phoneNumber")
                } catch (e: Exception) {
                    Log.e("GeofenceReceiver", "Eroare la trimiterea SMS: ${e.localizedMessage}")
                }
            }
        }

        private fun loadContacts(context: Context): MutableList<Contact> {
            val contactList: MutableList<Contact> = mutableListOf()
            try {
                context.openFileInput("contacts.json").use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        val json = reader.readText()
                        val type = object : com.google.gson.reflect.TypeToken<List<Contact>>() {}.type
                        contactList.addAll(Gson().fromJson(json, type))
                    }
                }
            } catch (e: Exception) {
                Log.e("GeofenceReceiver", "Eroare la incarcarea contactelor: ${e.localizedMessage}")
            }
            return contactList
        }
    }


