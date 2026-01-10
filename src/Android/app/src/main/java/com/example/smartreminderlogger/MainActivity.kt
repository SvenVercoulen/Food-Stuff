package com.example.smartreminderlogger

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var isOutside = false
    private var outsideStartTime: Long = 0
    private val userId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnForecast = findViewById<Button>(R.id.btnForecast)
        btnForecast.setOnClickListener {
            val intent = Intent(this, ForecastActivity::class.java)
            startActivity(intent)
        }

        val btnStats = findViewById<Button>(R.id.btnStats)
        btnStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        val btnEat = findViewById<Button>(R.id.btnEat)
        val btnDrink = findViewById<Button>(R.id.btnDrink)
        val btnOutside = findViewById<Button>(R.id.btnGoOutside)

        btnEat.setOnClickListener {
            logEvent("eat")
            Toast.makeText(this, "Logged: Eat", Toast.LENGTH_SHORT).show()
        }

        btnDrink.setOnClickListener {
            logEvent("drink")
            Toast.makeText(this, "Logged: Drink", Toast.LENGTH_SHORT).show()
        }


        btnOutside.setOnClickListener {
            if (!isOutside) {
                isOutside = true
                outsideStartTime = System.currentTimeMillis()
                btnOutside.text = "Back Inside"
                logEvent("outside_in")
                Toast.makeText(this, "Logged: Outside Start", Toast.LENGTH_SHORT).show()
            } else {
                isOutside = false
                btnOutside.text = "Go Outside"
                logEvent("outside_out")

                val duration = System.currentTimeMillis() - outsideStartTime
                val minutes = duration / 1000 / 60
                Toast.makeText(this, "Outside for $minutes minutes", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun logEvent(activity: String) {
        val timestamp = getCurrentTimestamp()
        val line = "$userId,$activity,$timestamp\n"

        val file = File(filesDir, "UserData.csv")
        val writeHeader = !file.exists()

        val writer = FileWriter(file, true)
        if (writeHeader) {
            writer.append("user_id,activity,timestamp\n")
        }

        writer.append(line)
        writer.flush()
        writer.close()
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}