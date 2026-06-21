package com.craigl.smartfolders

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.craigl.smartfolders.data.FolderCategory
import com.craigl.smartfolders.ui.theme.SmartFoldersTheme
import com.craigl.smartfolders.widget.SmartFolderWidgetReceiver

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(60)
        }
        setContent {
            SmartFoldersTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Smart Folders") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CategoryList(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val categories = FolderCategory.entries

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Tap a category to add a widget to your home screen.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(categories) { category ->
            CategoryItem(category = category) {
                pinWidget(context, category)
                // Close app and return to launcher to see the pinning dialog
                (context as? android.app.Activity)?.finish()
            }
        }
    }
}

@Composable
fun CategoryItem(category: FolderCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = category.getDisplayName(LocalContext.current), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = category.getSubtitle(LocalContext.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Widget")
        }
    }
}

private fun pinWidget(context: android.content.Context, category: FolderCategory) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val myProvider = ComponentName(context, SmartFolderWidgetReceiver::class.java)

    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val successIntent = Intent(context, SmartFolderWidgetReceiver::class.java).apply {
            action = "ACTION_WIDGET_PINNED"
            putExtra("category", category.name)
        }
        
        val successCallback = PendingIntent.getBroadcast(
            context,
            category.ordinal,
            successIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val extras = Bundle().apply {
            putString("pinned_category", category.name)
        }

        appWidgetManager.requestPinAppWidget(myProvider, extras, successCallback)
    }
}
