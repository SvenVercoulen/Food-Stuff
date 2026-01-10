package com.example.smartreminderlogger

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.FloatBuffer
import java.time.LocalDate
import java.util.Collections

class ForecastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        // Calculate features from UserData.csv
        val features = calculateFeatures()

        if (features != null) {
            // Run Predictions
            val drinksTomorrow = runPrediction("drink_model.onnx", features)
            val mealsTomorrow = runPrediction("eat_model.onnx", features)

            // Show Results
            findViewById<TextView>(R.id.tvDrinkPrediction).text =
                "💧 Drink Goal: ${String.format("%.1f", drinksTomorrow)} times"

            findViewById<TextView>(R.id.tvEatPrediction).text =
                "🍽️ Eat Goal: ${String.format("%.1f", mealsTomorrow)} times"
        } else {
            findViewById<TextView>(R.id.tvDrinkPrediction).text = "Not enough data yet!"
        }
    }

    // --- LOGIC TO MIMIC YOUR PYTHON FEATURE EXTRACTION ---
    private fun calculateFeatures(): FloatArray? {
        val today = LocalDate.now()

        // Read CSV and parse dates
        val entries = readCsvData()
        if (entries.isEmpty()) return null

        // get Today's Counts
        val todayStr = today.toString()
        val drinksToday = entries.filter { it.date == todayStr && it.activity == "drink" }.size.toFloat()
        val mealsToday = entries.filter { it.date == todayStr && it.activity == "eat" }.size.toFloat()

        // calc 7-Day Averages (The "Memory"), look back 7 days BEFORE today
        var drinkSum = 0
        var eatSum = 0
        var daysWithData = 0

        for (i in 1..7) {
            val checkDate = today.minusDays(i.toLong()).toString()
            val dayDrinks = entries.filter { it.date == checkDate && it.activity == "drink" }.size
            val dayMeals = entries.filter { it.date == checkDate && it.activity == "eat" }.size

            // Only count if there was activity (mimicking 'min_periods=1' somewhat)
            drinkSum += dayDrinks
            eatSum += dayMeals
            daysWithData++
        }

        val avgDrink = if (daysWithData > 0) drinkSum.toFloat() / 7f else drinksToday
        val avgEat = if (daysWithData > 0) eatSum.toFloat() / 7f else mealsToday

        // time features (Python's day_of_week is Mon=0, Sun=6. Java's DayOfWeek is Mon=1, Sun=7,must convert to match Python)
        val dayOfWeek = (today.dayOfWeek.value - 1).toFloat()
        val isWeekend = if (dayOfWeek >= 5) 1f else 0f

        // pack into array (MUST match the order used in Python training)
        // ['day_of_week', 'is_weekend', 'drink', 'eat', 'drink_7day_avg', 'eat_7day_avg']
        return floatArrayOf(dayOfWeek, isWeekend, drinksToday, mealsToday, avgDrink, avgEat)
    }

    private fun runPrediction(modelName: String, inputs: FloatArray): Float {
        try {
            val env = OrtEnvironment.getEnvironment()
            // Read model from assets
            val modelBytes = assets.open(modelName).readBytes()
            val session = env.createSession(modelBytes)

            // Create input tensor (Shape: [1, 6])
            val floatBuffer = FloatBuffer.wrap(inputs)
            val tensor = OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 6))

            // Run inference
            val result = session.run(Collections.singletonMap("float_input", tensor))

            // Extract result (The model returns a 2D array [[prediction]])
            val outputTensor = result[0] as OnnxTensor
            val outputArray = outputTensor.floatBuffer.array()

            return outputArray[0] // Return the single prediction

        } catch (e: Exception) {
            Log.e("AI_ERROR", "Error running model $modelName", e)
            return 0f
        }
    }

    // Helper data class for parsing
    data class CsvEntry(val activity: String, val date: String)

    private fun readCsvData(): List<CsvEntry> {
        val list = mutableListOf<CsvEntry>()
        try {
            val file = File(filesDir, "UserData.csv")

            if (!file.exists()) return emptyList()

            val reader = file.bufferedReader()
            reader.forEachLine { line ->
                if (!line.startsWith("user_id")) {
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val activity = parts[1]
                        val timestamp = parts[2]
                        val date = timestamp.split(" ")[0]
                        list.add(CsvEntry(activity, date))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CSV", "Error reading data", e)
        }
        return list
    }
}