package com.example.smartreminderlogger

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.io.File
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// data class for the list
data class ForecastUser(val id: Int, val name: String) {
    override fun toString(): String = name
}

class ForecastActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var lvUsers: ListView
    private lateinit var llResult: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvDrinkPred: TextView
    private lateinit var tvEatPred: TextView
    private lateinit var tvProgress: TextView
    private lateinit var btnBack: Button

    private val allUsers = mutableListOf<ForecastUser>()
    private val filteredUsers = mutableListOf<ForecastUser>()
    private lateinit var adapter: ArrayAdapter<ForecastUser>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        initViews()
        loadProfiles()

        // Search Filter Logic
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
        })

        // User Selection Logic
        lvUsers.setOnItemClickListener { _, _, position, _ ->
            val user = filteredUsers[position]
            showPredictionForUser(user)
        }

        // Back Button Logic
        btnBack.setOnClickListener {
            llResult.visibility = View.GONE
            lvUsers.visibility = View.VISIBLE
            etSearch.visibility = View.VISIBLE
            etSearch.setText("")
        }
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearchForecast)
        lvUsers = findViewById(R.id.lvForecastUsers)
        llResult = findViewById(R.id.llPredictionResult)

        tvTitle = findViewById(R.id.tvSelectedUserTitle)
        tvDrinkPred = findViewById(R.id.tvDrinkPrediction)
        tvEatPred = findViewById(R.id.tvEatPrediction)
        tvProgress = findViewById(R.id.DrinkProgress)
        btnBack = findViewById(R.id.btnBackToList)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredUsers)
        lvUsers.adapter = adapter
    }

    private fun loadProfiles() {
        allUsers.clear()
        val file = File(filesDir, "Profiles.csv")
        if (file.exists()) {
            val lines = file.readLines().drop(1)
            for (line in lines) {
                val parts = line.split(",")
                if (parts.size >= 2) {
                    allUsers.add(ForecastUser(parts[0].toInt(), parts[1]))
                }
            }
            filterList("") // Load initial list
        }
    }

    private fun filterList(query: String) {
        filteredUsers.clear()
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers)
        } else {
            val lower = query.lowercase()
            allUsers.forEach { if (it.name.lowercase().contains(lower)) filteredUsers.add(it) }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showPredictionForUser(user: ForecastUser) {
        // Hide list, show result container
        hideKeyboard()
        lvUsers.visibility = View.GONE
        etSearch.visibility = View.GONE
        llResult.visibility = View.VISIBLE
        tvTitle.text = "Plan for ${user.name}"

        // Calculate Features for THIS user
        val features = calculateFeatures(user.id)

        if (features != null) {
            // Run Predictions
            val drinksTomorrow = runPrediction("drink_model.onnx", features)
            val mealsTomorrow = runPrediction("eat_model.onnx", features)

            // Update UI
            tvDrinkPred.text = "💧 Drinking prediction: ${String.format("%.1f", drinksTomorrow)} times"
            tvEatPred.text = "🍽️ Eating prediction: ${String.format("%.1f", mealsTomorrow)} times"

            // Progress text
            val drinksToday = features[2]
            tvProgress.text = "Current status today:\nDrinks: ${drinksToday.toInt()} | Meals: ${features[3].toInt()}"
        } else {
            tvDrinkPred.text = "Not enough data"
            tvEatPred.text = "to make a prediction."
            tvProgress.text = ""
        }
    }


    private fun calculateFeatures(userId: Int): FloatArray? {
        val file = File(filesDir, "User_$userId.csv")
        if (!file.exists()) return null

        val entries = mutableListOf<CsvEntry>()
        val lines = file.readLines().drop(1)

        for (line in lines) {
            val parts = line.split(",")
            if (parts.size >= 3) {
                val act = parts[1]
                val ts = parts[2]
                val date = ts.split(" ")[0]
                entries.add(CsvEntry(act, date))
            }
        }

        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // Better Day of Week logic (Monday=0, Sunday=6)
        val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val isWeekend = if (dayOfWeek >= 5) 1f else 0f

        var drinkToday = 0f
        var eatToday = 0f
        var drinkWeekCount = 0
        var eatWeekCount = 0

        // Set oneWeekAgo to exactly 7 days ago at MIDNIGHT to be safe
        val calWeek = Calendar.getInstance()
        calWeek.add(Calendar.DAY_OF_YEAR, -7)
        calWeek.set(Calendar.HOUR_OF_DAY, 0)
        val oneWeekAgoDate = calWeek.time

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        entries.forEach { entry ->
            if (entry.date == today) {
                if (entry.activity == "drink") drinkToday++
                if (entry.activity == "eat") eatToday++
            }

            try {
                val entryDate = sdf.parse(entry.date)
                if (entryDate != null && !entryDate.before(oneWeekAgoDate)) {
                    if (entry.activity == "drink") drinkWeekCount++
                    if (entry.activity == "eat") eatWeekCount++
                }
            } catch (e: Exception) { }
        }

        val drinkAvg = drinkWeekCount / 7f
        val eatAvg = eatWeekCount / 7f

        val features = floatArrayOf(
            dayOfWeek.toFloat(),
            isWeekend,
            drinkToday,
            eatToday,
            drinkAvg,
            eatAvg
        )

        // DEBUG
        Log.d("AI_INPUT", "User $userId Features: ${features.joinToString(", ")}")

        return features
    }

    // Update the runPrediction function
    private fun runPrediction(modelName: String, inputData: FloatArray): Float {
        try {
            val env = OrtEnvironment.getEnvironment()

            // Ensure the model exists in internal storage
            val modelPath = File(filesDir, modelName).absolutePath
            if (!File(modelPath).exists()) {
                copyAssetToInternalStorage(modelName)
            }

            val session = env.createSession(modelPath)

            // Create Tensor
            val shape = longArrayOf(1, 6)
            val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)

            // Run prediction using the model's actual input name
            val inputName = session.inputNames.iterator().next()
            val result = session.run(Collections.singletonMap(inputName, tensor))

            // SAFE WAY TO GET THE RESULT:
            val outputTensor = result[0] as OnnxTensor
            val outputValue = outputTensor.floatBuffer.get(0) // Don't use .array()

            result.close()
            session.close()

            return outputValue
        } catch (e: Exception) {
            Log.e("AI_PREDICT", "Error running $modelName: ${e.message}")
            return 0f
        }
    }

    // function to move models from assets to internal storage
    private fun copyAssetToInternalStorage(fileName: String) {
        assets.open(fileName).use { inputStream ->
            File(filesDir, fileName).outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    data class CsvEntry(val activity: String, val date: String)
}