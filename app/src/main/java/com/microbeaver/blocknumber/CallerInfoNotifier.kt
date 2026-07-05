package com.microbeaver.blocknumber

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Shows silent notifications with caller info while the phone is ringing
 * (for allowed calls) and after a call has been blocked.
 */
object CallerInfoNotifier {

    private const val CHANNEL_CALLER_INFO = "caller_info"
    private const val CHANNEL_BLOCKED = "blocked_calls"
    private const val ID_CALLER_INFO = 1001

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CALLER_INFO,
                context.getString(R.string.channel_caller_info),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // never compete with the ringtone
                enableVibration(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BLOCKED,
                context.getString(R.string.channel_blocked),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun truecallerAction(
        context: Context,
        number: String
    ): NotificationCompat.Action? {
        if (number.none { it.isDigit() }) return null
        val pi = PendingIntent.getActivity(
            context, number.hashCode(),
            Truecaller.searchIntent(context, number),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(
            R.drawable.ic_info,
            context.getString(R.string.truecaller_search),
            pi
        )
    }

    /** Caller info while ringing: contact/spam/unknown summary. */
    fun showCallerInfo(context: Context, number: String, spam: SpamEntry?) {
        if (!canNotify(context)) return
        ensureChannels(context)

        val title: String
        val text: String
        val icon: Int
        if (spam != null) {
            title = spam.name.ifEmpty {
                if (spam.isNegative) context.getString(R.string.suspected_spam)
                else context.getString(R.string.known_number)
            }
            val cat = spam.category.ifEmpty { context.getString(R.string.uncategorized) }
            text = context.getString(
                R.string.caller_info_summary, cat, spam.negative, spam.positive
            )
            icon = if (spam.isNegative) R.drawable.ic_warning else R.drawable.ic_info
        } else {
            title = context.getString(R.string.unknown_caller)
            text = number
            icon = R.drawable.ic_info
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_CALLER_INFO)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(if (spam != null) "$number — $text" else text)
            .setContentIntent(contentIntent(context))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
        truecallerAction(context, number)?.let { builder.addAction(it) }
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(ID_CALLER_INFO, notification)
    }

    /** Silent notice that a call was blocked. */
    fun showBlocked(context: Context, number: String, reason: String) {
        if (!canNotify(context)) return
        ensureChannels(context)

        val reasonText = when (reason) {
            BlockedCallStore.REASON_BLACKLIST -> context.getString(R.string.reason_blacklist)
            BlockedCallStore.REASON_HIDDEN -> context.getString(R.string.reason_hidden)
            BlockedCallStore.REASON_SPAM -> context.getString(R.string.reason_spam)
            else -> context.getString(R.string.reason_not_in_contacts)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_block)
            .setContentTitle(context.getString(R.string.blocked_call_title, number))
            .setContentText(reasonText)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
        truecallerAction(context, number)?.let { builder.addAction(it) }
        val notification = builder.build()
        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % 100000).toInt() + 2000, notification)
    }
}
