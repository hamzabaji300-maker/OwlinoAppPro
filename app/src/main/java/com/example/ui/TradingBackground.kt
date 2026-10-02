package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle

data class TradingDoodleItem(val text: String, val x: Float, val y: Float, val fontSize: Float, val rotation: Float)

@Composable
fun TradingBackground(isDark: Boolean = false) {
    val items = remember {
        val list = mutableListOf<TradingDoodleItem>()
        var seed = 42.0
        fun seededRandom(): Float {
            val x = kotlin.math.sin(seed++) * 10000.0
            return (x - kotlin.math.floor(x)).toFloat()
        }
        val markets = listOf("NASDAQ", "XAUUSD", "BTCUSD", "ETHUSD", "S&P500", "US30", "OIL", "GOLD", "EURUSD", "GBPUSD", "TSLA", "AAPL", "MSFT", "FOREX", "CRYPTO", "NYSE", "FTSE", "NIFTY", "VIX", "WTI", "BRENT", "SILVER", "COPPER", "USDJPY", "AUDUSD", "SOLUSD", "BNB", "XRP", "ADA", "DOGE", "META", "AMZN", "NVDA", "DXY", "RUT", "DAX", "CAC40", "NIKKEI")

        for (x in 0 until 600 step 75) {
            for (y in 0 until 600 step 75) {
                if (seededRandom() > 0.85f) continue
                val text = markets[(seededRandom() * markets.size).toInt().coerceIn(0, markets.lastIndex)]
                val offsetX = (seededRandom() - 0.5f) * 50f
                val offsetY = (seededRandom() - 0.5f) * 50f
                val fontSize = 12f + kotlin.math.floor(seededRandom() * 16f)
                val rotation = kotlin.math.floor(seededRandom() * 360f)

                list.add(TradingDoodleItem(text, x.toFloat() + offsetX, y.toFloat() + offsetY, fontSize, rotation))
            }
        }
        list
    }

    val textMeasurer = rememberTextMeasurer()
    val textColor = if (isDark) Color(0xFF9CA3AF) else Color.Black
    val alpha = if (isDark) 0.12f else 0.18f

    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        val tileSize = 450.dp.toPx() // Slightly smaller tile for mobile screens so text density is better
        val cols = kotlin.math.ceil((size.width / tileSize).toDouble()).toInt() + 1
        val rows = kotlin.math.ceil((size.height / tileSize).toDouble()).toInt() + 1

        for (col in 0 until cols) {
            for (row in 0 until rows) {
                withTransform({
                    translate(left = col * tileSize, top = row * tileSize)
                }) {
                    items.forEach { item ->
                        val xPx = (item.x / 600f) * tileSize
                        val yPx = (item.y / 600f) * tileSize

                        withTransform({
                            translate(left = xPx, top = yPx)
                            rotate(degrees = item.rotation)
                        }) {
                            val textLayoutResult = textMeasurer.measure(
                                text = item.text,
                                style = TextStyle(
                                    color = textColor,
                                    fontSize = item.fontSize.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            )
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    x = -textLayoutResult.size.width / 2f,
                                    y = -textLayoutResult.size.height / 2f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
