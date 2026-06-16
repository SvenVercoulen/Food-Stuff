package com.example.smartreminderlogger

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
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

        // Use our new Custom Adapter instead of the default Android one
        val adapter = ResidentAdapter(this, userList, filesDir)
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
            loadUsersFromCsv() // Refresh the list instantly to clear the alert icon if they just ate
        }

        dialogView.findViewById<Button>(R.id.btnDialogDrink).setOnClickListener {
            saveAction(user.id, "drink")
            dialog.dismiss()
            loadUsersFromCsv()
        }

        dialogView.findViewById<Button>(R.id.btnDialogOutside).setOnClickListener {
            saveAction(user.id, "outside_out") // Simplified for demo
            dialog.dismiss()
        }

        dialog.show()
    }

    // SAVING LOGIC
    private fun saveAction(targetUserId: Int, action: String) {
        val fileName = "User_${targetUserId}.csv" // Creates "User_1.csv", "User_2.csv", etc.
        val file = File(filesDir, fileName)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        if (!file.exists()) {
            try {
                FileOutputStream(file, true).use { output ->
                    output.write("user_id,activity,timestamp\n".toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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

// --- CUSTOM ADAPTER FOR LISTVIEW ---
class ResidentAdapter(
    context: Context,
    private val users: List<ElderlyUser>,
    private val filesDir: File
) : ArrayAdapter<ElderlyUser>(context, 0, users) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val twelveHoursInMillis = 12 * 60 * 60 * 1000L // 12 hours in milliseconds
    private val sixHoursInMillis = 6 * 60 * 60 * 1000L // 6 hours in ms

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Inflate our custom item_resident.xml layout
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_resident, parent, false)

        val tvName = view.findViewById<TextView>(R.id.tvResidentName)
        val ivRedAlert = view.findViewById<ImageView>(R.id.ivRedAlert)
        val ivBlueAlert = view.findViewById<ImageView>(R.id.ivBlueAlert)

        val user = users[position]
        tvName.text = user.name

        // Check if user has to eat or drink
        if (needsToEat(user.id)) {
            ivRedAlert.visibility = View.VISIBLE
        } else {
            ivRedAlert.visibility = View.GONE
        }

        if (needsToDrink(user.id)) {
            ivBlueAlert.visibility = View.VISIBLE
        } else {
            ivBlueAlert.visibility = View.GONE
        }
        return view
    }

    private fun needsToEat(userId: Int): Boolean {
        val file = File(filesDir, "User_$userId.csv")

        // If they have no log file at all, assume they need to eat
        if (!file.exists()) return true

        var lastEatTime: Long = 0L

        file.readLines().drop(1).forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 3 && parts[1] == "eat") {
                try {
                    val date = dateFormat.parse(parts[2])
                    if (date != null && date.time > lastEatTime) {
                        lastEatTime = date.time // Update to the most recent meal
                    }
                } catch (e: Exception) { }
            }
        }

        // If a log exists but no "eat" events are found, show alert
        if (lastEatTime == 0L) return true

        val currentTime = System.currentTimeMillis()

        // Return true if the difference is greater than 12 hours
        return (currentTime - lastEatTime) > twelveHoursInMillis
    }
    private fun needsToDrink(userId: Int): Boolean {
        val file = File(filesDir, "User_$userId.csv")

        // If they have no log file at all, assume they need to eat
        if (!file.exists()) return true

        var lastDrinkTime: Long = 0L

        file.readLines().drop(1).forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 3 && parts[1] == "drink") {
                try {
                    val date = dateFormat.parse(parts[2])
                    if (date != null && date.time > lastDrinkTime) {
                        lastDrinkTime = date.time // Update to the most recent meal
                    }
                } catch (e: Exception) { }
            }
        }

        // If a log exists but no "eat" events are found, show alert
        if (lastDrinkTime == 0L) return true

        val currentTime = System.currentTimeMillis()

        // Return true if the difference is greater than 12 hours
        return (currentTime - lastDrinkTime) > sixHoursInMillis
    }
}