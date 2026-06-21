package com.example.smartreminderlogger

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Data class to represent a user and their current alert status
data class ElderlyUser(val id: Int, val name: String, var needsEat: Boolean = false, var needsDrink: Boolean = false) {
    override fun toString(): String = name
}

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private var userList = mutableListOf<ElderlyUser>()

    // Prevents notification spam. Keeps track of who we already notified this session.
    private val notifiedAlerts = mutableSetOf<String>()
    private val CHANNEL_ID = "SmartReminderChannel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // permissions at startup
        createNotificationChannel()
        requestNotificationPermission()

        listView = findViewById(R.id.userListView)

        // Navigation button listeners
        findViewById<Button>(R.id.btnAddUser).setOnClickListener {
            startActivity(Intent(this, CreateUserActivity::class.java))
        }
        findViewById<Button>(R.id.btnStats).setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
        findViewById<Button>(R.id.btnPredict).setOnClickListener {
            startActivity(Intent(this, ForecastActivity::class.java))
        }
        findViewById<Button>(R.id.btnRecommend).setOnClickListener {
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }

        // notification test button
        findViewById<ImageView>(R.id.ivTestNotification).setOnClickListener {
            sendNotification("Test Alert", "This is a test notification from the system!", 999)
            Toast.makeText(this, "Test notification triggered", Toast.LENGTH_SHORT).show()
        }

        // Click a user in the list to log an activity for them
        listView.setOnItemClickListener { _, _, position, _ ->
            showActionDialog(userList[position])
        }

        loadUsersFromCsv()
    }

    override fun onResume() {
        super.onResume()
        loadUsersFromCsv()// Reload list when returning to this screen
    }
    // Reads user list from storage and checks if they need alerts
    private fun loadUsersFromCsv() {
        userList.clear()
        val file = File(filesDir, "Profiles.csv")

        if (file.exists()) {
            val lines = file.readLines().drop(1)
            for (line in lines) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    val userId = parts[0].toInt()
                    val name = parts[1]

                    // Check statuses ONCE when loading data (check if 12h has passed for eating and 6h for drinking)
                    val needsEat = checkNeedsAction(userId, "eat", 12 * 60 * 60 * 1000L)
                    val needsDrink = checkNeedsAction(userId, "drink", 6 * 60 * 60 * 1000L)

                    userList.add(ElderlyUser(userId, name, needsEat, needsDrink))
                }
            }
        }

        // Update the list UI
        val adapter = ResidentAdapter(this, userList)
        listView.adapter = adapter

        triggerRealNotifications()
    }

    // Readds a user's log file to find the last time they performed an action
    private fun checkNeedsAction(userId: Int, actionType: String, limitInMillis: Long): Boolean {
        val file = File(filesDir, "User_$userId.csv")
        if (!file.exists()) return true// If no file, assume they need help/input

        var lastActionTime = 0L
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        file.readLines().drop(1).forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 3 && parts[1] == actionType) {
                try {
                    val date = dateFormat.parse(parts[2])
                    if (date != null && date.time > lastActionTime) {
                        lastActionTime = date.time
                    }
                } catch (e: Exception) {}
            }
        }

        if (lastActionTime == 0L) return true

        // Compare current time with last action time to see if limit is exceeded
        return (System.currentTimeMillis() - lastActionTime) > limitInMillis
    }

    // go through users to send alerts if they missed a deadline
    private fun triggerRealNotifications() {
        for (user in userList) {
            val eatKey = "${user.id}_eat"// Check for missed meal
            if (user.needsEat) {
                if (!notifiedAlerts.contains(eatKey)) {
                    sendNotification("Missed Meal Alert", "${user.name} hasn't had anything to eat for 12 hours now!", user.id * 10)
                    notifiedAlerts.add(eatKey)
                }
            } else {
                notifiedAlerts.remove(eatKey) // Clear "need to eat" status if they have eaten
            }

            // Check Drink
            val drinkKey = "${user.id}_drink"
            if (user.needsDrink) {
                if (!notifiedAlerts.contains(drinkKey)) {
                    sendNotification("Dehydration Alert", "${user.name} hasn't had anything to drink for 6 hours now!", (user.id * 10) + 1)
                    notifiedAlerts.add(drinkKey)
                }
            } else {
                notifiedAlerts.remove(drinkKey) // Reset if they drank
            }
        }
    }

    // this method is needed for Android 8.0+ to show notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Resident Alerts"
            val descriptionText = "Notifications for missed meals and drinks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    // this method is needed for Android 13+ to show notifications
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    // Helper to build and show the actual notification
    private fun sendNotification(title: String, message: String, notificationId: Int) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return // Don't send if permission is denied
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
    }

    // Opens a popup dialog to log an activity for the selected user
    private fun showActionDialog(user: ElderlyUser) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_actions, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = "Log for ${user.name}"

        // Handle buttons within the dialog
        dialogView.findViewById<Button>(R.id.btnDialogEat).setOnClickListener {
            saveAction(user.id, "eat")
            dialog.dismiss()
            loadUsersFromCsv()
        }
        dialogView.findViewById<Button>(R.id.btnDialogDrink).setOnClickListener {
            saveAction(user.id, "drink")
            dialog.dismiss()
            loadUsersFromCsv()
        }
        dialogView.findViewById<Button>(R.id.btnDialogOutside).setOnClickListener {
            saveAction(user.id, "outside_out")
            dialog.dismiss()
        }
        dialog.show()
    }

    // method saves the timestamp of the action to the user's CSV file
    private fun saveAction(targetUserId: Int, action: String) {
        val fileName = "User_${targetUserId}.csv"
        val file = File(filesDir, fileName)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        // Create file header if it doesn't exist
        if (!file.exists()) {
            FileOutputStream(file, true).use { it.write("user_id,activity,timestamp\n".toByteArray()) }
        }

        val data = "$targetUserId,$action,$timestamp\n"
        FileOutputStream(file, true).use { it.write(data.toByteArray()) }
        Toast.makeText(this, "Saved to $fileName", Toast.LENGTH_SHORT).show()
    }
}

//adapter to display users in the ListView and update their alert icons
class ResidentAdapter(context: Context, private val users: List<ElderlyUser>) : ArrayAdapter<ElderlyUser>(context, 0, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_resident, parent, false)

        val tvName = view.findViewById<TextView>(R.id.tvResidentName)
        val ivRedAlert = view.findViewById<ImageView>(R.id.ivRedAlert)
        val ivBlueAlert = view.findViewById<ImageView>(R.id.ivBlueAlert)

        val user = users[position]
        tvName.text = user.name

        // Toggle visibility of alert icons based on the boolean status calculated earlier
        ivRedAlert.visibility = if (user.needsEat) View.VISIBLE else View.GONE
        ivBlueAlert.visibility = if (user.needsDrink) View.VISIBLE else View.GONE

        return view
    }
}