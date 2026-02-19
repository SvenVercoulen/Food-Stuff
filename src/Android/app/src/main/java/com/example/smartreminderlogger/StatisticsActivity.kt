package com.example.smartreminderlogger

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// class for list display
data class SearchUser(val id: Int, val name: String) {
    override fun toString(): String = name
}

class StatisticsActivity : AppCompatActivity() {

    // UI Components
    private lateinit var etSearch: EditText
    private lateinit var lvResults: ListView
    private lateinit var svStats: ScrollView

    // Stats TextViews
    private lateinit var tvName: TextView
    private lateinit var tvEat: TextView
    private lateinit var tvDrink: TextView
    private lateinit var tvOutsideMins: TextView
    private lateinit var tvOutsideWeek: TextView
    private lateinit var tvLastEat: TextView
    private lateinit var tvLastDrink: TextView

    // Data
    private val allUsers = mutableListOf<SearchUser>()
    private val filteredUsers = mutableListOf<SearchUser>()
    private lateinit var adapter: ArrayAdapter<SearchUser>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.statistics_activity)

        initViews()
        loadProfiles()

        // Setup Search Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
        })

        // Setup List Click Listener
        lvResults.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = filteredUsers[position]
            loadUserStats(selectedUser)

            // Hide list, show stats
            lvResults.visibility = View.GONE
            svStats.visibility = View.VISIBLE
            etSearch.setSelection(etSearch.text.length)
        }
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearchUser)
        lvResults = findViewById(R.id.lvSearchResults)
        svStats = findViewById(R.id.svStatsContainer)

        tvName = findViewById(R.id.tvSelectedUserName)
        tvEat = findViewById(R.id.tvEatCount)
        tvDrink = findViewById(R.id.tvDrinkCount)
        tvOutsideMins = findViewById(R.id.tvOutsideMinutes)
        tvOutsideWeek = findViewById(R.id.tvOutsideWeek)
        tvLastEat = findViewById(R.id.tvLastEat)
        tvLastDrink = findViewById(R.id.tvLastDrink)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredUsers)
        lvResults.adapter = adapter
    }

    private fun loadProfiles() {
        val file = File(filesDir, "Profiles.csv")
        if (file.exists()) {
            val lines = file.readLines().drop(1) // Skip header
            for (line in lines) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    allUsers.add(SearchUser(parts[0].toInt(), parts[1]))
                }
            }
        }
    }

    private fun filterList(query: String) {
        filteredUsers.clear()
        if (query.isEmpty()) {
            lvResults.visibility = View.GONE
            svStats.visibility = View.GONE
        } else {
            val lowerQuery = query.lowercase()
            allUsers.forEach { user ->
                if (user.name.lowercase().contains(lowerQuery)) {
                    filteredUsers.add(user)
                }
            }
            adapter.notifyDataSetChanged()
            lvResults.visibility = View.VISIBLE
            svStats.visibility = View.GONE
        }
    }

    private fun loadUserStats(user: SearchUser) {
        val file = File(filesDir, "User_${user.id}.csv")
        tvName.text = "Statistics for ${user.name}"

        if (!file.exists()) {
            resetStats()
            return
        }

        var eatToday = 0
        var drinkToday = 0
        var outsideMinutesToday = 0L
        var outsideCountWeek = 0
        var lastEat = "Never"
        var lastDrink = "Never"

        // Date Helpers
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = Date()
        val oneWeekAgo = Date(now.time - TimeUnit.DAYS.toMillis(7))

        // Logic for calculating "Time Outside"
        var lastOutTime: Date? = null

        val lines = file.readLines().drop(1) // Skip header: user_id,activity,timestamp

        for (line in lines) {
            val parts = line.split(",")
            if (parts.size < 3) continue

            val activity = parts[1]
            val timestampStr = parts[2]
            val dateObj = try { dateFormat.parse(timestampStr) } catch (e: Exception) { null } ?: continue
            val dateOnlyStr = timestampStr.split(" ")[0]

            // Daily Counts (Eat/Drink)
            if (dateOnlyStr == todayStr) {
                if (activity == "eat") eatToday++
                if (activity == "drink") drinkToday++
            }

            // Weekly Outside Count
            if (activity.contains("outside") && dateObj.after(oneWeekAgo)) {
                // Count every time they went OUT as 1 trip
                if (activity == "outside_out") outsideCountWeek++
            }

            // Last Times
            if (activity == "eat") lastEat = timestampStr
            if (activity == "drink") lastDrink = timestampStr

            // Outside Minutes Calculation
            if (dateOnlyStr == todayStr) {
                if (activity == "outside_out") {
                    lastOutTime = dateObj
                } else if (activity == "outside_in" && lastOutTime != null) {
                    val diff = dateObj.time - lastOutTime.time
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    outsideMinutesToday += minutes
                    lastOutTime = null // Reset pair
                }
            }
        }

        // Set Text Views
        tvEat.text = "Meals today: $eatToday"
        tvDrink.text = "Drinks today: $drinkToday"
        tvOutsideMins.text = "Time outside today: $outsideMinutesToday mins"
        tvOutsideWeek.text = "Outside this week: $outsideCountWeek times"
        tvLastEat.text = "Last meal: $lastEat"
        tvLastDrink.text = "Last drink: $lastDrink"
    }

    private fun resetStats() {
        tvEat.text = "Meals today: 0"
        tvDrink.text = "Drinks today: 0"
        tvOutsideMins.text = "Time outside today: 0 mins"
        tvOutsideWeek.text = "Outside this week: 0 times"
        tvLastEat.text = "Last meal: Never"
        tvLastDrink.text = "Last drink: Never"
    }
}