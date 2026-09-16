package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AacUsage
import com.example.data.AppDatabase
import com.example.data.RoutineTask
import com.example.data.UserPreferences
import com.example.network.askGemini
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val routineDao = db.routineTaskDao()
    private val aacDao = db.aacUsageDao()
    val userPreferences = UserPreferences(application)

    private var tts: TextToSpeech? = null
    val isBoyVoice = MutableStateFlow(true) // True for boy (lower pitch), False for girl (higher pitch)
    
    fun toggleVoice() {
        isBoyVoice.value = !isBoyVoice.value
        tts?.setPitch(if (isBoyVoice.value) 0.7f else 1.5f)
    }
    
    val currentLocationContext = MutableStateFlow("Home")
    private var isLocationMonitoringStarted = false

    @SuppressLint("MissingPermission")
    fun startLocationMonitoring() {
        if (isLocationMonitoringStarted) return
        isLocationMonitoringStarted = true
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        
        viewModelScope.launch {
            while (true) {
                try {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                // Simulate context resolution based on movement and coordinates
                                // In a full production app, this would query a Geofencing API or Places API
                                val newContext = if (location.hasSpeed() && location.speed > 3.0) {
                                    "Transit"
                                } else {
                                    // Create a stable but arbitrary mock mapping for demonstration
                                    val latHash = (location.latitude * 100).toInt()
                                    when (Math.abs(latHash) % 3) {
                                        0 -> "Home"
                                        1 -> "School"
                                        else -> "Public"
                                    }
                                }
                                currentLocationContext.value = newContext
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(15000) // Poll every 15 seconds
            }
        }
    }

    val routineTasks: StateFlow<List<RoutineTask>> = routineDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(if (isBoyVoice.value) 0.7f else 1.5f)
            }
        }
        
        viewModelScope.launch {
            routineTasks.collect { tasks ->
                tasks.forEach { task ->
                    if (!task.isCompleted) {
                        scheduleTaskNotification(task)
                    }
                }
            }
        }
    }

    private fun scheduleTaskNotification(task: RoutineTask) {
        val alarmManager = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(getApplication<Application>(), com.example.NotificationReceiver::class.java).apply {
            putExtra("TASK_NAME", task.name)
            putExtra("TASK_ICON", task.icon)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            getApplication<Application>(),
            task.id,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val timeParts = task.time.split(":")
        if (timeParts.size != 2) return
        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return
        
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }
        
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    val aacOptions = MutableStateFlow<List<com.example.ui.AacItem>>(emptyList())

    fun updateAacOptionsForLocation(location: String) {
        val baseOptions = listOf(
            com.example.ui.AacItem("😊", "Happy", "I feel happy.", "Feelings"),
            com.example.ui.AacItem("😐", "Okay", "I feel okay.", "Feelings"),
            com.example.ui.AacItem("😟", "Worried", "I am worried.", "Feelings"),
            com.example.ui.AacItem("😣", "Uncomfortable", "I feel uncomfortable.", "Feelings"),
            com.example.ui.AacItem("😡", "Angry", "I am angry.", "Feelings"),
            com.example.ui.AacItem("😢", "Sad", "I am sad.", "Feelings"),
            com.example.ui.AacItem("🤕", "Pain", "I am in pain.", "Needs"),
            com.example.ui.AacItem("🥤", "Thirsty", "I am thirsty.", "Needs"),
            com.example.ui.AacItem("🍎", "Hungry", "I am hungry.", "Needs"),
            com.example.ui.AacItem("🛑", "Break", "I need a break.", "Needs"),
            com.example.ui.AacItem("❤️", "Help", "I need help.", "Needs"),
            com.example.ui.AacItem("🔇", "Too loud", "It is too loud.", "Sensory"),
            com.example.ui.AacItem("💡", "Too bright", "It is too bright.", "Sensory"),
            com.example.ui.AacItem("😴", "Tired", "I am tired.", "Sensory")
        )

        val locationOptions = when (location) {
            "School" -> listOf(
                com.example.ui.AacItem("✏️", "Pencil", "I need a pencil.", "Context: School"),
                com.example.ui.AacItem("🙋", "Question", "I have a question.", "Context: School"),
                com.example.ui.AacItem("🚻", "Bathroom", "I need to use the bathroom.", "Context: School"),
                com.example.ui.AacItem("🎧", "Headphones", "I need my noise-canceling headphones.", "Context: School")
            )
            "Public" -> listOf(
                com.example.ui.AacItem("🏠", "Go Home", "I want to go home now.", "Context: Public"),
                com.example.ui.AacItem("🛒", "Too Crowded", "It is too crowded here.", "Context: Public"),
                com.example.ui.AacItem("🛍️", "Look", "I want to look at this.", "Context: Public"),
                com.example.ui.AacItem("🧍", "Personal Space", "I need more personal space.", "Context: Public")
            )
            "Transit" -> listOf(
                com.example.ui.AacItem("🤢", "Sick", "I feel motion sick.", "Context: Transit"),
                com.example.ui.AacItem("🛑", "Stop", "I want to get off.", "Context: Transit"),
                com.example.ui.AacItem("🎵", "Music", "I want to listen to music.", "Context: Transit"),
                com.example.ui.AacItem("💺", "Sit", "I need to sit down.", "Context: Transit")
            )
            else -> listOf(
                com.example.ui.AacItem("📺", "TV", "I want to watch TV.", "Context: Home"),
                com.example.ui.AacItem("🛏️", "Rest", "I want to go to my room.", "Context: Home"),
                com.example.ui.AacItem("🫂", "Hug", "I need a hug.", "Context: Home"),
                com.example.ui.AacItem("🧸", "Toy", "I want my comfort item.", "Context: Home")
            )
        }
        
        aacOptions.value = locationOptions + baseOptions
    }

    val topPhrases = aacDao.getTopPhrases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routineCompletions = db.routineCompletionDao().getAllCompletions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnboardingCompleted = userPreferences.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val currentSoundLevel = MutableStateFlow(45f)
    val currentLightLevel = MutableStateFlow(200f)
    private val sensorMonitor = com.example.data.SensorEnvironmentMonitor(application)
    private var useRealSensors = MutableStateFlow(false)
    private var isSensingStarted = false

    fun startSensing() {
        if (isSensingStarted) return
        isSensingStarted = true
        useRealSensors.value = true

        viewModelScope.launch {
            sensorMonitor.getLightLevel().collect { level ->
                if (useRealSensors.value) {
                    currentLightLevel.value = level
                    checkEnvironmentState()
                }
            }
        }
        
        viewModelScope.launch {
            sensorMonitor.getSoundLevelDecibels().collect { level ->
                if (useRealSensors.value) {
                    currentSoundLevel.value = level
                    checkEnvironmentState()
                }
            }
        }
    }

    val caregiverPhone = userPreferences.caregiverPhone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        
    private var lastWarningTime = 0L

    private fun checkEnvironmentState() {
        val now = System.currentTimeMillis()
        if (now - lastWarningTime < 30000) return // Limit warnings to every 30s
        
        if (currentSoundLevel.value > 100f) {
            getAiSuggestion("The environment is too loud.")
            notifyCaregiver("The environment is too loud")
            lastWarningTime = now
        } else if (currentLightLevel.value > 1000f) {
            getAiSuggestion("The environment is too bright.")
            notifyCaregiver("The environment is too bright")
            lastWarningTime = now
        } else if (currentSoundLevel.value < 85f && currentLightLevel.value < 500f) {
            if (aiSuggestion.value.isNotEmpty() && aiSuggestion.value != "Thinking...") {
                aiSuggestion.value = "" // Clear suggestion when safe
            }
        }
    }

    fun completeOnboarding(sensitivity: String, family: String, friends: String, strangers: String, phone: String) {
        viewModelScope.launch {
            userPreferences.saveOnboardingData(sensitivity, family, friends, strangers, phone)
            
            // Populate initial tasks if empty
            if (routineTasks.value.isEmpty()) {
                routineDao.insertTask(RoutineTask(name = "Wake Up", icon = "☀️", time = "07:00", orderIndex = 0))
                routineDao.insertTask(RoutineTask(name = "Brush Teeth", icon = "🪥", time = "07:15", orderIndex = 1))
                routineDao.insertTask(RoutineTask(name = "Breakfast", icon = "🍳", time = "07:30", orderIndex = 2))
                routineDao.insertTask(RoutineTask(name = "School", icon = "🎒", time = "08:30", orderIndex = 3))
                routineDao.insertTask(RoutineTask(name = "Snack", icon = "🍎", time = "15:00", orderIndex = 4))
                routineDao.insertTask(RoutineTask(name = "Study", icon = "📚", time = "16:00", orderIndex = 5))
                routineDao.insertTask(RoutineTask(name = "Free Time", icon = "🎮", time = "17:00", orderIndex = 6))
                routineDao.insertTask(RoutineTask(name = "Dinner", icon = "🍽️", time = "18:30", orderIndex = 7))
                routineDao.insertTask(RoutineTask(name = "Sleep", icon = "🌙", time = "21:00", orderIndex = 8))
            }
        }
    }

    fun toggleTaskCompletion(task: RoutineTask) {
        viewModelScope.launch {
            routineDao.updateTask(task.copy(isCompleted = !task.isCompleted))
            
            // Allow DB update to reflect in flow, or just calculate now
            val currentTasks = routineTasks.value.map { if (it.id == task.id) it.copy(isCompleted = !task.isCompleted) else it }
            val total = currentTasks.size
            val completed = currentTasks.count { it.isCompleted }
            
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val today = dateFormat.format(java.util.Date())
            
            val dao = db.routineCompletionDao()
            val existing = dao.getCompletionByDate(today)
            if (existing != null) {
                dao.insertCompletion(existing.copy(totalTasks = total, completedTasks = completed))
            } else {
                dao.insertCompletion(com.example.data.RoutineCompletion(date = today, totalTasks = total, completedTasks = completed))
            }
        }
    }
    
    fun recordAacUsage(phrase: String) {
        viewModelScope.launch {
            aacDao.insertUsage(AacUsage(phrase = phrase))
        }
    }
    
    fun simulateEnvironment(sound: Float, light: Float) {
        useRealSensors.value = false
        currentSoundLevel.value = sound
        currentLightLevel.value = light
        checkEnvironmentState()
    }
    
    @SuppressLint("MissingPermission")
    fun sendAlertToCaregiver() {
        val baseMessage = "EMERGENCY: I need help immediately. Please contact me."
        val context = getApplication<Application>()
        val phone = "+918608957527"
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        val launchWhatsApp = { messageWithLocation: String ->
            try {
                val url = "https://api.whatsapp.com/send?phone=$phone&text=${android.net.Uri.encode(messageWithLocation)}"
                val waIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(waIntent)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Opening WhatsApp...", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Simulating WhatsApp (App Not Found)", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: android.location.Location? ->
                    val locStr = location?.let { " My location: https://maps.google.com/?q=${it.latitude},${it.longitude}" } ?: ""
                    launchWhatsApp(baseMessage + locStr)
                }.addOnFailureListener {
                    launchWhatsApp(baseMessage)
                }
        } else {
            launchWhatsApp(baseMessage)
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyCaregiver(reason: String) {
        val baseMessage = "Hello, just letting you know: $reason. I might need some support soon."
        sendCombinedAlert(baseMessage)
    }

    @SuppressLint("MissingPermission")
    private fun sendCombinedAlert(baseMessage: String) {
        val context = getApplication<Application>()
        val phone = "+918608957527" // Using requested number
        
        // 1. Launch SMS Intent (Info Only)
        try {
            val smsUri = android.net.Uri.parse("smsto:$phone")
            val smsIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", baseMessage)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fetch Location and Launch WhatsApp Intent (Info + Location)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        val launchWhatsApp = { messageWithLocation: String ->
            try {
                val url = "https://api.whatsapp.com/send?phone=$phone&text=${android.net.Uri.encode(messageWithLocation)}"
                val waIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(waIntent)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Opening SMS and WhatsApp...", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Simulating WhatsApp/SMS (App Not Found)", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: android.location.Location? ->
                    val locStr = location?.let { " My location: https://maps.google.com/?q=${it.latitude},${it.longitude}" } ?: ""
                    launchWhatsApp(baseMessage + locStr)
                }.addOnFailureListener {
                    launchWhatsApp(baseMessage)
                }
        } else {
            launchWhatsApp(baseMessage)
        }
    }

    val pendingAiRoutine = MutableStateFlow<List<RoutineTask>?>(null)

    fun generateRoutineWithAi(promptRequest: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val prompt = "Create a short, structured daily routine for an autistic individual based on this request: '$promptRequest'. Return ONLY a raw JSON array. Do not include markdown blocks like ```json. Each object must have three keys: 'name' (a short 1-3 word task name), 'icon' (a single emoji representing the task), and 'time' (a string in HH:MM format)."
                val response = askGemini(prompt, "You are a helpful assistant.")
                
                // Extract JSON array using regex
                val matchResult = "\\[.*\\]".toRegex(RegexOption.DOT_MATCHES_ALL).find(response)
                val jsonArrayStr = matchResult?.value ?: "[]"
                
                val tasksToInsert = mutableListOf<RoutineTask>()
                
                try {
                    val jsonArray = org.json.JSONArray(jsonArrayStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        tasksToInsert.add(RoutineTask(
                            name = obj.getString("name"),
                            icon = obj.getString("icon"),
                            time = obj.getString("time"),
                            orderIndex = i
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (tasksToInsert.isNotEmpty()) {
                    pendingAiRoutine.value = tasksToInsert
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }

    fun approveAiRoutine() {
        val tasks = pendingAiRoutine.value ?: return
        viewModelScope.launch {
            routineDao.deleteAllTasks()
            tasks.forEach { routineDao.insertTask(it) }
            pendingAiRoutine.value = null
        }
    }

    fun rejectAiRoutine() {
        pendingAiRoutine.value = null
    }

    val aiSuggestion = MutableStateFlow("")
    
    fun getAiSuggestion(context: String) {
        viewModelScope.launch {
            aiSuggestion.value = "Thinking..."
            val prompt = "Based on this context: '$context', give a short, supportive, non-medical suggestion for someone with sensory sensitivities."
            val response = askGemini(prompt, "You are a helpful assistant for an autistic individual.")
            aiSuggestion.value = response
        }
    }
}
