package com.example

import coil.imageLoader
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput

object AppState {
    var currentChatId: String? = null
    var chatMessagesCache = java.util.concurrent.ConcurrentHashMap<String, List<com.example.ui.MessageModel>>()
}

object NotificationHelper {
    private const val CHANNEL_ID = "new_messages_channel"

    /** Canal des notifications push (abonné, message d'ami, etc.) avec le son personnalisé. */
    const val OWLINO_CHANNEL_ID = "owlino_high_priority_v3"

    fun owlinoSoundUri(context: Context): android.net.Uri =
        android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.sound_notification}")

    fun ensureOwlinoChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val attrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(OWLINO_CHANNEL_ID, "Owlino Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Owlino High Priority Notifications"
                enableVibration(true)
                setShowBadge(true)
                setSound(owlinoSoundUri(context), attrs)
            }
            nm.createNotificationChannel(channel)
            try { nm.deleteNotificationChannel("owlino_high_priority_v2") } catch (e: Exception) {}
        }
    }
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "New Messages"
            val descriptionText = "Notifications for new messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun generateFallbackAvatar(name: String): android.graphics.Bitmap {
        val size = 256
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#34B7F1") // A nice blue
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 120f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val text = if (name.isNotBlank()) name.first().uppercase() else "?"
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val y = (size / 2f) + (textBounds.height() / 2f)
        canvas.drawText(text, size / 2f, y, textPaint)
        
        return bitmap
    }

    fun showNotification(context: Context, chatId: String, messageId: String, senderName: String, messageText: String, avatarUrl: String? = null) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var largeIcon: android.graphics.Bitmap? = null
            if (avatarUrl != null) {
                try {
                    val request = coil.request.ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .size(256)
                        .transformations(coil.transform.CircleCropTransformation())
                        .build()
                    val result = coil.Coil.imageLoader(context).execute(request)
                    if (result is coil.request.SuccessResult) {
                        largeIcon = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    } else {
                    }
                } catch(e: Exception) {
                }
            }
            
            if (largeIcon == null) {
                largeIcon = generateFallbackAvatar(senderName)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("chatId", chatId)
                putExtra("senderName", senderName)
                action = "OPEN_CHAT_${chatId}"
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, chatId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Mark as Read Action
            val readIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_MARK_AS_READ"
                putExtra("chatId", chatId)
                putExtra("messageId", messageId)
            }
            val readPendingIntent = PendingIntent.getBroadcast(
                context, chatId.hashCode() + 1, readIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val readAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_view,
                "Mark as read",
                readPendingIntent
            ).build()

            // Reply Action
            val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_REPLY"
                putExtra("chatId", chatId)
                putExtra("messageId", messageId)
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context, chatId.hashCode() + 2, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val remoteInput = RemoteInput.Builder("key_text_reply")
                .setLabel("Reply...")
                .build()
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Reply",
                replyPendingIntent
            ).addRemoteInput(remoteInput).build()

            val person = androidx.core.app.Person.Builder()
                .setName(senderName)
                .apply {
                    largeIcon?.let {
                        setIcon(androidx.core.graphics.drawable.IconCompat.createWithBitmap(it))
                    }
                }
                .build()

            val messagingStyle = NotificationCompat.MessagingStyle(person)
                .addMessage(messageText, System.currentTimeMillis(), person)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setStyle(messagingStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(replyAction)
                .addAction(readAction)

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        return@launch
                    }
                }
                
                if (!notificationManager.areNotificationsEnabled()) {
                }
                
                notificationManager.notify(chatId.hashCode(), builder.build())
            } catch (e: SecurityException) {
            } catch (e: Exception) {
            }
        }
    }
}
