package com.example.safealert
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.safealert.ui.theme.SafeAlertTheme
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
val contactList: MutableList<Contact> = mutableListOf()
private lateinit var batteryReceiver: BatteryReceiver
lateinit var geofenceReceiver: GeofenceReceiver
private lateinit var geofencingClient: GeofencingClient
private lateinit var geofenceHelper: GeofenceHelper

class MainActivity : ComponentActivity() {
    private val locationHelper: LocationHelper by lazy { LocationHelper(this) }
    private var isAlertActive by mutableStateOf(false)
    private val handler = Handler(Looper.getMainLooper())
    private val callRunnable: Runnable by lazy {
        Runnable {
            makePhoneCall()
        } }
    private val alertRunnable: Runnable by lazy {
        Runnable {
            synchronized(this@MainActivity) {
                if (isAlertActive) {
                    try {
                        sendLocationAlert()
                        handler.postDelayed(callRunnable, 10000)
                        Toast.makeText(
                            this@MainActivity,
                            "Emergency alert sent. Calling in 10 seconds.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "Error sending alert",
                            Toast.LENGTH_SHORT
                        ).show()
                    } } } } }
    private var startTime: Long = 0
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            sendLocationAlert()
        } else {
            Toast.makeText(this, "All permissions are required!", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadContacts()
        setContent {
            SafeAlertTheme {
                Scaffold(
                    topBar = { AppToolbar() },
                    content = { padding ->
                        MainContent(
                            modifier = Modifier
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                        ) }) } }
        batteryReceiver = BatteryReceiver()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        geofenceReceiver = GeofenceReceiver()
        geofencingClient = LocationServices.getGeofencingClient(this)
        geofenceHelper = GeofenceHelper(this)
        geofenceHelper.createGeofences()
    }
    private fun loadContacts() {
        try {
            openFileInput("contacts.json").use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    val json = reader.readText()
                    val type = object : TypeToken<List<Contact>>() {}.type
                    contactList.clear()
                    contactList.addAll(Gson().fromJson(json, type))
                } } } catch (e: Exception) {
            Log.e("Contacts", "Load error: ${e.localizedMessage}")
        }
    }
    private fun saveContacts() {
        try {
            openFileOutput("contacts.json", Context.MODE_PRIVATE).use { stream ->
                OutputStreamWriter(stream).use { writer ->
                    writer.write(Gson().toJson(contactList))
                } } } catch (e: Exception) {
            Toast.makeText(this, "Error saving contacts", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onStop() {
        super.onStop()
        saveContacts()
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppToolbar() {
        TopAppBar(
            title = {
                Text(
                    text = "Safe Alert",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
    @Composable
    fun MainContent(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmergencyAlertSection()
            ContactsSection()
            ActionButtons()
        } }
    @Composable
    fun EmergencyAlertSection() {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Emergency",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Emergency Alert",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Press to send your location to emergency contacts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                AlertButton()
            }
        }
    }
    @Composable
    fun AlertButton() {
        val containerColor = if (isAlertActive) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.tertiaryContainer
        val contentColor = if (isAlertActive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onTertiaryContainer
        FilledTonalButton(
            onClick = {
                if (isAlertActive) {
                    isAlertActive = false
                    handler.removeCallbacks(alertRunnable)
                    Toast.makeText(
                        this@MainActivity,
                        "Emergency alert canceled",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    isAlertActive = true
                    startTime = System.currentTimeMillis()
                    handler.post(alertRunnable)
                    Toast.makeText(
                        this@MainActivity,
                        "Emergency alert activated",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Alert",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAlertActive) "EMERGENCY ACTIVE" else "TRIGGER EMERGENCY",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
    @Composable
    fun ContactsSection() {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Emergency Contacts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (contactList.isEmpty()) {
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contactList) { contact ->
                        ContactItem(contact)
                    } } } } }
    @Composable
    fun ContactItem(contact: Contact) {
        val context = LocalContext.current
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Contact",
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(context, AddContactActivity::class.java).apply {
                                putExtra("contact", contact)
                            }
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = {
                        contactList.remove(contact)
                        saveContacts()
                        Toast.makeText(context, "Contact removed", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    ) } } } }
    @Composable
    fun ActionButtons() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AddContactButton()
            ScheduleMessageButton()
        }
    }
    @Composable
    fun AddContactButton() {
        FilledTonalButton(
            onClick = { startActivity(Intent(this@MainActivity, AddContactActivity::class.java)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add New Contact", style = MaterialTheme.typography.labelLarge)
        }
    }
    @Composable
    fun ScheduleMessageButton() {
        OutlinedButton(
            onClick = { startActivity(Intent(this@MainActivity, ScheduledMessagesActivity::class.java)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Schedule",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Schedule Messages",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    private fun checkPermissionsAndSendAlert() {
        val required = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (required.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            if (isProcessingAlert) return
            isProcessingAlert = true
            sendLocationAlert()
        } else {
            requestPermissions.launch(required)
        }
    }

    private fun sendLocationAlert() {
        handler.removeCallbacks(callRunnable)
        locationHelper.getCurrentLocation { location ->
            isProcessingAlert = false
            if (location != null) {
                val msg = "Urgenta! Locatie: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                sendSmsToContacts(msg)
            } else {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
            }
            handler.postDelayed(callRunnable, 10000)
            Toast.makeText(
                this@MainActivity,
                "Emergency alert sent. Calling in 10 seconds.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun isSimAvailable(): Boolean {
        return when {
            true -> {
                val subscriptionManager = getSystemService(SubscriptionManager::class.java)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
                subscriptionManager?.activeSubscriptionInfoList?.isNotEmpty() ?: false
            }
            else -> {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.simState == TelephonyManager.SIM_STATE_READY
            }
        }
    }
    private fun sendSmsToContacts(message: String) {
        if (!isSimAvailable()) {
            Toast.makeText(this, "SIM card nedisponibil", Toast.LENGTH_SHORT).show()
            return
        }
        contactList.forEach { contact ->
            try {
                val phoneNumber = contact.phone.ensurePhoneFormat()
                SmsManager.getDefault().sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
                Log.d("SMS", "Message sent to $phoneNumber")
            } catch (e: Exception) {
                Log.e("SMS", "Error: ${e.localizedMessage}")
                Toast.makeText(
                    this,
                    "Error for ${contact.name}: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun String.ensurePhoneFormat(): String {
        return when {
            startsWith("+") -> this
            length == 10 && startsWith("07") -> "+4$this"
            length == 9 -> "+40$this"
            else -> {
                Toast.makeText(
                    this@MainActivity,
                    "Invalid number: $this",
                    Toast.LENGTH_SHORT
                ).show()
                this
            }
        }
    }
    private fun makePhoneCall() {
        if (contactList.isNotEmpty()) {
            val phoneNumber = contactList[0].phone
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Call permission needed!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter("com.example.safealert.ACTION_GEOFENCE_EVENT")
        ContextCompat.registerReceiver(
            this,
            geofenceReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }
    private var lastVolumeKeyPressTime: Long = 0
    private val VOLUME_PRESS_COOLDOWN = 1000L // 1 second cooldown
    private var isProcessingAlert = false
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastVolumeKeyPressTime > VOLUME_PRESS_COOLDOWN && !isProcessingAlert) {
                lastVolumeKeyPressTime = currentTime
                checkPermissionsAndSendAlert()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}