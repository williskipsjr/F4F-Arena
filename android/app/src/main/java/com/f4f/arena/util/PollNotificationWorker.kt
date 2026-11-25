package com.f4f.arena.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.f4f.arena.MainActivity
import com.f4f.arena.data.api.NetworkClient
import java.util.concurrent.TimeUnit

class PollNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val response = NetworkClient.matchApi.listMatches()
            if (!response.isSuccessful || response.body() == null) {
                return Result.retry()
            }

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val notifiedIds = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

            val openPolls = response.body()!!.data.filter { it.isPollOpen }
            var added = false
            for (match in openPolls) {
                if (!notifiedIds.contains(match.id)) {
                    showNotification(
                        matchId = match.id,
                        title = match.title,
                        kickoff = match.kickoffTime,
                        formatLabel = match.format.label
                    )
                    notifiedIds.add(match.id)
                    added = true
                }
            }

            if (added) {
                prefs.edit().putStringSet(KEY_NOTIFIED_IDS, notifiedIds).apply()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(matchId: String, title: String, kickoff: String?, formatLabel: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Match Polls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when an admin opens a new F4F match poll"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            matchId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = if (!kickoff.isNullOrBlank()) " at $kickoff" else ""
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚽ Match Poll Open: $title")
            .setContentText("Kickoff$timeText ($formatLabel) — Tap to vote IN or OUT!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(matchId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "f4f_match_polls"
        private const val PREFS_NAME = "f4f_notified_polls"
        private const val KEY_NOTIFIED_IDS = "notified_match_ids"
        private const val PERIODIC_WORK_NAME = "f4f_poll_check_periodic"
        private const val IMMEDIATE_WORK_NAME = "f4f_poll_check_now"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodic = PeriodicWorkRequestBuilder<PollNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
            )

            val immediate = OneTimeWorkRequestBuilder<PollNotificationWorker>()
                .setConstraints(constraints)
                .build()
            wm.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediate
            )
        }
    }
}
