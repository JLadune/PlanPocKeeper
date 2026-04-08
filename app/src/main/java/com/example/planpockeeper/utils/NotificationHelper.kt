package com.example.planpockeeper.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.planpockeeper.R
import com.example.planpockeeper.data.model.BudgetSummary
import java.text.SimpleDateFormat
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "budget_period_end"
    private const val CHANNEL_NAME = "Fin de période budget"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications de fin de période budgétaire"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun sendPeriodEndNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            )
            .setContentTitle("Récapitulatif budgétaire à venir")
            .setContentText(
                "Votre période budgétaire touche à sa fin. " +
                        "Un email récapitulatif vous sera envoyé et vos données seront réinitialisées pour la prochaine période."
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Votre période budgétaire touche à sa fin. " +
                        "Un email récapitulatif vous sera envoyé et vos données seront réinitialisées pour la prochaine période."
                    )
            )
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(1001, notification)
    }

    fun sendNoExpenseReminderNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            )
            .setContentTitle("Vous n'avez pas saisi de dépense récemment")
            .setContentText("Aucune dépense enregistrée depuis 3 jours.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Vous n'avez pas enregistré de dépense depuis 3 jours. " +
                                "Est-ce normal ? Pensez à tenir votre budget à jour."
                    )
            )
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(1002, notification)
    }


}