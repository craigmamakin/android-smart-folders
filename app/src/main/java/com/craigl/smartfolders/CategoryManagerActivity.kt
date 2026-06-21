package com.craigl.smartfolders

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.craigl.smartfolders.data.*
import com.craigl.smartfolders.ui.theme.SmartFoldersTheme
import kotlinx.coroutines.launch

class CategoryManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val categoryName = intent.getStringExtra("category") ?: FolderCategory.MONEY.name
        val category = FolderCategory.valueOf(categoryName)

        setContent {
            SmartFoldersTheme {
                CategoryManagerScreen(category) {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(category: FolderCategory, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    val allApps = remember { 
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(intent, 0).map { 
            val info = it.activityInfo.applicationInfo
            AppInfo(info.packageName, info.loadLabel(pm).toString(), info.category)
        }.sortedBy { it.label }
    }

    val overrides by db.appOverrideDao().getAllOverrides().collectAsState(initial = emptyList())
    val config = remember { AppManager.getCategoryInfo(context, category) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage ${category.getDisplayName(context)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search by app name...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val filteredApps = allApps.filter { 
                    it.label.contains(searchQuery, ignoreCase = true)
                }

                items(filteredApps) { app ->
                    val override = overrides.find { it.packageName == app.packageName && it.category == category.name }
                    val isIncluded = override?.isIncluded ?: AppManager.isSmartMatch(app, category, config)

                    ListItem(
                        headlineContent = { 
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium
                            ) 
                        },
                        supportingContent = { 
                            val status = if (override != null) "Manual Override" else "Auto Categorized"
                            Text(status, style = MaterialTheme.typography.labelSmall)
                        },
                        trailingContent = {
                            Checkbox(
                                checked = isIncluded,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        db.appOverrideDao().saveOverride(
                                            AppOverride(app.packageName, category.name, checked)
                                        )
                                    }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
