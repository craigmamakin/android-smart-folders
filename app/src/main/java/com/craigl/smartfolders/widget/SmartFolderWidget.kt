package com.craigl.smartfolders.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.craigl.smartfolders.FolderExpandedActivity
import com.craigl.smartfolders.R
import com.craigl.smartfolders.data.AppManager
import com.craigl.smartfolders.data.FolderCategory

class SmartFolderWidget : GlanceAppWidget() {

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val categoryName = prefs[CategoryKey] ?: FolderCategory.MONEY.name
            val category = try {
                FolderCategory.valueOf(categoryName)
            } catch (e: Exception) {
                when (categoryName) {
                    "BANKING", "FINANCE" -> FolderCategory.MONEY
                    "MAPS" -> FolderCategory.TRAVEL
                    else -> FolderCategory.MONEY
                }
            }

            // Fetch up to 7 apps for a circular 2-3-2 staggered preview
            val apps = AppManager.getAppsForCategory(LocalContext.current, category).take(7)
            val accentColor = getAccentColor(category)

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ROW 1: Categorical Folder Circle
                // Nesting layers inside a Box to ensure they stack correctly on all launchers
                Box(
                    modifier = GlanceModifier
                        .size(64.dp)
                        .clickable(actionStartActivity(
                            Intent(LocalContext.current, FolderExpandedActivity::class.java).apply {
                                putExtra("category", category.name)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )),
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER 1: The Translucent Category Color Base
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(32.dp)
                            .background(accentColor.copy(alpha = 0.60f))
                    ) {}

                    // LAYER 2: The Glossy Glass Polish
                    Image(
                        provider = ImageProvider(R.drawable.folder_bg),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize()
                    )

                    // LAYER 3: The App Icons
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(LocalContext.current, apps.getOrNull(0))
                            AppIcon(LocalContext.current, apps.getOrNull(1))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(LocalContext.current, apps.getOrNull(2))
                            AppIcon(LocalContext.current, apps.getOrNull(3))
                            AppIcon(LocalContext.current, apps.getOrNull(4))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(LocalContext.current, apps.getOrNull(5))
                            AppIcon(LocalContext.current, apps.getOrNull(6))
                        }
                    }
                }

                // ROW 2: High-Contrast Accessibility Label
                Box(
                    modifier = GlanceModifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Shadow Layer
                    Text(
                        text = category.getShortLabel(LocalContext.current),
                        maxLines = 1,
                        style = TextStyle(
                            color = object : ColorProvider {
                                override fun getColor(context: Context): Color = Color.Black.copy(alpha = 0.6f)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.padding(top = 1.dp, start = 1.dp)
                    )
                    // Primary Label Layer
                    Text(
                        text = category.getShortLabel(LocalContext.current),
                        maxLines = 1,
                        style = TextStyle(
                            color = object : ColorProvider {
                                override fun getColor(context: Context): Color = Color.White
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }

    private fun getAccentColor(category: FolderCategory): Color {
        return when (category) {
            FolderCategory.MONEY -> Color(0xFF00C853) // Neon Green
            FolderCategory.SOCIAL -> Color(0xFF0091EA) // Neon Blue
            FolderCategory.PRODUCTIVITY -> Color(0xFFFFAB00) // Neon Amber
            FolderCategory.TRAVEL -> Color(0xFF00B8D4) // Neon Cyan
            FolderCategory.GAMES -> Color(0xFFAA00FF) // Neon Purple
            FolderCategory.GOOGLE -> Color(0xFF2979FF) // Google Blue
            FolderCategory.ENTERTAINMENT -> Color(0xFFFF1744) // Neon Red
            FolderCategory.SHOPPING -> Color(0xFFFF6D00) // Neon Orange
            FolderCategory.NEWS -> Color(0xFF37474F) // Solid Grey
            FolderCategory.UTILITIES -> Color(0xFF00BFA5) // Neon Teal
            FolderCategory.SPORTS -> Color(0xFF64DD17) // Neon Lime
            FolderCategory.HEALTH -> Color(0xFFFF5252) // Medical Red
            FolderCategory.FOOD -> Color(0xFFBF360C) // Rich Orange
            FolderCategory.LIFESTYLE -> Color(0xFF6200EA) // Deep Purple
        }
    }

    @Composable
    private fun AppIcon(context: Context, app: com.craigl.smartfolders.data.AppInfo?) {
        val modifier = GlanceModifier
            .size(18.dp)
            .padding(1.dp)
            .cornerRadius(4.dp)
        
        if (app != null) {
            val icon = getAppIcon(context, app.packageName)
            if (icon != null) {
                Box(
                    modifier = modifier.background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(icon),
                        contentDescription = app.label,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            } else {
                Box(modifier = modifier.background(Color.Gray.copy(alpha = 0.3f))) {}
            }
        } else {
            Box(modifier = modifier) {}
        }
    }

    private fun getAppIcon(context: Context, packageName: String): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        val CategoryKey = stringPreferencesKey("folder_category")
    }
}
