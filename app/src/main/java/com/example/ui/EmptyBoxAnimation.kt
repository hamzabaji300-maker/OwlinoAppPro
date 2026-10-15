package com.example.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/** صندوق فارغ متحرك (res/raw/empty_box.json) يظهر عند عدم وجود نتائج بحث. */
@Composable
fun EmptyBoxAnimation(size: Dp = 120.dp, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.example.R.raw.empty_box)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}
