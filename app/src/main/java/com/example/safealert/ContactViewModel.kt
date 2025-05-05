package com.example.safealert

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ContactViewModel(private val context: Context) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> get() = _contacts

    private val fileName = "contacts.json"

    init {
        loadContacts()
    }


    private fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.openFileInput(fileName).use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        val json = reader.readText()
                        val type = object : TypeToken<List<Contact>>() {}.type
                        val loadedContacts: List<Contact> = Gson().fromJson(json, type)
                        _contacts.value = loadedContacts
                    }
                }
                Log.d("ContactViewModel", "Contactele au fost incarcate cu succes.")
            } catch (e: Exception) {
                Log.e("ContactViewModel", "Eroare la incarcarea contactelor: ${e.localizedMessage}")
            }
        }
    }


     //JSON file in the internal storage.

    private fun saveContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write(Gson().toJson(_contacts.value))
                    }
                }
                Log.d("ContactViewModel", "Contactele au fost salvate cu succes.")
            } catch (e: Exception) {
                Log.e("ContactViewModel", "Eroare la salvarea contactelor: ${e.localizedMessage}")
            }
        }
    }


    fun addOrUpdateContact(contact: Contact) {
        val currentContacts = _contacts.value.toMutableList()
        val existingIndex = currentContacts.indexOfFirst { it.phone == contact.phone }

        if (existingIndex != -1) {
            currentContacts[existingIndex] = contact
        } else {
            currentContacts.add(contact)
        }

        _contacts.value = currentContacts
        saveContacts()
    }


    fun removeContact(contact: Contact) {
        val currentContacts = _contacts.value.toMutableList()
        if (currentContacts.remove(contact)) {
            _contacts.value = currentContacts
            saveContacts()
        } else {
            Toast.makeText(context, "Contactul nu a fost găsit", Toast.LENGTH_SHORT).show()
        }
    }
}
