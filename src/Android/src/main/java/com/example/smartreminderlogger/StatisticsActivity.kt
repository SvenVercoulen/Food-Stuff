package com.example.smartreminderlogger

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.statistics_activity)

        val eatCountView = findViewById<TextView>(R.id.statEatCount)
        val drinkCountView = findViewById<TextView>(R.id.statDrinkCount)
        val outsideMinutesView = findViewById<TextView>(R.id.statOutsideMinutes)
        val outsideWeekView = findViewById<TextView>(R.id.statOutsideWeek)
        val sinceEatView = findViewById<TextView>(R.id.statSinceEat)
        val sinceDrinkView = findViewById<TextView>(R.id.statSinceDrink)

        val file = File(getExternalFilesDir(null), "UserData.csv")
        if (!file.exists()) {
            eatCountView.text = "No data found"
            return
        }

        val lines = file.readLines().drop(1) // Skip header

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        var eatCountToday = 0
        var drinkCountToday = 0
        var outsideMinutesToday = 0L
        var outsideCountThisWeek = 0
        var lastEatTime = 0L
        var lastDrinkTime = 0L

        var lastOutsideStart = 0L
        var outsideOpen = false

        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        for (line in lines) {
            val parts = line.split(",")
            val activity = parts[1]
            val timestamp = dateFormat.parse(parts[2])?.time ?: continue

            val lineDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

            // Today counts
            if (lineDay == today) {
                if (activity == "eat") eatCountToday++
                if (activity == "drink") drinkCountToday++
            }

            // Time since last
            if (activity == "eat") lastEatTime = timestamp
            if (activity == "drink") lastDrinkTime = timestamp

            // Weekly outdoor count
            if (activity == "outside_in") {
                val cal = Calendar.getInstance()
                cal.time = Date(timestamp)
                if (cal.get(Calendar.WEEK_OF_YEAR) == currentWeek) {
                    outsideCountThisWeek++
                }
                lastOutsideStart = timestamp
                outsideOpen = true
            }

            if (activity == "outside_out" && outsideOpen) {
                outsideOpen = false
                if (lineDay == today) {
                    outsideMinutesToday += (timestamp - lastOutsideStart)
                }
            }
        }

        eatCountView.text = "Eaten today: $eatCountToday"
        drinkCountView.text = "Drank today: $drinkCountToday"
        outsideMinutesView.text =
            "Time outside today: ${(outsideMinutesToday / 60000)} minutes"
        outsideWeekView.text = "Times outside this week: $outsideCountThisWeek"

        sinceEatView.text = "Time since last meal: " +
                formatTimeDifference(now - lastEatTime)

        sinceDrinkView.text = "Time since last drink: " +
                formatTimeDifference(now - lastDrinkTime)
    }

    private fun formatTimeDifference(ms: Long): String {
        val minutes = (ms / 60000).toInt()
        val hours = minutes / 60
        val remMinutes = minutes % 60
        return "${hours}h ${remMinutes}m"
    }
}
