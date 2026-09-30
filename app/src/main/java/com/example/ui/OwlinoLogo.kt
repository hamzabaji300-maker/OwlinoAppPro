package com.example.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun OwlinoLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    defaultColor: Color,
    accentColor: Color = Color(0xFF7E22CE)
) {
    Text(
        text = buildAnnotatedString {
            append("Owlin")
            withStyle(style = SpanStyle(color = accentColor)) {
                append("o")
            }
        },
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1).sp,
        color = defaultColor,
        modifier = modifier
    )
}
