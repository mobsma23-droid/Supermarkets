package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.CatalogRepository
import com.example.util.CatalogSyncManager
import com.example.util.SpreadsheetImporter

class CatalogSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "CatalogSyncWorker"
        const val CHANNEL_ID = "catalog_sync_periodic_channel"
        const val NOTIFICATION_ID = 2001
    }

    private val periodicDriveFiles = listOf(
        "https://drive.google.com/uc?export=download&id=dreamprice_file_001",
        "https://drive.google.com/uc?export=download&id=intermart_file_002",
        "https://drive.google.com/uc?export=download&id=superu_file_003",
        "https://drive.google.com/uc?export=download&id=winners_file_004",
        "https://drive.google.com/uc?export=download&id=jumbo_file_005",
        "https://drive.google.com/uc?export=download&id=carrefour_file_006",
        "https://drive.google.com/uc?export=download&id=kingsavers_file_007"
    )

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic background catalog sync...")

        return try {
            val database = AppDatabase.getDatabase(appContext)
            val repository = CatalogRepository(database.catalogDao())

            CatalogSyncManager.updateProgress(
                isRunning = true,
                message = "Synchronisation périodique des catalogues...",
                current = 0,
                total = periodicDriveFiles.size
            )

            var importedCount = 0
            val storesUpdated = mutableSetOf<String>()

            periodicDriveFiles.forEachIndexed { index, fileUrl ->
                try {
                    val result = SpreadsheetImporter.downloadAndParse(fileUrl) { _ -> }
                    if (result.products.isNotEmpty()) {
                        repository.importProducts(result.products)
                        importedCount += result.products.size
                        storesUpdated.add(result.targetCatalog)
                    }
                    CatalogSyncManager.updateProgress(
                        isRunning = true,
                        message = "Mise à jour en arrière-plan (${index + 1}/${periodicDriveFiles.size})...",
                        current = index + 1,
                        total = periodicDriveFiles.size
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed syncing file: $fileUrl", e)
                }
            }

            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val summary = "Mise à jour automatique terminée : $importedCount produits rafraîchis pour ${storesUpdated.size} enseignes ($timeStr)."

            CatalogSyncManager.updateProgress(
                isRunning = false,
                message = "Synchronisation en arrière-plan réussie",
                current = periodicDriveFiles.size,
                total = periodicDriveFiles.size,
                summary = summary
            )

            if (importedCount > 0) {
                showSyncNotification(
                    title = "Catalogues à jour",
                    message = "$importedCount produits synchronisés automatiquement (${storesUpdated.joinToString(", ")})."
                )
            }

            Log.d(TAG, "Periodic catalog sync finished successfully with $importedCount items.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic catalog sync failed", e)
            CatalogSyncManager.updateProgress(
                isRunning = false,
                message = "Échec de la synchronisation en arrière-plan",
                isError = true
            )
            Result.retry()
        }
    }

    private fun showSyncNotification(title: String, message: String) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mises à jour automatiques des Catalogues",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications des synchronisations d'arrière-plan"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
