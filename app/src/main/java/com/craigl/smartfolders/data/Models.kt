package com.craigl.smartfolders.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class FolderCategory {
    MONEY, SOCIAL, PRODUCTIVITY, TRAVEL, GAMES, GOOGLE, ENTERTAINMENT, SHOPPING, NEWS, UTILITIES, SPORTS, HEALTH, FOOD, LIFESTYLE;

    fun getDisplayName(context: Context): String {
        return AppManager.getCategoryInfo(context, this)?.displayName ?: name.lowercase().replaceFirstChar { it.uppercase() }
    }

    fun getShortLabel(context: Context): String {
        return AppManager.getCategoryInfo(context, this)?.shortLabel ?: name.lowercase().replaceFirstChar { it.uppercase() }
    }

    fun getSubtitle(context: Context): String {
        return AppManager.getCategoryInfo(context, this)?.subtitle ?: "Smart categorized folder"
    }
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val category: Int
)

@Serializable
data class CategoryConfig(
    val displayName: String,
    val shortLabel: String,
    val subtitle: String,
    val keywords: List<String>,
    val packagePrefixes: List<String> = emptyList(),
    val packageIds: List<String> = emptyList(),
    val excludedPackages: List<String> = emptyList()
)

@Serializable
data class CategoryData(
    val MONEY: CategoryConfig,
    val SOCIAL: CategoryConfig,
    val PRODUCTIVITY: CategoryConfig,
    val TRAVEL: CategoryConfig,
    val GAMES: CategoryConfig,
    val GOOGLE: CategoryConfig,
    val ENTERTAINMENT: CategoryConfig,
    val SHOPPING: CategoryConfig,
    val NEWS: CategoryConfig,
    val UTILITIES: CategoryConfig,
    val SPORTS: CategoryConfig,
    val HEALTH: CategoryConfig,
    val FOOD: CategoryConfig,
    val LIFESTYLE: CategoryConfig
)

object AppManager {
    private var categoryKeywords: CategoryData? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun getCategoryInfo(context: Context, category: FolderCategory): CategoryConfig? {
        val data = loadKeywords(context) ?: return null
        return when (category) {
            FolderCategory.MONEY -> data.MONEY
            FolderCategory.SOCIAL -> data.SOCIAL
            FolderCategory.PRODUCTIVITY -> data.PRODUCTIVITY
            FolderCategory.TRAVEL -> data.TRAVEL
            FolderCategory.GAMES -> data.GAMES
            FolderCategory.GOOGLE -> data.GOOGLE
            FolderCategory.ENTERTAINMENT -> data.ENTERTAINMENT
            FolderCategory.SHOPPING -> data.SHOPPING
            FolderCategory.NEWS -> data.NEWS
            FolderCategory.UTILITIES -> data.UTILITIES
            FolderCategory.SPORTS -> data.SPORTS
            FolderCategory.HEALTH -> data.HEALTH
            FolderCategory.FOOD -> data.FOOD
            FolderCategory.LIFESTYLE -> data.LIFESTYLE
        }
    }

    private fun loadKeywords(context: Context): CategoryData? {
        if (categoryKeywords != null) return categoryKeywords
        
        return try {
            val jsonString = context.assets.open("category_keywords.json").bufferedReader().use { it.readText() }
            categoryKeywords = json.decodeFromString<CategoryData>(jsonString)
            categoryKeywords
        } catch (e: Exception) {
            Log.e("SmartFolders", "CRITICAL: Failed to load keywords JSON", e)
            null
        }
    }

    fun getAppsForCategory(context: Context, category: FolderCategory): List<AppInfo> {
        val pm = context.packageManager
        
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            Log.e("SmartFolders", "Failed to query activities", e)
            emptyList()
        }

        val config = getCategoryInfo(context, category)

        val apps = resolveInfos.map { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            AppInfo(
                packageName = appInfo.packageName,
                label = resolveInfo.loadLabel(pm).toString(),
                category = appInfo.category
            )
        }.filter { app ->
            isSmartMatch(app, category, config)
        }.distinctBy { it.packageName }

        Log.d("SmartFolders", "Found ${apps.size} apps for category ${category.name}")
        return apps
    }

    fun isSmartMatch(app: AppInfo, category: FolderCategory, config: CategoryConfig?): Boolean {
        if (config == null) return false
        
        val label = app.label.lowercase()
        val packageName = app.packageName.lowercase()

        // 1. Explicit Exclusions
        if (config.excludedPackages.any { packageName == it || packageName.startsWith("$it.") }) {
            return false
        }

        // 2. Global Game Exception
        if (app.category == ApplicationInfo.CATEGORY_GAME) {
            return category == FolderCategory.GAMES
        }

        // 3. Explicit Package ID Matching
        if (config.packageIds.contains(packageName)) {
            return true
        }

        // 4. Package Prefix Matching
        if (config.packagePrefixes.any { packageName.startsWith(it) }) {
            return true
        }

        // 5. System Category Matching
        val matchesSystemCategory = when (category) {
            FolderCategory.SOCIAL -> app.category == ApplicationInfo.CATEGORY_SOCIAL
            FolderCategory.PRODUCTIVITY -> app.category == ApplicationInfo.CATEGORY_PRODUCTIVITY
            FolderCategory.TRAVEL -> app.category == ApplicationInfo.CATEGORY_MAPS
            FolderCategory.ENTERTAINMENT -> app.category == ApplicationInfo.CATEGORY_VIDEO || app.category == ApplicationInfo.CATEGORY_AUDIO
            FolderCategory.NEWS -> app.category == ApplicationInfo.CATEGORY_NEWS
            else -> false
        }
        if (matchesSystemCategory) return true

        // 6. Robust Keyword Matching (Whole words only)
        return config.keywords.any { keyword ->
            val regex = Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
            regex.containsMatchIn(label) || regex.containsMatchIn(packageName.replace(".", " ").replace("_", " "))
        }
    }
}
