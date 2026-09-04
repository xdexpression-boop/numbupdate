package com.example.gambianumberupdater

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ContactMatch(
    val dataId: Long,
    val name: String,
    val oldNumber: String,
    val newNumber: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var prefixInput: EditText
    private lateinit var statusText: TextView
    private lateinit var listView: ListView
    private lateinit var scanButton: Button
    private lateinit var applyButton: Button

    private var matches = mutableListOf<ContactMatch>()

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_CONTACTS] == true &&
                result[Manifest.permission.WRITE_CONTACTS] == true
        if (!granted) {
            statusText.text = "Contacts permission is required for this app to work."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefixInput = findViewById(R.id.prefixInput)
        statusText = findViewById(R.id.statusText)
        listView = findViewById(R.id.contactsList)
        scanButton = findViewById(R.id.scanButton)
        applyButton = findViewById(R.id.applyButton)

        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        ensurePermissions()

        scanButton.setOnClickListener { scanContacts() }
        applyButton.setOnClickListener { applyChanges() }
    }

    private fun ensurePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.READ_CONTACTS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) needed.add(Manifest.permission.WRITE_CONTACTS)
        if (needed.isNotEmpty()) {
            requestPermissions.launch(needed.toTypedArray())
        }
    }

    /**
     * Splits a raw phone number into (countryPrefix, localDigits).
     * countryPrefix is "+220", "220", or "" (no country code present).
     * localDigits is the digit-only remainder.
     */
    private fun splitNumber(raw: String): Pair<String, String> {
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }

        return when {
            hasPlus && digitsOnly.startsWith("220") ->
                Pair("+220", digitsOnly.removePrefix("220"))
            !hasPlus && digitsOnly.startsWith("220") && digitsOnly.length > 7 ->
                Pair("220", digitsOnly.removePrefix("220"))
            else ->
                Pair("", digitsOnly)
        }
    }

    private fun scanContacts() {
        val prefix = prefixInput.text.toString().trim()
        if (prefix.length != 2 || !prefix.all { it.isDigit() }) {
            statusText.text = "Enter exactly 2 digits for the new prefix (e.g. 20) first."
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ensurePermissions()
            return
        }

        matches = mutableListOf()
        val cursor = contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone._ID, Phone.DISPLAY_NAME, Phone.NUMBER),
            null, null, null
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(Phone._ID)
            val nameIdx = it.getColumnIndex(Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(Phone.NUMBER)

            while (it.moveToNext()) {
                val dataId = it.getLong(idIdx)
                val name = it.getString(nameIdx) ?: "(no name)"
                val rawNumber = it.getString(numberIdx) ?: continue

                val (countryPrefix, localDigits) = splitNumber(rawNumber)

                // Only touch old-format 7-digit local Gambia numbers.
                // Skip anything already 9 digits, international, or malformed.
                if (localDigits.length == 7) {
                    val newLocal = prefix + localDigits
                    val newNumber = countryPrefix + newLocal
                    matches.add(ContactMatch(dataId, name, rawNumber, newNumber))
                }
            }
        }

        val displayLines = matches.map { "${it.name}\n${it.oldNumber}  ->  ${it.newNumber}" }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            displayLines
        )
        listView.adapter = adapter

        // Pre-select everything found; user can uncheck individual ones.
        for (i in matches.indices) {
            listView.setItemChecked(i, true)
        }

        statusText.text = if (matches.isEmpty()) {
            "No old-format 7-digit Gambia numbers found."
        } else {
            "${matches.size} number(s) found. Uncheck any you don't want to change, then tap Update."
        }
    }

    private fun writeBackup(items: List<ContactMatch>) {
        try {
            val dir = getExternalFilesDir(null) ?: return
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "gambia_number_backup_$timestamp.csv")
            file.bufferedWriter().use { writer ->
                writer.write("name,old_number,new_number\n")
                items.forEach { m ->
                    writer.write("\"${m.name.replace("\"", "'")}\",${m.oldNumber},${m.newNumber}\n")
                }
            }
            statusText.append("\nBackup saved: ${file.absolutePath}")
        } catch (e: Exception) {
            statusText.append("\n(Backup could not be saved: ${e.message})")
        }
    }

    private fun applyChanges() {
        if (matches.isEmpty()) {
            statusText.text = "Scan contacts first."
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ensurePermissions()
            return
        }

        val checkedPositions = listView.checkedItemPositions
        val selected = mutableListOf<ContactMatch>()
        for (i in matches.indices) {
            if (checkedPositions.get(i)) selected.add(matches[i])
        }

        if (selected.isEmpty()) {
            statusText.text = "Nothing selected."
            return
        }

        // Always back up before touching anything.
        writeBackup(selected)

        var successCount = 0
        for (m in selected) {
            try {
                val values = ContentValues().apply {
                    put(Phone.NUMBER, m.newNumber)
                }
                val rows = contentResolver.update(
                    Phone.CONTENT_URI,
                    values,
                    "${Phone._ID}=?",
                    arrayOf(m.dataId.toString())
                )
                if (rows > 0) successCount++
            } catch (e: Exception) {
                // Skip contacts that can't be modified (e.g. read-only synced accounts)
            }
        }

        statusText.append("\nUpdated $successCount of ${selected.size} contact(s).")
        Toast.makeText(this, "Updated $successCount contact(s)", Toast.LENGTH_LONG).show()

        // Re-scan so the list reflects the current state.
        scanContacts()
    }
}
