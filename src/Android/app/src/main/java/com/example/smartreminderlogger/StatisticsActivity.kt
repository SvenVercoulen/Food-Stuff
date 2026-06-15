package com.example.smartreminderlogger

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class SearchUser(val id: Int, val name: String)

class StatisticsActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var llStatsContainer: LinearLayout
    private val allUsers = mutableListOf<SearchUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.statistics_activity)

        etSearch = findViewById(R.id.etSearchUser)
        llStatsContainer = findViewById(R.id.llStatsContainer)

        loadProfiles()
        displayUserCards(allUsers) // Show everyone initially

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndDisplay(s.toString())
            }
        })
    }

    private fun loadProfiles() {
        allUsers.clear()
        val file = File(filesDir, "Profiles.csv")
        if (file.exists()) {
            val lines = file.readLines().drop(1)
            for (line in lines) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    allUsers.add(SearchUser(parts[0].toInt(), parts[1]))
                }
            }
        }
    }

    private fun filterAndDisplay(query: String) {
        if (query.isEmpty()) {
            displayUserCards(allUsers)
        } else {
            val filtered = allUsers.filter { it.name.lowercase().contains(query.lowercase()) }
            displayUserCards(filtered)
        }
    }

    private fun displayUserCards(users: List<SearchUser>) {
        llStatsContainer.removeAllViews() // Clear layout list completely

        val inflater = LayoutInflater.from(this)

        for (user in users) {
            // Inflate our card design
            val cardView = inflater.inflate(R.layout.item_user_stats, llStatsContainer, false)

            // Reference UI elements inside our newly created card view block
            val tvName = cardView.findViewById<TextView>(R.id.tvCardName)
            val tvEat = cardView.findViewById<TextView>(R.id.tvCardEat)
            val tvDrink = cardView.findViewById<TextView>(R.id.tvCardDrink)
            val tvOutsideMins = cardView.findViewById<TextView>(R.id.tvCardOutsideMins)
            val tvOutsideWeek = cardView.findViewById<TextView>(R.id.tvCardOutsideWeek)
            val tvLastEat = cardView.findViewById<TextView>(R.id.tvCardLastEat)
            val tvLastDrink = cardView.findViewById<TextView>(R.id.tvCardLastDrink)

            tvName.text = user.name

            // Load CSV metrics data details
            val file = File(filesDir, "User_${user.id}.csv")
            if (file.exists()) {
                var eatToday = 0
                var drinkToday = 0
                var outsideMinutesToday = 0L
                var outsideCountWeek = 0
                var lastEat = "Never"
                var lastDrink = "Never"

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val now = Date()
                val oneWeekAgo = Date(now.time - TimeUnit.DAYS.toMillis(7))
                var lastOutTime: Date? = null

                file.readLines().drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val activity = parts[1]
                        val timestampStr = parts[2]
                        val dateObj = try { dateFormat.parse(timestampStr) } catch (e: Exception) { null }
                        val dateOnlyStr = timestampStr.split(" ")[0]

                        if (dateObj != null) {
                            if (dateOnlyStr == todayStr) {
                                if (activity == "eat") eatToday++
                                if (activity == "drink") drinkToday++
                            }
                            if (activity.contains("outside") && dateObj.after(oneWeekAgo)) {
                                if (activity == "outside_out") outsideCountWeek++
                            }
                            if (activity == "eat") lastEat = timestampStr
                            if (activity == "drink") lastDrink = timestampStr

                            if (dateOnlyStr == todayStr) {
                                if (activity == "outside_out") {
                                    lastOutTime = dateObj
                                } else if (activity == "outside_in" && lastOutTime != null) {
                                    val diff = dateObj.time - lastOutTime!!.time
                                    outsideMinutesToday += TimeUnit.MILLISECONDS.toMinutes(diff)
                                    lastOutTime = null
                                }
                            }
                        }
                    }
                }

                tvEat.text = "Meals today: $eatToday"
                tvDrink.text = "Drinks today: $drinkToday"
                tvOutsideMins.text = "Outside today: ${outsideMinutesToday}m"
                tvOutsideWeek.text = "Outside week: ${outsideCountWeek}x"
                tvLastEat.text = "Last meal: $lastEat"
                tvLastDrink.text = "Last drink: $lastDrink"
            } else {
                // Default fallback if a user has zero files
                tvEat.text = "Meals today: 0"
                tvDrink.text = "Drinks today: 0"
                tvOutsideMins.text = "Outside today: 0m"
                tvOutsideWeek.text = "Outside week: 0x"
                tvLastEat.text = "Last meal: Never"
                tvLastDrink.text = "Last drink: Never"
            }

            // Put layout box down inside container frame safely
            llStatsContainer.addView(cardView)
        }
    }
}