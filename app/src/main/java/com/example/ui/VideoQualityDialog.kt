package com.example.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.util.VideoQuality

/** يظهر عند إرسال فيديو: اختيار الجودة (عادي أسرع وأصغر، عالٍ أوضح وأكبر). */
@Composable
fun VideoQualityDialog(
    onPick: (VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("جودة الفيديو") },
        text = {
            Text(
                "عادي (720p): أسرع وأصغر حجمًا، يناسب أغلب الفيديوهات.\n\n" +
                    "عالٍ (1080p): أوضح لكن حجمه ضعف تقريبًا، وقد لا يُقبل الفيديو الطويل (الحد 50 ميغابايت)."
            )
        },
        confirmButton = {
            TextButton(onClick = { onPick(VideoQuality.HIGH) }) { Text("عالٍ") }
        },
        dismissButton = {
            TextButton(onClick = { onPick(VideoQuality.NORMAL) }) { Text("عادي") }
        }
    )
}
