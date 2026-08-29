package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.example.data.AppDatabase
import com.example.data.CatalogRepository
import com.example.ui.AppThemeMode
import com.example.ui.CatalogManagerNavHost
import com.example.ui.CatalogViewModel
import com.example.ui.CatalogViewModelFactory
import com.example.ui.FontStyleOption
import com.example.ui.theme.CatalogManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                val initialized = try {
                    FirebaseApp.initializeApp(applicationContext)
                } catch (e: Exception) {
                    null
                }
                if (initialized == null) {
                    try {
                        val options = com.google.firebase.FirebaseOptions.Builder()
                            .setApplicationId("1:14577422115:android:shoppingcartcatalog")
                            .setProjectId("shopping-cart-80d07")
                            .setApiKey("AIzaSyDummyKeyForLocalFallbackCatalogApp")
                            .build()
                        FirebaseApp.initializeApp(applicationContext, options)
                    } catch (e: Throwable) {
                        android.util.Log.w("MainActivity", "Firebase options init fallback: ${e.message}")
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "FirebaseApp init skipped or failed: ${e.message}")
        }
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = CatalogRepository(database.catalogDao())
        val factory = CatalogViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[CatalogViewModel::class.java]

        // Initialize background catalog sync via WorkManager according to user preferences
        try {
            com.example.util.CatalogSyncManager.init(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "CatalogSyncManager init skipped: ${e.message}")
        }

        // Request notification permission on Android 13+ (API 33+)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Notification permission check error: ${e.message}")
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val primaryTheme by viewModel.primaryColorTheme.collectAsState()
            val fontStyleOpt by viewModel.fontStyleOption.collectAsState()
            val fontScaleOpt by viewModel.fontScaleOption.collectAsState()
            val cardShapeOpt by viewModel.cardShapeOption.collectAsState()

            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            val fontFamily = when (fontStyleOpt) {
                FontStyleOption.SANS_SERIF -> FontFamily.SansSerif
                FontStyleOption.SERIF -> FontFamily.Serif
                FontStyleOption.MONOSPACE -> FontFamily.Monospace
            }

            CatalogManagerTheme(
                darkTheme = isDarkTheme,
                primaryColor = Color(primaryTheme.primaryColorHex),
                primaryContainerColor = Color(primaryTheme.containerHex),
                fontFamily = fontFamily,
                fontScaleMultiplier = fontScaleOpt.multiplier,
                cornerRadiusDp = cardShapeOpt.cornerRadiusDp
            ) {
                CatalogManagerNavHost(viewModel = viewModel)
            }
        }
    }
}

