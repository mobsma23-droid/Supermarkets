package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.service.CatalogDownloadService
import com.example.service.CatalogSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

data class SyncProgressState(
    val isRunning: Boolean = false,
    val progressMessage: String = "",
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val lastSyncTimeFormatted: String = "",
    val lastSyncTimestamp: Long = 0L,
    val lastSummary: String? = null,
    val isError: Boolean = false
)

object CatalogSyncManager {

    private const val PREFS_NAME = "catalog_sync_prefs"
    private const val KEY_PERIODIC_SYNC_ENABLED = "periodic_work_enabled"
    private const val PERIODIC_WORK_TAG = "catalog_periodic_sync_work"

    private val _syncState = MutableStateFlow(SyncProgressState())
    val syncState: StateFlow<SyncProgressState> = _syncState.asStateFlow()

    private val _isPeriodicSyncEnabled = MutableStateFlow(true)
    val isPeriodicSyncEnabled: StateFlow<Boolean> = _isPeriodicSyncEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PERIODIC_SYNC_ENABLED, true)
        _isPeriodicSyncEnabled.value = enabled
        if (enabled) {
            schedulePeriodicSync(context)
        } else {
            cancelPeriodicSync(context)
        }
    }

    fun setPeriodicSyncEnabled(context: Context, enabled: Boolean) {
        _isPeriodicSyncEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERIODIC_SYNC_ENABLED, enabled)
            .apply()

        if (enabled) {
            schedulePeriodicSync(context)
        } else {
            cancelPeriodicSync(context)
        }
    }

    fun updateProgress(
        isRunning: Boolean,
        message: String,
        current: Int = 0,
        total: Int = 0,
        summary: String? = null,
        isError: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))
        _syncState.value = _syncState.value.copy(
            isRunning = isRunning,
            progressMessage = message,
            currentStep = current,
            totalSteps = total,
            lastSummary = summary ?: _syncState.value.lastSummary,
            lastSyncTimestamp = if (!isRunning && !isError && summary != null) now else _syncState.value.lastSyncTimestamp,
            lastSyncTimeFormatted = if (!isRunning && !isError && summary != null) timeStr else _syncState.value.lastSyncTimeFormatted,
            isError = isError
        )
    }

    /**
     * Schedules periodic background sync using WorkManager (every 6 hours when connected to network)
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CatalogSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Cancels the periodic background sync WorkManager job
     */
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_TAG)
    }

    /**
     * Starts the Foreground Service to download all Drive catalog files with live persistent notification
     */
    fun startDownloadAllFiles(context: Context) {
        val intent = Intent(context, CatalogDownloadService::class.java).apply {
            action = CatalogDownloadService.ACTION_DOWNLOAD_ALL
        }
        startService(context, intent)
    }

    /**
     * Starts the Foreground Service to download and import a single Drive file
     */
    fun startDownloadSingleFile(context: Context, fileUrl: String, fileName: String, targetCatalog: String) {
        val intent = Intent(context, CatalogDownloadService::class.java).apply {
            action = CatalogDownloadService.ACTION_DOWNLOAD_FILE
            putExtra(CatalogDownloadService.EXTRA_FILE_URL, fileUrl)
            putExtra(CatalogDownloadService.EXTRA_FILE_NAME, fileName)
            putExtra(CatalogDownloadService.EXTRA_TARGET_CATALOG, targetCatalog)
        }
        startService(context, intent)
    }

    /**
     * Cancels any active download or sync task
     */
    fun cancelSync(context: Context) {
        val intent = Intent(context, CatalogDownloadService::class.java).apply {
            action = CatalogDownloadService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    private fun startService(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("CatalogSyncManager", "Failed to start service", e)
        }
    }
}
