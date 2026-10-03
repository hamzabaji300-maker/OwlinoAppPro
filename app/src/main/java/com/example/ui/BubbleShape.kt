package com.example.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class BubbleShape(private val cornerRadiusDp: Float = 16f, private val isMine: Boolean = true) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val d = density.density
        
        val cr = (cornerRadiusDp * d).coerceAtMost(h / 2f)
        val tailWidth = 10f * d
        val tailHeight = 14f * d

        if (isMine) {
            val bodyRight = w - tailWidth
            
            // Top Left
            path.moveTo(cr, 0f)
            // Top Right
            path.lineTo(bodyRight - cr, 0f)
            path.arcTo(Rect(bodyRight - 2 * cr, 0f, bodyRight, 2 * cr), -90f, 90f, false)
            
            // Right edge down to where the tail starts
            path.lineTo(bodyRight, h - tailHeight)
            
            // Curve out to the tail tip
            path.quadraticBezierTo(bodyRight, h, w, h)
            
            // Flat bottom edge
            path.lineTo(cr, h)
            path.arcTo(Rect(0f, h - 2 * cr, 2 * cr, h), 90f, 90f, false)
            
            // Top Left
            path.lineTo(0f, cr)
            path.arcTo(Rect(0f, 0f, 2 * cr, 2 * cr), 180f, 90f, false)
        } else {
            val bodyLeft = tailWidth
            
            // Top Right
            path.moveTo(w - cr, 0f)
            path.arcTo(Rect(w - 2 * cr, 0f, w, 2 * cr), -90f, 90f, false)
            
            // Bottom Right
            path.lineTo(w, h - cr)
            path.arcTo(Rect(w - 2 * cr, h - 2 * cr, w, h), 0f, 90f, false)
            
            // Flat bottom edge to the tail tip
            path.lineTo(0f, h)
            
            // Curve up from the tail tip to the left edge
            path.quadraticBezierTo(bodyLeft, h, bodyLeft, h - tailHeight)
            
            // Top Left
            path.lineTo(bodyLeft, cr)
            path.arcTo(Rect(bodyLeft, 0f, bodyLeft + 2 * cr, 2 * cr), 180f, 90f, false)
        }
        
        path.close()
        return Outline.Generic(path)
    }
}
