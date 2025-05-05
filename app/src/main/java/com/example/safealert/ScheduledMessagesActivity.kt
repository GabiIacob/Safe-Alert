package com.example.safealert

import android.app.TimePickerDialog
import androidx.work.*
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.safealert.ui.theme.SafeAlertTheme
import java.util.*
import java.util.concurrent.TimeUnit

class ScheduledMessagesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeAlertTheme {
                ScheduledMessagesScreen()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessagesScreen() {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var scheduledTime by remember { mutableStateOf("") }
    val scheduledMessages = remember { mutableStateListOf<ScheduledMessage>() }
    val workRequests = remember { mutableStateListOf<WorkRequest>() }
    LaunchedEffect(Unit) {
        loadExistingScheduledMessages(context, scheduledMessages)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Messages") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "New Scheduled Message",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        leadingIcon = { Icon(Icons.Default.Message, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        scheduledTime = String.format("%02d:%02d", hour, minute)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (scheduledTime.isEmpty()) "Select Time" else scheduledTime)
                        }
                        FilledTonalButton(
                            onClick = {
                                if (phoneNumber.isNotEmpty() && message.isNotEmpty() && scheduledTime.isNotEmpty()) {
                                    val parts = scheduledTime.split(":")
                                    val hour = parts[0].toInt()
                                    val minute = parts[1].toInt()

                                    val workRequest = scheduleMessage(
                                        context,
                                        hour,
                                        minute,
                                        phoneNumber,
                                        message,
                                        workRequests
                                    )
                                    scheduledMessages.add(
                                        ScheduledMessage(
                                            id = workRequest.id.toString(),
                                            phone = phoneNumber,
                                            message = message,
                                            time = scheduledTime
                                        )
                                    )
                                    phoneNumber = ""
                                    message = ""
                                    scheduledTime = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = phoneNumber.isNotEmpty() &&
                                    message.isNotEmpty() &&
                                    scheduledTime.isNotEmpty()
                        ) {
                            Text("Schedule")
                        }
                    }
                }
            }

            Text(
                text = "Upcoming Messages",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (scheduledMessages.isEmpty()) {
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
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No scheduled messages",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scheduledMessages) { item ->
                        ScheduledMessageItem(
                            message = item,
                            onDelete = {
                                WorkManager.getInstance(context).cancelWorkById(UUID.fromString(item.id))
                                scheduledMessages.remove(item)
                            }
                        )
                    }
                }
            }
        }
    }
}
private fun loadExistingScheduledMessages(
    context: Context,
    scheduledMessages: MutableList<ScheduledMessage>
) {
    val workManager = WorkManager.getInstance(context)

    workManager.getWorkInfosByTagLiveData("scheduled_message").observeForever { workInfos ->
        workInfos?.forEach { workInfo ->
            if (workInfo.state == WorkInfo.State.ENQUEUED) {
                val phone = workInfo.outputData.getString("phone") ?: ""
                val msg = workInfo.outputData.getString("message") ?: ""
                val time = workInfo.outputData.getString("scheduled_time") ?: ""

                scheduledMessages.add(ScheduledMessage(
                    id = workInfo.id.toString(),
                    phone = phone,
                    message = msg,
                    time = time
                ))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledMessageItem(
    message: ScheduledMessage,
    onDelete: () -> Unit
) {
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = message.time,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = message.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}



fun scheduleMessage(
    context: Context,
    hour: Int,
    minute: Int,
    phone: String,
    message: String,
    workRequests: MutableList<WorkRequest>
): WorkRequest {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    val delayMillis = calendar.timeInMillis - System.currentTimeMillis()
    if (delayMillis < 0) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    val data = Data.Builder()
        .putString("phone", phone)
        .putString("message", message)
        .build()

    val workRequest = OneTimeWorkRequestBuilder<SendScheduledMessageWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
    workRequests.add(workRequest)

    return workRequest
}

@Preview(showBackground = true)
@Composable
fun ScheduledMessagesPreview() {
    SafeAlertTheme {
        ScheduledMessagesScreen()
    }
}