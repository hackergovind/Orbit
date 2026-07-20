package com.bmtp.app.ui.discovery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.bmtp.app.ui.theme.SignalEmpty
import com.bmtp.app.ui.theme.SignalExcellent
import com.bmtp.app.ui.theme.SignalFair
import com.bmtp.app.ui.theme.SignalGood
import com.bmtp.app.ui.theme.SignalVeryWeak
import com.bmtp.app.ui.theme.SignalWeak
import com.bmtp.app.utils.RssiUtils

/**
 * Custom component displaying a 4-bar signal strength indicator.
 *
 * @param rssi The raw RSSI value to display.
 * @param modifier Modifier for styling or layout.
 */
@Composable
fun SignalStrengthIndicator(rssi: Int, modifier: Modifier = Modifier) {
    val level = RssiUtils.rssiToSignalLevel(rssi)
    
    val activeColor = when (level) {
        4 -> SignalExcellent
        3 -> SignalGood
        2 -> SignalFair
        1 -> SignalWeak
        else -> SignalVeryWeak
    }

    Canvas(modifier = modifier.size(24.dp, 20.dp)) {
        val totalBars = 4
        val barWidth = size.width / (totalBars * 2f - 1f)
        val cornerRadius = CornerRadius(2.dp.toPx())

        for (i in 0 until totalBars) {
            val barHeight = size.height * ((i + 1f) / totalBars)
            val x = i * (barWidth * 2f)
            val y = size.height - barHeight

            val isFilled = i < level
            val color = if (isFilled) activeColor else SignalEmpty

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
