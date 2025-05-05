package com.example.safealert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0

        if (level > 0) {
            Log.d("BatteryReceiver", "Nivel baterie: $level%")
        }

        when {
            level == 10 && !isCharging && !wasMessageSent(context, "battery_10") -> {
                sendBatteryWarning(context)
                saveMessageSent(context, "battery_10")
            }
            level == 3 && !isCharging && !wasMessageSent(context, "battery_3") -> {
                sendLowBatteryAlert(context)
                saveMessageSent(context, "battery_3")
            }
        }
    }

    private fun sendBatteryWarning(context: Context) {
        val message = "⚠️ Atentie! Bateria mea este aproape descarcata (10%)!"
        sendSmsToAllContacts(context, message)
        suggestBatterySaver(context)
    }

    private fun sendLowBatteryAlert(context: Context) {
        val locationHelper = LocationHelper(context)
        locationHelper.getCurrentLocation { location ->
            if (location != null) {
                val message = "🚨 Bateria mea se va termina curand (3%)! 📍 Locatie: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                sendSmsToAllContacts(context, message)
            } else {
                Toast.makeText(context, "Nu s-a putut obtine locatia", Toast.LENGTH_SHORT).show()
            }
        }
        suggestBatterySaver(context)
    }

    private fun suggestBatterySaver(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isPowerSaveMode) {
            try {
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Toast.makeText(context, "Nivelul bateriei este scazut. Poti activa modul de economisire a energiei.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("BatteryReceiver", "Eroare la deschiderea setarilor de economisire a energiei: ${e.localizedMessage}")
            }
        }
    }

    private fun sendSmsToAllContacts(context: Context, message: String) {
        val contacts = loadContacts(context)

        if (contacts.isEmpty()) {
            Log.d("BatteryReceiver", "Nu exista contacte salvate.")
            return
        }

        contacts.forEach { contact ->
            try {
                SmsManager.getDefault().sendTextMessage(contact.phone, null, message, null, null)
                Log.d("BatteryReceiver", "Mesaj trimis catre ${contact.phone}")
            } catch (e: Exception) {
                Log.e("BatteryReceiver", "Eroare la trimiterea SMS-ului catre ${contact.phone}: ${e.localizedMessage}")
            }
        }
    }

    private fun loadContacts(context: Context): List<Contact> {
        return try {
            context.openFileInput("contacts.json").use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    val json = reader.readText()
                    val type = object : TypeToken<List<Contact>>() {}.type
                    Gson().fromJson(json, type) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("BatteryReceiver", "Eroare la incarcarea contactelor: ${e.localizedMessage}")
            emptyList()
        }
    }

    private fun wasMessageSent(context: Context, key: String): Boolean {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("BatteryPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(key, false)
    }

    private fun saveMessageSent(context: Context, key: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("BatteryPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(key, true)
            apply()
        }
    }
}
