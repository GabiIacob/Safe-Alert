package com.example.safealert

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.safealert.ui.theme.SafeAlertTheme

class AddContactActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafeAlertTheme {
                val contact = intent?.getParcelableExtra<Contact>("contact")
                AddContactScreen(contact = contact)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(contact: Contact?) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (contact == null) "Add New Contact" else "Edit Contact") },
                navigationIcon = {
                    IconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        content = { padding ->
            ContactForm(
                contact = contact,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            )
        }
    )
}

@Composable
fun ContactForm(
    contact: Contact?,
    modifier: Modifier = Modifier
) {
    var contactName by remember { mutableStateOf(contact?.name ?: "") }
    var contactPhone by remember { mutableStateOf(contact?.phone ?: "") }
    var contactEmail by remember { mutableStateOf(contact?.email ?: "") }

    val context = LocalContext.current
    val isEditMode = contact != null

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isEditMode) "Update contact details" else "Add emergency contact",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        InputField(
            value = contactName,
            onValueChange = { contactName = it },
            label = "Full Name",
            leadingIcon = Icons.Default.Person,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )
        InputField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = "Phone Number",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )
        InputField(
            value = contactEmail,
            onValueChange = { contactEmail = it },
            label = "Email (Optional)",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val updatedContact = Contact(
                    name = contactName,
                    phone = contactPhone,
                    email = contactEmail
                )

                if (isEditMode) {
                    val index = contactList.indexOfFirst { it.name == contact?.name && it.phone == contact?.phone }
                    if (index != -1) contactList[index] = updatedContact
                } else {
                    contactList.add(updatedContact)
                }

                (context as? Activity)?.finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = contactName.isNotBlank() && contactPhone.isNotBlank()
        ) {
            Text(
                text = if (isEditMode) "Update Contact" else "Save Contact",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        )
    )
}

@Preview(showBackground = true)
@Composable
fun AddContactPreview() {
    SafeAlertTheme {
        AddContactScreen(
            contact = Contact(
                name = "John Doe",
                phone = "1234567890",
                email = "john@example.com"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddNewContactPreview() {
    SafeAlertTheme {
        AddContactScreen(contact = null)
    }
}