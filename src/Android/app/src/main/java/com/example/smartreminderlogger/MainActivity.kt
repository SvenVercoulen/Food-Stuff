package com.example.smartreminderlogger

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ElderlyUser(val id: Int, val name: String) {
    override fun toString(): String = name
}

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private var userList = mutableListOf<ElderlyUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.userListView)

        findViewById<Button>(R.id.btnAddUser).setOnClickListener {
            startActivity(Intent(this, CreateUserActivity::class.java))
        }

        findViewById<Button>(R.id.btnStats).setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        findViewById<Button>(R.id.btnPredict).setOnClickListener {
            startActivity(Intent(this, ForecastActivity::class.java))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            showActionDialog(userList[position])
        }

        loadUsersFromCsv()
    }

    override fun onResume() {
        super.onResume()
        loadUsersFromCsv() // Refresh list when returning to this page
    }

    private fun loadUsersFromCsv() {
        userList.clear()
        val file = File(filesDir, "Profiles.csv")

        if (file.exists()) {
            val lines = file.readLines().drop(1) // Skip header
            for (line in lines) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    userList.add(ElderlyUser(parts[0].toInt(), parts[1]))
                }
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, userList)
        listView.adapter = adapter
    }

    private fun showActionDialog(user: ElderlyUser) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_actions, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = "Log for ${user.name}"

        dialogView.findViewById<Button>(R.id.btnDialogEat).setOnClickListener {
            saveAction(user.id, "eat")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnDialogDrink).setOnClickListener {
            saveAction(user.id, "drink")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnDialogOutside).setOnClickListener {
            saveAction(user.id, "outside_out") // Simplified for demo
            dialog.dismiss()
        }

        dialog.show()
    }

    // SAVING LOGIC
    private fun saveAction(targetUserId: Int, action: String) {
        // Creates "User_1.csv", "User_2.csv", etc.
        val fileName = "User_${targetUserId}.csv"

        // Save to Internal Storage (Private to app)
        val file = File(filesDir, fileName)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        // Create Header if this is the FIRST time this specific user is being logged
        if (!file.exists()) {
            try {
                FileOutputStream(file, true).use { output ->
                    output.write("user_id,activity,timestamp\n".toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Save the data
        val data = "$targetUserId,$action,$timestamp\n"

        try {
            FileOutputStream(file, true).use { output ->
                output.write(data.toByteArray())
            }
            Toast.makeText(this, "Saved to $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show()
        }
    }
}