package com.example.deepwork4dx.presentation

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeepWorkApp()
        }
    }
}

@Composable
fun DeepWorkApp() {
    val brightColorScheme = ColorScheme(
        primary = Color(0xFF0066CC),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3F2FD),
        onPrimaryContainer = Color(0xFF003366),
        secondary = Color(0xFFE0E0E0),
        onSecondary = Color(0xFF212121),
        background = Color(0xFFF5F5F5),
        onBackground = Color(0xFF121212),
        surfaceContainer = Color(0xFFFFFFFF),
        onSurface = Color(0xFF121212),
        error = Color(0xFFD32F2F),
        onError = Color.White
    )

    MaterialTheme(colorScheme = brightColorScheme) {
        AppScaffold {
            ScreenScaffold(
                timeText = { TimeText() }
            ) {
                DeepWorkTrackerScreen()
            }
        }
    }
}

@Composable
fun DeepWorkTrackerScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("DeepWorkPrefs", Context.MODE_PRIVATE) }

    var isRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    // Load initial persistent state
    val categories = remember { mutableStateListOf<String>().apply { addAll(loadCategories(prefs)) } }
    val taskTimeLogs = remember { mutableStateMapOf<String, Long>().apply { putAll(loadTimeLogs(prefs)) } }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var isEditMode by remember { mutableStateOf(false) }

    val listState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val totalLoggedSeconds = taskTimeLogs.values.sum()
    val currentTaskName = categories.getOrNull(selectedCategoryIndex) ?: "Deep Work"

    // Safe Speech/Keyboard Input Launcher
    val inputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            val remoteInputText = RemoteInput.getResultsFromIntent(result.data)?.getCharSequence("extra_task_name")?.toString()

            val taskName = spokenText ?: remoteInputText
            if (!taskName.isNullOrBlank()) {
                val cleanedName = taskName.trim()
                if (!categories.contains(cleanedName)) {
                    categories.add(cleanedName)
                    saveCategories(prefs, categories)
                }
            }
        }
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .onRotaryScrollEvent {
                coroutineScope.launch {
                    listState.scrollBy(it.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. 4DX Lead Measure Overall Scoreboard
        item {
            Card(
                onClick = { },
                enabled = false,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(bottom = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "4DX LEAD MEASURE",
                        style = TextStyle(fontSize = 10.sp, color = Color(0xFF666666), fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatHours(totalLoggedSeconds + elapsedSeconds)} / 4.0 Hrs",
                        style = TextStyle(fontSize = 15.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }

        // 2. Active Session Display
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTaskName,
                    style = TextStyle(fontSize = 11.sp, color = Color(0xFF0066CC), fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = formatTime(elapsedSeconds),
                    style = TextStyle(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) Color(0xFF008822) else Color(0xFF111111),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // 3. Primary Start/Pause Control Button
        item {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFD32F2F) else Color(0xFF0066CC),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = if (isRunning) "PAUSE" else "START WORK",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                )
            }
        }

        // 4. Commit Time Log to Storage
        item {
            if (elapsedSeconds > 0) {
                CompactButton(
                    onClick = {
                        val currentLog = taskTimeLogs[currentTaskName] ?: 0L
                        val newTotal = currentLog + elapsedSeconds
                        taskTimeLogs[currentTaskName] = newTotal
                        saveTimeLogs(prefs, taskTimeLogs)

                        elapsedSeconds = 0L
                        isRunning = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(text = "COMMIT TO $currentTaskName", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. Tasks Management Section Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TASK LOGS",
                    style = TextStyle(fontSize = 10.sp, color = Color(0xFF555555), fontWeight = FontWeight.Bold)
                )
                CompactButton(
                    onClick = { isEditMode = !isEditMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode) Color(0xFFFFCC00) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = if (isEditMode) "DONE" else "EDIT",
                        fontSize = 9.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Dynamic Editable Task Items
        itemsIndexed(categories) { index, task ->
            val isSelected = (index == selectedCategoryIndex)
            val taskTime = taskTimeLogs[task] ?: 0L

            Button(
                onClick = {
                    if (isEditMode) {
                        if (categories.size > 1) {
                            val removedTask = categories[index]
                            categories.removeAt(index)
                            taskTimeLogs.remove(removedTask)

                            saveCategories(prefs, categories)
                            saveTimeLogs(prefs, taskTimeLogs)

                            if (selectedCategoryIndex >= categories.size) {
                                selectedCategoryIndex = 0
                            }
                        }
                    } else if (!isRunning) {
                        selectedCategoryIndex = index
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isEditMode -> Color(0xFFFFEBEE)
                        isSelected -> Color(0xFF0066CC)
                        else -> Color.White
                    },
                    contentColor = when {
                        isEditMode -> Color(0xFFC62828)
                        isSelected -> Color.White
                        else -> Color(0xFF222222)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "🗑 $task" else if (isSelected) "✓ $task" else task,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                    Text(
                        text = formatTimeCompact(taskTime),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF2E7D32)
                        )
                    )
                }
            }
        }

        // 7. Add Task Button
        item {
            CompactButton(
                onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "New Task")
                        }
                        inputLauncher.launch(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "+ ADD TASK",
                    fontSize = 10.sp,
                    color = Color(0xFF003366),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

// --- Persistence Helpers (SharedPreferences + JSON) ---
private fun saveCategories(prefs: SharedPreferences, categories: List<String>) {
    val jsonArray = JSONArray(categories)
    prefs.edit().putString("categories_list", jsonArray.toString()).apply()
}

private fun loadCategories(prefs: SharedPreferences): List<String> {
    val jsonString = prefs.getString("categories_list", null) ?: return listOf("Deep Work", "Planning", "Admin", "Break")
    val list = mutableListOf<String>()
    val jsonArray = JSONArray(jsonString)
    for (i in 0 until jsonArray.length()) {
        list.add(jsonArray.getString(i))
    }
    return list
}

private fun saveTimeLogs(prefs: SharedPreferences, timeLogs: Map<String, Long>) {
    val jsonObject = JSONObject(timeLogs)
    prefs.edit().putString("time_logs", jsonObject.toString()).apply()
}

private fun loadTimeLogs(prefs: SharedPreferences): Map<String, Long> {
    val jsonString = prefs.getString("time_logs", null) ?: return emptyMap()
    val map = mutableMapOf<String, Long>()
    val jsonObject = JSONObject(jsonString)
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = jsonObject.getLong(key)
    }
    return map
}

// --- Formatting Helpers ---
private fun formatTime(seconds: Long): String {
    val hrs = TimeUnit.SECONDS.toHours(seconds)
    val mins = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

private fun formatTimeCompact(seconds: Long): String {
    val hrs = TimeUnit.SECONDS.toHours(seconds)
    val mins = TimeUnit.SECONDS.toMinutes(seconds) % 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m"
        mins > 0 -> "${mins}m"
        seconds > 0 -> "${seconds}s"
        else -> "0m"
    }
}

private fun formatHours(seconds: Long): String {
    val hours = seconds.toDouble() / 3600.0
    return String.format("%.1f", hours)
}