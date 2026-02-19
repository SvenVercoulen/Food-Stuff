package com.example.smartreminderlogger

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class CreateUserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)

        val etName = findViewById<EditText>(R.id.etName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val btnSave = findViewById<Button>(R.id.btnSaveUser)

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val ageString = etAge.text.toString()

            if (name.isNotEmpty() && ageString.isNotEmpty()) {
                val age = ageString.toIntOrNull() ?: 0

                if (age > 150) {
                    Toast.makeText(this, "Age number is too high", Toast.LENGTH_SHORT).show()

                } else if (age < 1) {
                    Toast.makeText(this, "Age cannot be below 1", Toast.LENGTH_SHORT).show()

                } else {
                    saveProfile(name, age.toString())
                    finish()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProfile(name: String, age: String) {
        val file = File(filesDir, "Profiles.csv")

        // Determine the next ID based on number of lines
        val currentId = if (file.exists()) file.readLines().size else 1

        // Prepare header if new file
        if (!file.exists()) {
            FileOutputStream(file, true).use { it.write("id,name,age\n".toByteArray()) }
        }

        // Save the new profile
        val entry = "$currentId,$name,$age\n"
        FileOutputStream(file, true).use { it.write(entry.toByteArray()) }

        Toast.makeText(this, "Profile created for $name", Toast.LENGTH_SHORT).show()
    }
}