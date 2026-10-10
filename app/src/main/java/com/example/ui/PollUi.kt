package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** معالج التصويت: يوفّره LabMessagesScreen (messageId, الخيارات المختارة الجديدة). */
val LocalPollVote = staticCompositionLocalOf<(String, Set<Int>) -> Unit> { { _, _ -> } }

private fun pollEnded(p: Poll, now: Long) = p.endsAt != null && now >= p.endsAt

// ======================= فقاعة الاستفتاء =======================
/** اسم صاحب المنشور عندما يُعرض الاستفتاء داخل قناة (null = مجموعة/دردشة). */
val LocalPollChannelAuthor = staticCompositionLocalOf<String?> { null }

@Composable
fun PollBubbleContent(msg: MessageModel, isMe: Boolean, textColor: Color, accent: Color) {
    val poll = msg.poll ?: return
    val onVote = LocalPollVote.current
    val channelAuthor = LocalPollChannelAuthor.current
    val screenW = LocalConfiguration.current.screenWidthDp.dp

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    if (poll.endsAt != null) {
        LaunchedEffect(poll.endsAt) {
            while (System.currentTimeMillis() < poll.endsAt) { delay(1000); now = System.currentTimeMillis() }
            now = System.currentTimeMillis()
        }
    }
    val closed = pollEnded(poll, now)
    val voted = poll.myVotes.isNotEmpty()
    val showResults = voted || closed
    val total = poll.options.sumOf { it.votes }
    var pending by remember(msg.id) { mutableStateOf(setOf<Int>()) }

    val fill = if (isMe) Color.White else accent
    val track = textColor.copy(alpha = 0.14f)
    val divider = textColor.copy(alpha = 0.12f)
    val good = Color(0xFF22C55E)
    val bad = Color(0xFFEF4444)

    Column(
        modifier = Modifier.width(screenW * 0.78f).padding(horizontal = 12.dp, vertical = 8.dp).animateContentSize(tween(300, easing = FastOutSlowInEasing))
    ) {
        if (channelAuthor != null) {
            Text(channelAuthor, color = fill, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text(poll.question, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        val sub = buildString {
            append(if (poll.quizCorrect != null) "اختبار سري" else "استفتاء سري")
            if (poll.multiple) append(" · اختر واحدًا أو أكثر")
            if (closed) append(" · مغلق")
            else if (poll.endsAt != null) {
                val sec = ((poll.endsAt - now) / 1000).coerceAtLeast(0)
                append(" · ")
                append(if (sec >= 3600) "${sec / 3600} س" else if (sec >= 60) "${sec / 60} د" else "$sec ث")
            }
        }
        Text(sub, color = textColor.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))

        // انتقال انسحابي ناعم بين وضع التصويت ووضع النتائج
        AnimatedContent(
            targetState = showResults,
            transitionSpec = {
                (fadeIn(tween(320, delayMillis = 60)) + slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 4 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -it / 5 })
            },
            label = "poll_mode"
        ) { results ->
            Column(Modifier.fillMaxWidth()) {
                poll.options.forEachIndexed { i, opt ->
                    val mine = i in poll.myVotes
                    if (!results) {
                        val picked = i in pending
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (poll.multiple) pending = if (picked) pending - i else pending + i
                                    else onVote(msg.id, setOf(i))
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opt.text, color = textColor, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(10.dp))
                            PollCheck(selected = picked, color = fill, ring = textColor.copy(alpha = 0.5f), checkTint = if (isMe) accent else Color.White)
                        }
                        if (i < poll.options.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(divider))
                    } else {
                        val quizState = when {
                            poll.quizCorrect == null -> 0
                            poll.quizCorrect == i -> 1
                            mine -> 2
                            else -> 0
                        }
                        ResultRow(opt.text, opt.votes, total, i, mine, quizState, textColor, fill, track, good, bad)
                    }
                }
            }
        }

        if (!showResults && poll.multiple) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = pending.isNotEmpty()) { onVote(msg.id, pending) }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    "تصويت",
                    color = if (pending.isNotEmpty()) fill else textColor.copy(alpha = 0.4f),
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                )
            }
        }
        if (voted && poll.revoting && !closed) {
            Text(
                "سحب التصويت", color = fill, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp)).clickable { onVote(msg.id, emptySet()) }.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                if (total == 0) "لا توجد أصوات بعد" else "$total صوت",
                color = textColor.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.weight(1f)
            )
            Text(msg.time, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        // منشور القناة: صف التعليقات كما في تيليجرام
        if (channelAuthor != null) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(divider))
            Row(
                Modifier.fillMaxWidth().clickable { }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = fill, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("كتابة تعليق", color = fill, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = fill, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** دائرة التحديد: حلقة فارغة، وعند الاختيار تمتلئ وتظهر علامة صح بحركة ناعمة. */
@Composable
private fun PollCheck(selected: Boolean, color: Color, ring: Color, checkTint: Color) {
    val bg by animateColorAsState(if (selected) color else Color.Transparent, tween(200), label = "poll_check_bg")
    val ringColor by animateColorAsState(if (selected) color else ring, tween(200), label = "poll_check_border")
    Box(
        Modifier.size(26.dp).clip(CircleShape).background(bg).border(1.6.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = selected, enter = scaleIn(tween(180)) + fadeIn(tween(180)), exit = scaleOut(tween(120)) + fadeOut(tween(120))) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(17.dp))
        }
    }
}

/** صف نتيجة: الشريط ينمو من الصفر والنسبة تعدّ تصاعديًا بتتابع انسحابي بين الخيارات. */
@Composable
private fun ResultRow(
    text: String, votes: Int, total: Int, index: Int, mine: Boolean, quizState: Int,
    textColor: Color, fill: Color, track: Color, good: Color, bad: Color
) {
    val frac = if (total == 0) 0f else votes.toFloat() / total
    val anim = remember { Animatable(0f) }
    LaunchedEffect(frac) {
        if (anim.value == 0f) delay(index * 90L)
        anim.animateTo(frac, tween(750, easing = FastOutSlowInEasing))
    }
    val barColor = when (quizState) { 1 -> good; 2 -> bad; else -> fill }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = textColor, fontSize = 17.sp, modifier = Modifier.weight(1f))
            if (mine || quizState == 1) {
                Icon(
                    if (quizState == 2) Icons.Outlined.Cancel else Icons.Outlined.CheckCircle,
                    contentDescription = null, tint = barColor, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "${(anim.value * 100).roundToInt()}%", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End, modifier = Modifier.width(46.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(track)) {
            Box(Modifier.fillMaxWidth(anim.value.coerceIn(0.015f, 1f)).fillMaxHeight().clip(CircleShape).background(barColor))
        }
    }
}

// ======================= شاشة إنشاء استفتاء =======================
private const val MAX_OPTIONS = 12

@Composable
fun LabPollComposer(isChannel: Boolean, onDismiss: () -> Unit, onCreate: (Poll) -> Unit) {
    BackHandler { onDismiss() }
    val cfg = LocalSettingsTheme.current
    val theme = cfg.theme
    val accent = cfg.accent
    val pageBg = if (theme.isDark) Color(0xFF0B0B0B) else Color(0xFFF2F2F7)

    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var multiple by remember { mutableStateOf(false) }
    var revoting by remember { mutableStateOf(true) }
    var shuffle by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf(false) }
    var correct by remember { mutableStateOf<Int?>(null) }
    var restrictSubs by remember { mutableStateOf(false) }
    var restrictCountries by remember { mutableStateOf(false) }
    var durationOn by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(24L * 3600_000L) }

    val filled = options.count { it.isNotBlank() }
    val valid = question.isNotBlank() && filled >= 2 && (!quiz || (correct != null && options.getOrNull(correct!!)?.isNotBlank() == true))

    Column(Modifier.fillMaxSize().background(pageBg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        // الشريط العلوي
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(theme.surfaceColor).clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = theme.textPrimary) }
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier.weight(1f).height(46.dp).clip(CircleShape).background(theme.surfaceColor).padding(start = 18.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (quiz) "اختبار جديد" else "استفتاء جديد", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (valid) accent else accent.copy(alpha = 0.35f))
                        .clickable(enabled = valid) {
                            val idxs = options.indices.filter { options[it].isNotBlank() }
                            var order = idxs
                            if (shuffle) order = idxs.shuffled()
                            val finalOpts = order.map { PollOption(options[it].trim()) }
                            val corr = if (quiz) order.indexOf(correct) else null
                            onCreate(
                                Poll(
                                    question = question.trim(), options = finalOpts,
                                    multiple = multiple && !quiz, revoting = revoting && !quiz, quizCorrect = corr,
                                    restrictSubscribers = restrictSubs, restrictCountries = restrictCountries,
                                    endsAt = if (durationOn) System.currentTimeMillis() + durationMs else null
                                )
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) { Text("إنشاء", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            // السؤال
            PollCard(theme.surfaceColor) {
                SectionTitle("السؤال", accent)
                PollField(question, { if (it.length <= 255) question = it }, "اطرح سؤالًا", theme.textPrimary, theme.textSecondary, accent)
            }
            Spacer(Modifier.height(14.dp))
            // الخيارات
            PollCard(theme.surfaceColor) {
                SectionTitle(if (quiz) "الخيارات (اختر الإجابة الصحيحة)" else "الخيارات", accent)
                options.forEachIndexed { i, text ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (quiz) {
                            Icon(
                                if (correct == i) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (correct == i) Color(0xFF22C55E) else theme.textSecondary,
                                modifier = Modifier.size(26.dp).clip(CircleShape).clickable { correct = i }
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Outlined.DragHandle, contentDescription = null, tint = theme.textSecondary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                        }
                        Box(Modifier.weight(1f)) {
                            PollField(text, { if (it.length <= 100) options[i] = it }, "خيار", theme.textPrimary, theme.textSecondary, accent)
                        }
                        if (options.size > 2) {
                            Icon(
                                Icons.Outlined.Close, contentDescription = null, tint = theme.textSecondary,
                                modifier = Modifier.size(28.dp).clip(CircleShape).clickable {
                                    options.removeAt(i)
                                    if (correct == i) correct = null else if (correct != null && correct!! > i) correct = correct!! - 1
                                }.padding(4.dp)
                            )
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(theme.textSecondary.copy(alpha = 0.12f)))
                }
                if (options.size < MAX_OPTIONS) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { options.add("") }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("إضافة خيار...", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text(
                if (options.size < MAX_OPTIONS) "يمكنك إضافة ${MAX_OPTIONS - options.size} خيارًا إضافيًا." else "وصلت إلى الحد الأقصى للخيارات.",
                color = theme.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 14.dp)
            )
            // الإعدادات
            PollCard(theme.surfaceColor) {
                SectionTitle("الإعدادات", accent)
                SettingRow(Icons.Outlined.DoneAll, Color(0xFFF59E0B), Color(0xFFD97706), "السماح بإجابات متعددة", "يستطيع المصوتون اختيار أكثر من خيار", multiple && !quiz, !quiz, accent) { multiple = it }
                SettingRow(Icons.Outlined.Autorenew, Color(0xFFA78BFA), Color(0xFF7C3AED), "السماح بإعادة التصويت", "بإمكان المصوتين تغيير تصويتهم", revoting && !quiz, !quiz, accent) { revoting = it }
                SettingRow(Icons.Outlined.Shuffle, Color(0xFFFB923C), Color(0xFFEA580C), "ترتيب الخيارات عشوائيًا", "تظهر الإجابات بترتيب عشوائي", shuffle, true, accent) { shuffle = it }
                SettingRow(Icons.Outlined.CheckCircle, Color(0xFF4ADE80), Color(0xFF16A34A), "تحديد الإجابة الصحيحة", "تحديد أحد الخيارات كإجابة صحيحة", quiz, true, accent) {
                    quiz = it
                    if (!it) correct = null
                }
                SettingRow(Icons.Outlined.Group, Color(0xFF60A5FA), Color(0xFF2563EB), if (isChannel) "تقييده للمشتركين" else "تقييده للأعضاء", "التصويت متاح فقط لمن انضموا قبل 24 ساعة", restrictSubs, true, accent) { restrictSubs = it }
                SettingRow(Icons.Outlined.LocationOn, Color(0xFF38BDF8), Color(0xFF0284C7), "التقييد لدول محددة", "التصويت متاح فقط للمستخدمين من دول محددة", restrictCountries, true, accent) { restrictCountries = it }
                SettingRow(Icons.Outlined.Timer, Color(0xFFFB7185), Color(0xFFE11D48), "تحديد المدة", "إغلاق الاستفتاء تلقائيًا في وقت محدد", durationOn, true, accent) { durationOn = it }
                if (durationOn) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("5 دقائق" to 300_000L, "ساعة" to 3_600_000L, "24 ساعة" to 86_400_000L, "أسبوع" to 604_800_000L).forEach { (label, ms) ->
                            val sel = durationMs == ms
                            Text(
                                label,
                                color = if (sel) Color.White else theme.textPrimary,
                                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clip(CircleShape).background(if (sel) accent else theme.textSecondary.copy(alpha = 0.14f)).clickable { durationMs = ms }.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PollCard(bg: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(bg).padding(horizontal = 16.dp, vertical = 14.dp), content = content)
}

@Composable
private fun SectionTitle(text: String, accent: Color) {
    Text(text, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun PollField(value: String, onChange: (String) -> Unit, hint: String, textColor: Color, hintColor: Color, accent: Color) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, color = hintColor, fontSize = 16.sp)
        BasicTextField(
            value = value, onValueChange = onChange,
            textStyle = TextStyle(color = textColor, fontSize = 16.sp),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector, c1: Color, c2: Color, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean, accent: Color, onChange: (Boolean) -> Unit
) {
    val theme = LocalSettingsTheme.current.theme
    val alpha = if (enabled) 1f else 0.45f
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(c1, c2))),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = alpha), modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = theme.textPrimary.copy(alpha = alpha), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = theme.textSecondary.copy(alpha = alpha), fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White)
        )
    }
}
