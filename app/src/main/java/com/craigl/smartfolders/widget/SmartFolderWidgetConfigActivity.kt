package com.craigl.smartfolders.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.craigl.smartfolders.data.FolderCategory
import com.craigl.smartfolders.ui.theme.SmartFoldersTheme
import kotlinx.coroutines.launch

class SmartFolderWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(60)
        }

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        lifecycleScope.launch {
            // Give the system a moment to sync options if pinned from app
            val appWidgetManager = AppWidgetManager.getInstance(this@SmartFolderWidgetConfigActivity)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val pinnedCategory = options.getString("pinned_category")
            
            if (pinnedCategory != null) {
                val category = try {
                    FolderCategory.valueOf(pinnedCategory)
                } catch (e: Exception) {
                    when (pinnedCategory) {
                        "BANKING", "FINANCE" -> FolderCategory.MONEY
                        "MAPS" -> FolderCategory.TRAVEL
                        else -> null
                    }
                }
                if (category != null) {
                    saveConfig(category)
                    return@launch
                }
            }

            setupUI()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setupUI() {
        setContent {
            SmartFoldersTheme {
                val scope = rememberCoroutineScope()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Select Category") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        items(FolderCategory.entries) { category ->
                            ListItem(
                                headlineContent = { Text(category.getDisplayName(this@SmartFolderWidgetConfigActivity)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            saveConfig(category)
                                        }
                                    },
                                colors = androidx.compose.material3.ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun saveConfig(category: FolderCategory) {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        updateAppWidgetState(this, glanceId) { prefs ->
            prefs[SmartFolderWidget.CategoryKey] = category.name
        }
        SmartFolderWidget().update(this, glanceId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
