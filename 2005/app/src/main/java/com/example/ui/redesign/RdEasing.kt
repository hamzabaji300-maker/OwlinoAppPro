package com.example.ui.redesign

import androidx.compose.animation.core.Easing
import kotlin.math.pow

/** Équivalent Compose de android.view.animation.DecelerateInterpolator(factor) : 1 - (1 - x)^(2 * factor). */
class RdDecelerateEasing(private val factor: Float) : Easing {
    override fun transform(fraction: Float): Float =
        if (factor == 1f) 1f - (1f - fraction) * (1f - fraction)
        else 1f - (1f - fraction).pow(2f * factor)
}
