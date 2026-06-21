package com.craigl.smartfolders.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SmartFolderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartFolderWidget()

    private val scope = MainScope()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_WIDGET_PINNED") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val categoryName = intent.getStringExtra("category")
            
            Log.d("SmartFolder", "Pinned broadcast received: ID=$appWidgetId, Cat=$categoryName")
            
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && categoryName != null) {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[SmartFolderWidget.CategoryKey] = categoryName
                        }
                        glanceAppWidget.update(context, glanceId)
                        Log.d("SmartFolder", "Widget state updated for $appWidgetId to $categoryName")
                    } catch (e: Exception) {
                        Log.e("SmartFolder", "Failed to update pinned widget", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
        super.onReceive(context, intent)
    }
}
