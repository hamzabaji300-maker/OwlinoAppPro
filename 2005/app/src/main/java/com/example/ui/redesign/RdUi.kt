package com.example.ui.redesign

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Port Compose du kit UI du prototype (UiKit.kt / HomeUi.kt / SearchUi.kt / BottomBarActivity.kt).
 * Uniquement du visuel : aucune donnée, aucun état métier.
 * Le repère du prototype est « 591 unités = largeur de l'écran » : [rdK] donne les dp par unité.
 */
@Composable
fun rdK(): Float = LocalConfiguration.current.screenWidthDp / 591f

/** Couleurs du prototype (UiColors + couleurs de la barre du bas et des onglets). */
object RdColors {
    fun bg(d: Boolean) = if (d) Color(0xFF191919) else Color(0xFFF9FAFB)
    fun surface(d: Boolean) = if (d) Color(0xFF272727) else Color(0xFFFFFFFF)
    fun text(d: Boolean) = if (d) Color(0xFFFFFFFF) else Color(0xFF000000)
    fun text2(d: Boolean) = if (d) Color(0xFFAAAAAA) else Color(0xFF6B7280)
    fun preview(d: Boolean) = if (d) Color(0xFFCFCFCF) else Color(0xFF222222)
    fun chip(d: Boolean) = if (d) Color(0x1AFFFFFF) else Color(0xFFF3F4F6)
    fun border(d: Boolean) = if (d) Color(0x0FFFFFFF) else Color(0x0A000000)
    fun divider(d: Boolean) = if (d) Color(0x1FFFFFFF) else Color(0x14000000)
    fun pill(d: Boolean) = if (d) Color(0xFF2A3350) else Color(0xFFE6EDFF)
    fun blue(d: Boolean) = if (d) Color(0xFF8FB0FF) else Color(0xFF4A7BE5)
    fun greyBar(d: Boolean) = if (d) Color(0xFF9A9DA3) else Color(0xFF5F6168)
    fun greyTab(d: Boolean) = if (d) Color(0xFF9A9DA3) else Color(0xFF6B6E7A)
    fun badgeFill(d: Boolean) = if (d) Color(0xFF3E5190) else Color(0xFFD3DFFF)
    fun badgeText(d: Boolean) = if (d) Color(0xFFDCE6FF) else Color(0xFF2F55C4)
    val Blue = Color(0xFF3B82F6)
    val Green = Color(0xFF22C55E)
}

// ----------------------------------------------------------------------------------------------
// Icônes (grille 24x24), mêmes coordonnées que UiGlyph
// ----------------------------------------------------------------------------------------------
object RdGlyphKind {
    const val MENU = 0
    const val SEARCH = 1
    const val PENCIL = 2
    const val CHEV_DOWN = 3
    const val MORE = 4
    const val MEGA = 5
    const val BELL_OFF = 6
    const val STAR = 7
    const val PIN = 8
    const val CHECK = 9
    const val DONE_ALL = 10
    const val BOT = 11
    const val VERIFIED = 12
    const val MIC = 13
    const val CLOSE = 14
    const val ARROW_BACK = 15
    const val CHEV_RIGHT = 16
}

@Composable
fun RdGlyph(kind: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = min(size.width, size.height) / 24f
        withTransform({
            translate((size.width - 24f * s) / 2f, (size.height - 24f * s) / 2f)
            scale(s, s, pivot = Offset.Zero)
        }) { rdDrawGlyph(kind, color) }
    }
}

private fun rdStroke(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.rdLine(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float) =
    drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = w, cap = StrokeCap.Round)

private fun DrawScope.rdPoly(c: Color, w: Float, vararg v: Float) {
    val path = Path()
    path.moveTo(v[0], v[1])
    var i = 2
    while (i < v.size) {
        path.lineTo(v[i], v[i + 1])
        i += 2
    }
    drawPath(path, c, style = rdStroke(w))
}

private fun DrawScope.rdDrawGlyph(kind: Int, c: Color) {
    when (kind) {
        RdGlyphKind.MENU -> {
            rdLine(c, 3f, 6f, 21f, 6f, 2f)
            rdLine(c, 3f, 12f, 21f, 12f, 2f)
            rdLine(c, 3f, 18f, 21f, 18f, 2f)
        }
        RdGlyphKind.SEARCH -> {
            drawCircle(c, 6.5f, Offset(10.5f, 10.5f), style = rdStroke(2.2f))
            rdLine(c, 15.4f, 15.4f, 20.5f, 20.5f, 2.2f)
        }
        RdGlyphKind.PENCIL -> {
            val a = Path()
            a.moveTo(3f, 17.25f); a.lineTo(3f, 21f); a.lineTo(6.75f, 21f); a.lineTo(17.81f, 9.94f); a.lineTo(14.06f, 6.19f); a.close()
            drawPath(a, c, style = Fill)
            val b = Path()
            b.moveTo(20.71f, 7.04f); b.cubicTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
            b.lineTo(18.37f, 3.29f); b.cubicTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
            b.lineTo(15.13f, 5.12f); b.lineTo(18.88f, 8.87f); b.close()
            drawPath(b, c, style = Fill)
        }
        RdGlyphKind.CHEV_DOWN -> rdPoly(c, 2.4f, 6f, 9f, 12f, 15f, 18f, 9f)
        RdGlyphKind.MORE -> {
            drawCircle(c, 2f, Offset(12f, 5f))
            drawCircle(c, 2f, Offset(12f, 12f))
            drawCircle(c, 2f, Offset(12f, 19f))
        }
        RdGlyphKind.MEGA -> {
            rdPoly(c, 1.9f, 3f, 10f, 7f, 10f, 15f, 5.5f, 15f, 18.5f, 7f, 14f, 3f, 14f, 3f, 10f)
            rdLine(c, 7f, 14f, 8.5f, 19.5f, 1.9f)
            rdLine(c, 18.5f, 9.5f, 18.5f, 14.5f, 1.9f)
        }
        RdGlyphKind.BELL_OFF -> {
            val b = Path()
            b.moveTo(5.5f, 17f)
            b.cubicTo(7f, 15.5f, 7.5f, 14f, 7.5f, 11f)
            b.cubicTo(7.5f, 8.5f, 9.5f, 6.5f, 12f, 6.5f)
            b.cubicTo(14.5f, 6.5f, 16.5f, 8.5f, 16.5f, 11f)
            b.cubicTo(16.5f, 14f, 17f, 15.5f, 18.5f, 17f)
            b.close()
            drawPath(b, c, style = rdStroke(1.9f))
            val k = Path()
            k.moveTo(10f, 19.5f); k.quadraticBezierTo(12f, 21.5f, 14f, 19.5f)
            drawPath(k, c, style = rdStroke(1.9f))
            rdLine(c, 4.5f, 4.5f, 19.5f, 19.5f, 1.9f)
        }
        RdGlyphKind.STAR -> {
            val path = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) 9.2f else 4.4f
                val a = -Math.PI / 2.0 + i * Math.PI / 5.0
                val x = 12f + (r * cos(a)).toFloat()
                val y = 12.8f + (r * sin(a)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, c, style = Fill)
            drawPath(path, c, style = Stroke(width = 1.2f, join = StrokeJoin.Round))
        }
        RdGlyphKind.PIN -> {
            val b = Path()
            b.moveTo(16f, 9f); b.lineTo(16f, 4f); b.lineTo(17f, 4f)
            b.cubicTo(17.55f, 4f, 18f, 3.55f, 18f, 3f)
            b.cubicTo(18f, 2.45f, 17.55f, 2f, 17f, 2f)
            b.lineTo(7f, 2f)
            b.cubicTo(6.45f, 2f, 6f, 2.45f, 6f, 3f)
            b.cubicTo(6f, 3.55f, 6.45f, 4f, 7f, 4f)
            b.lineTo(8f, 4f); b.lineTo(8f, 9f)
            b.cubicTo(8f, 10.66f, 6.66f, 12f, 5f, 12f)
            b.lineTo(5f, 14f); b.lineTo(10.97f, 14f); b.lineTo(10.97f, 21f); b.lineTo(11.97f, 22f); b.lineTo(12.97f, 21f); b.lineTo(12.97f, 14f)
            b.lineTo(19f, 14f); b.lineTo(19f, 12f)
            b.cubicTo(17.34f, 12f, 16f, 10.66f, 16f, 9f)
            b.close()
            drawPath(b, c, style = Fill)
        }
        RdGlyphKind.CHECK -> rdPoly(c, 2.4f, 5f, 12.5f, 10f, 17.5f, 19f, 7.5f)
        RdGlyphKind.DONE_ALL -> {
            rdPoly(c, 2.2f, 2f, 13f, 7f, 18f, 17f, 8f)
            rdPoly(c, 2.2f, 11.5f, 16.5f, 13f, 18f, 22.5f, 8f)
        }
        RdGlyphKind.BOT -> {
            drawRoundRect(c, Offset(4f, 8f), Size(16f, 12f), CornerRadius(3f, 3f), style = rdStroke(1.8f))
            rdLine(c, 12f, 8f, 12f, 4.5f, 1.8f)
            rdLine(c, 2f, 12f, 2f, 16f, 1.8f)
            rdLine(c, 22f, 12f, 22f, 16f, 1.8f)
            drawCircle(c, 1.4f, Offset(12f, 3.8f))
            drawCircle(c, 1.3f, Offset(9f, 13.8f))
            drawCircle(c, 1.3f, Offset(15f, 13.8f))
        }
        RdGlyphKind.VERIFIED -> {
            val path = Path()
            for (i in 0..72) {
                val th = i * 5.0 * Math.PI / 180.0
                val r = 9.4 + 0.8 * cos(8.0 * th)
                val x = (12.0 + r * cos(th)).toFloat()
                val y = (12.0 + r * sin(th)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Color(0xFF3B82F6), style = Fill)
            rdPoly(Color.White, 2f, 7.8f, 12.3f, 10.8f, 15.2f, 16.4f, 9.2f)
        }
        RdGlyphKind.MIC -> {
            drawRoundRect(c, Offset(9f, 3f), Size(6f, 11f), CornerRadius(3f, 3f), style = Fill)
            val a = Path()
            a.moveTo(6f, 11f); a.cubicTo(6f, 14.3f, 8.7f, 17f, 12f, 17f); a.cubicTo(15.3f, 17f, 18f, 14.3f, 18f, 11f)
            drawPath(a, c, style = rdStroke(1.8f))
            rdLine(c, 12f, 17f, 12f, 21f, 1.8f)
        }
        RdGlyphKind.CLOSE -> {
            rdLine(c, 6f, 6f, 18f, 18f, 2.2f)
            rdLine(c, 18f, 6f, 6f, 18f, 2.2f)
        }
        RdGlyphKind.CHEV_RIGHT -> rdPoly(c, 2.2f, 9f, 5.5f, 15.5f, 12f, 9f, 18.5f)
        RdGlyphKind.ARROW_BACK -> {
            rdLine(c, 20f, 12f, 4f, 12f, 2.2f)
            rdPoly(c, 2.2f, 11f, 5f, 4f, 12f, 11f, 19f)
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Onglets à pastille glissante (SearchTabStrip) : suit le doigt pendant le balayage des pages
// ----------------------------------------------------------------------------------------------
private class RdStripMetrics(val lefts: FloatArray, val widths: FloatArray, val total: Float)

/**
 * [progress] : position fractionnaire (0.0 = 1er onglet, 1.0 = 2e, 0.5 = entre les deux).
 * Les onglets se répartissent à parts égales si la place le permet, sinon le bandeau défile.
 */
@Composable
fun RdTabStrip(
    labels: List<String>,
    isDark: Boolean,
    progress: () -> Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val n = labels.size
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
        val layouts = remember(labels, density) { labels.map { measurer.measure(AnnotatedString(it), style) } }
        val select = rememberUpdatedState(onSelect)
        val scroll = rememberScrollState()
        val pillCol = RdColors.pill(isDark)
        val blue = RdColors.blue(isDark)
        val grey = RdColors.greyTab(isDark)

        BoxWithConstraints(modifier.fillMaxWidth().height(46.dp)) {
            val availPx = with(density) { maxWidth.toPx() }
            val padH = with(density) { 14.dp.toPx() }
            val gap = with(density) { 4.dp.toPx() }
            val margin = with(density) { 10.dp.toPx() }
            val metrics = remember(layouts, availPx) {
                val w = FloatArray(n)
                val l = FloatArray(n)
                var natural = margin * 2 + gap * (n - 1)
                for (i in 0 until n) {
                    w[i] = layouts[i].size.width + padH * 2
                    natural += w[i]
                }
                val total = max(natural, availPx)
                val extra = (total - natural) / n
                var x = margin
                for (i in 0 until n) {
                    w[i] += extra
                    l[i] = x
                    x += w[i] + gap
                }
                RdStripMetrics(l, w, total)
            }

            LaunchedEffect(metrics) {
                snapshotFlow { progress() }.collect { v ->
                    if (metrics.total > availPx) {
                        val pv = v.coerceIn(0f, (n - 1).toFloat())
                        val i = min(floor(pv).toInt(), n - 1)
                        val j = min(i + 1, n - 1)
                        val f = pv - i
                        val cx = (metrics.lefts[i] + metrics.widths[i] / 2f) +
                            ((metrics.lefts[j] + metrics.widths[j] / 2f) - (metrics.lefts[i] + metrics.widths[i] / 2f)) * f
                        scroll.scrollTo((cx - availPx / 2f).roundToInt().coerceIn(0, scroll.maxValue))
                    }
                }
            }

            Canvas(
                Modifier
                    .horizontalScroll(scroll)
                    .width(with(density) { metrics.total.toDp() })
                    .height(46.dp)
                    .pointerInput(metrics) {
                        detectTapGestures { o ->
                            for (i in 0 until n) {
                                if (o.x >= metrics.lefts[i] - gap / 2 && o.x <= metrics.lefts[i] + metrics.widths[i] + gap / 2) {
                                    select.value(i)
                                    break
                                }
                            }
                        }
                    }
            ) {
                val v = progress().coerceIn(0f, (n - 1).toFloat())
                val i0 = min(floor(v).toInt(), n - 1)
                val j0 = min(i0 + 1, n - 1)
                val f0 = v - i0
                val l = metrics.lefts[i0] + (metrics.lefts[j0] - metrics.lefts[i0]) * f0
                val r = metrics.lefts[i0] + metrics.widths[i0] +
                    (metrics.lefts[j0] + metrics.widths[j0] - metrics.lefts[i0] - metrics.widths[i0]) * f0
                val h = 34.dp.toPx()
                val top = (size.height - h) / 2f
                drawRoundRect(pillCol, Offset(l, top), Size(r - l, h), CornerRadius(h / 2f, h / 2f))
                for (i in 0 until n) {
                    val w = (1f - kotlin.math.abs(v - i)).coerceIn(0f, 1f)
                    val lay = layouts[i]
                    drawText(
                        lay,
                        color = lerp(grey, blue, w),
                        topLeft = Offset(
                            metrics.lefts[i] + metrics.widths[i] / 2f - lay.size.width / 2f,
                            size.height / 2f - lay.size.height / 2f
                        )
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Barre du bas (BottomBarView) : pastille bleu clair qui glisse (240 ms, decelerate 1.6)
// ----------------------------------------------------------------------------------------------
private fun rdBubble(
    x0: Float, y0: Float, s: Float,
    l: Float, t: Float, r: Float, b: Float, rad: Float, tw: Float, th: Float, tailLeft: Boolean
): Path {
    fun x(v: Float) = x0 + v * s
    fun y(v: Float) = y0 + v * s
    val d = 2f * rad * s
    return Path().apply {
        moveTo(x(l + rad), y(t))
        lineTo(x(r - rad), y(t))
        arcTo(Rect(x(r) - d, y(t), x(r), y(t) + d), 270f, 90f, false)
        if (tailLeft) {
            lineTo(x(r), y(b - rad))
            arcTo(Rect(x(r) - d, y(b) - d, x(r), y(b)), 0f, 90f, false)
            lineTo(x(l + tw), y(b))
            lineTo(x(l), y(b + th))
            lineTo(x(l), y(t + rad))
        } else {
            lineTo(x(r), y(b + th))
            lineTo(x(r - tw), y(b))
            lineTo(x(l + rad), y(b))
            arcTo(Rect(x(l), y(b) - d, x(l) + d, y(b)), 90f, 90f, false)
            lineTo(x(l), y(t + rad))
        }
        arcTo(Rect(x(l), y(t), x(l) + d, y(t) + d), 180f, 90f, false)
        close()
    }
}

/**
 * Barre du bas du prototype. [unreadCount] = vrai compteur de non lus (le badge disparaît à 0).
 * [selectedIndex] : 0 = Chats, 1 = Followers. Appui moitié gauche / droite.
 */
@Composable
fun RdBottomBar(
    chatsLabel: String,
    followersLabel: String,
    selectedIndex: Int,
    unreadCount: Int,
    isDark: Boolean,
    onChatsClick: () -> Unit,
    onFollowersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val k = rdK()
        val density = LocalDensity.current
        val kPx = with(density) { k.dp.toPx() }
        val prog by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = tween(240, easing = RdDecelerateEasing(1.6f)),
            label = "rdBarPill"
        )
        val measurer = rememberTextMeasurer()
        val onChats = rememberUpdatedState(onChatsClick)
        val onFollowers = rememberUpdatedState(onFollowersClick)

        val barBg = RdColors.bg(isDark)
        val pillCol = RdColors.pill(isDark)
        val blue = RdColors.blue(isDark)
        val grey = RdColors.greyBar(isDark)
        val badgeFill = RdColors.badgeFill(isDark)
        val badgeText = RdColors.badgeText(isDark)
        val hairline = if (isDark) Color(0x22FFFFFF) else Color(0x14000000)

        val labelSizeSp = with(density) { (15f * kPx).toSp() }
        val badgeSizeSp = with(density) { (11.5f * kPx).toSp() }
        val labelReg = remember(chatsLabel, followersLabel, kPx) {
            listOf(
                measurer.measure(AnnotatedString(chatsLabel), TextStyle(fontSize = labelSizeSp)),
                measurer.measure(AnnotatedString(followersLabel), TextStyle(fontSize = labelSizeSp))
            )
        }
        val labelBold = remember(chatsLabel, followersLabel, kPx) {
            listOf(
                measurer.measure(AnnotatedString(chatsLabel), TextStyle(fontSize = labelSizeSp, fontWeight = FontWeight.Bold)),
                measurer.measure(AnnotatedString(followersLabel), TextStyle(fontSize = labelSizeSp, fontWeight = FontWeight.Bold))
            )
        }
        val badgeStr = if (unreadCount > 99) "99+" else unreadCount.toString()
        val badgeLay = remember(badgeStr, kPx) {
            measurer.measure(AnnotatedString(badgeStr), TextStyle(fontSize = badgeSizeSp, fontWeight = FontWeight.Bold))
        }

        Canvas(
            modifier
                .fillMaxWidth()
                .height((88f * k).dp)
                .pointerInput(Unit) {
                    detectTapGestures { o -> if (o.x < size.width / 2f) onChats.value() else onFollowers.value() }
                }
        ) {
            val w = size.width
            val h = size.height
            val kk = w / 591f

            drawRect(barBg)
            drawRect(hairline, Offset.Zero, Size(w, max(1f, 0.6f * kk)))

            // Pastille bleu clair (glisse entre les deux éléments)
            val pcx = w * (0.25f + 0.5f * prog)
            val pw = 112f * kk
            val ph = 78f * kk
            val pt = (h - ph) / 2f
            drawRoundRect(pillCol, Offset(pcx - pw / 2f, pt), Size(pw, ph), CornerRadius(36f * kk, 36f * kk))

            val s = 34f * kk
            val cy = h / 2f - 11f * kk
            val labelY = h / 2f + 27f * kk

            for (i in 0..1) {
                val cx = w * (0.25f + 0.5f * i)
                val sf = (if (i == 0) 1f - prog else prog).coerceIn(0f, 1f)
                val col = lerp(grey, blue, sf)
                if (i == 0) {
                    rdChatIcon(cx, cy, s, col, lerp(barBg, pillCol, sf), kk, unreadCount, badgeStr, badgeLay, badgeFill, badgeText)
                } else {
                    rdPeopleIcon(cx, cy, s, col)
                }
                val lay = if (sf > 0.5f) labelBold[i] else labelReg[i]
                drawText(lay, color = col, topLeft = Offset(cx - lay.size.width / 2f, labelY - lay.firstBaseline))
            }
        }
    }
}

private fun DrawScope.rdChatIcon(
    cx: Float, cy: Float, s: Float, color: Color, gap: Color, kk: Float,
    unread: Int, badgeStr: String, badgeLay: androidx.compose.ui.text.TextLayoutResult,
    badgeFill: Color, badgeText: Color
) {
    val x0 = cx - s / 2f
    val y0 = cy - s / 2f
    // Bulle arrière (queue en bas à droite) puis bulle avant (queue en bas à gauche)
    val back = rdBubble(x0, y0, s, 0.42f, 0.28f, 1.00f, 0.74f, 0.15f, 0.17f, 0.15f, false)
    val front = rdBubble(x0, y0, s, 0.02f, 0.06f, 0.70f, 0.60f, 0.17f, 0.19f, 0.17f, true)
    drawPath(back, color, style = Fill)
    // Liseré de la couleur du fond pour détacher la bulle avant
    drawPath(front, gap, style = Stroke(width = 0.10f * s, join = StrokeJoin.Round))
    drawPath(front, color, style = Fill)

    if (unread > 0) {
        val bx = x0 + 0.97f * s
        val by = y0 + 0.08f * s
        val br = 10f * kk
        val bw = max(2f * br, badgeLay.size.width + 10f * kk)
        val ring = 2f * kk
        drawRoundRect(gap, Offset(bx - bw / 2f - ring, by - br - ring), Size(bw + 2f * ring, 2f * br + 2f * ring), CornerRadius(br + ring, br + ring))
        drawRoundRect(badgeFill, Offset(bx - bw / 2f, by - br), Size(bw, 2f * br), CornerRadius(br, br))
        drawText(badgeLay, color = badgeText, topLeft = Offset(bx - badgeLay.size.width / 2f, by - badgeLay.size.height / 2f))
    }
}

private fun DrawScope.rdPeopleIcon(cx: Float, cy: Float, s: Float, color: Color) {
    val x0 = cx - s / 2f
    val y0 = cy - s / 2f - 0.02f * s
    fun x(v: Float) = x0 + v * s
    fun y(v: Float) = y0 + v * s
    val st = Stroke(width = 0.085f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Personne arrière (droite) : tête + épaule
    drawCircle(color, 0.125f * s, Offset(x(0.76f), y(0.27f)), style = st)
    val b2 = Path().apply {
        moveTo(x(0.73f), y(0.545f))
        cubicTo(x(0.87f), y(0.545f), x(0.97f), y(0.64f), x(0.97f), y(0.90f))
    }
    drawPath(b2, color, style = st)

    // Personne avant : tête + épaules symétriques
    drawCircle(color, 0.16f * s, Offset(x(0.36f), y(0.29f)), style = st)
    val b1 = Path().apply {
        moveTo(x(0.04f), y(0.92f))
        cubicTo(x(0.04f), y(0.66f), x(0.18f), y(0.58f), x(0.36f), y(0.58f))
        cubicTo(x(0.54f), y(0.58f), x(0.68f), y(0.66f), x(0.68f), y(0.92f))
    }
    drawPath(b1, color, style = st)
}
