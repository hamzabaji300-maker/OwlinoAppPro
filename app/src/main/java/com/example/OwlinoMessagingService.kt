package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.net.URL
import kotlin.math.min

class OwlinoMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // استخراج البيانات بشكل آمن سواء جاءت في الـ data أو الـ notification
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "رسالة جديدة"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
        val avatarUrl = remoteMessage.data["avatarUrl"] ?: remoteMessage.data["avatar_url"]
        val chatId = remoteMessage.data["chatId"] ?: remoteMessage.data["chat_id"] ?: ""
        val messageId = remoteMessage.data["messageId"] ?: remoteMessage.data["message_id"] ?: ""
        
        val notifType = remoteMessage.data["notif_type"] ?: ""
        val profileId = remoteMessage.data["profile_id"] ?: ""
        
        // تشغيل معالجة الإشعار في الـ Background Thread (Dispatchers.IO)
        CoroutineScope(Dispatchers.IO).launch {
            sendNotification(title, body, avatarUrl, chatId, messageId, notifType, profileId)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Update token in Supabase
        updateTokenInSupabase(token)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            (bitmap.width + size) / 2,
            (bitmap.height + size) / 2
        )
        val destRect = Rect(0, 0, size, size)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = -0xbdbdbe // Arbitrary color for drawing the initial circle mask
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, destRect, paint)

        return output
    }

    private suspend fun sendNotification(title: String, messageBody: String, avatarUrl: String?, chatId: String, messageId: String, notifType: String, profileId: String) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (notifType == "follow" && profileId.isNotEmpty()) {
                    putExtra("profileId", profileId)
                    action = "OPEN_PROFILE_$profileId"
                } else if (chatId.isNotEmpty()) {
                    putExtra("chatId", chatId)
                    action = "OPEN_CHAT_$chatId"
                }
            }
            
            val requestCode = if (notifType == "follow" && profileId.isNotEmpty()) profileId.hashCode() else chatId.hashCode()
            val pendingIntent = PendingIntent.getActivity(
                this, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val channelId = "owlino_high_priority_v2"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            // محاولة تحميل الصورة من الرابط (إن وُجد)
            var largeIcon: Bitmap? = null
            if (!avatarUrl.isNullOrBlank()) {
                try {
                    val url = URL(avatarUrl)
                    val originalBitmap = BitmapFactory.decodeStream(url.openStream())
                    largeIcon = originalBitmap?.let { getCircularBitmap(it) }
                } catch (e: Exception) {
                }
            }

            // تجهيز هوية المرسل (Person) لدعم الـ MessagingStyle
            val personBuilder = Person.Builder().setName(title)
            largeIcon?.let {
                personBuilder.setIcon(IconCompat.createWithBitmap(it))
            }
            val person = personBuilder.build()

            val messagingStyle = NotificationCompat.MessagingStyle(person)
                .addMessage(messageBody, System.currentTimeMillis(), person)
            
            val currentUserId = com.example.supabase.auth.currentUserOrNull()?.id

            var replyAction: NotificationCompat.Action? = null
            if (notifType != "follow" && chatId.isNotEmpty()) {
                val replyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                    action = "ACTION_REPLY"
                    putExtra("chatId", chatId)
                    putExtra("messageId", messageId)
                    if (currentUserId != null) {
                        putExtra("senderId", currentUserId)
                    }
                }
                val replyPendingIntent = PendingIntent.getBroadcast(
                    this, chatId.hashCode() + 2, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                val remoteInput = androidx.core.app.RemoteInput.Builder("key_text_reply")
                    .setLabel("Reply...")
                    .build()
                replyAction = NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_send,
                    "Reply",
                    replyPendingIntent
                ).addRemoteInput(remoteInput).build()
            }

            var muteAction: NotificationCompat.Action? = null
            if (notifType != "follow" && chatId.isNotEmpty()) {
                val muteIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                    action = "ACTION_MUTE"
                    putExtra("chatId", chatId)
                    val currentUserId = com.example.supabase.auth.currentUserOrNull()?.id
                    if (currentUserId != null) {
                        putExtra("senderId", currentUserId)
                    }
                }
                val mutePendingIntent = PendingIntent.getBroadcast(
                    this, chatId.hashCode() + 3, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                muteAction = NotificationCompat.Action.Builder(
                    android.R.drawable.ic_lock_silent_mode_off,
                    "Mute",
                    mutePendingIntent
                ).build()
            }

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(messagingStyle)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)

            if (replyAction != null) {
                notificationBuilder.addAction(replyAction)
            }
            if (muteAction != null) {
                notificationBuilder.addAction(muteAction)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Owlino Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Owlino High Priority Notifications"
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notificationId = if (notifType == "follow" && profileId.isNotEmpty()) profileId.hashCode() else if (chatId.isNotEmpty()) chatId.hashCode() else System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())

        } catch (e: Exception) {
        }
    }

    private fun updateTokenInSupabase(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUserId = com.example.supabase.auth.currentUserOrNull()?.id
                if (currentUserId != null) {
                    com.example.supabase.postgrest["profiles"] // check if users table or profiles table
                        .update(mapOf("fcm_token" to token)) {
                            filter { eq("id", currentUserId) }
                        }
                }
            } catch (e: Exception) {
            }
        }
    }
}
