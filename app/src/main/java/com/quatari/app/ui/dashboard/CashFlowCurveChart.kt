package com.quatari.app.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quatari.app.domain.model.CashFlowPoint
import com.quatari.app.domain.util.AmountUtils
import com.quatari.app.ui.theme.Emerald400
import com.quatari.app.ui.theme.Emerald500
import com.quatari.app.ui.theme.Slate700
import com.quatari.app.ui.theme.Slate800

@Composable
fun CashFlowCurveChart(
    points: List<CashFlowPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No transaction curve yet. Categorize transactions to view cash flow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val maxVal = points.maxOfOrNull { it.netIncomeSantim }?.coerceAtLeast(100L) ?: 100L
    val minVal = points.minOfOrNull { it.netIncomeSantim }?.coerceAtMost(0L) ?: 0L
    val range = (maxVal - minVal).coerceAtLeast(1L).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected point preview
        if (selectedIndex in points.indices) {
            val pt = points[selectedIndex]
            Text(
                text = "${pt.label}: ${AmountUtils.formatSantimToEtb(pt.netIncomeSantim)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Emerald400,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        } else {
            Text(
                text = "Tap on curve to inspect points",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                        val idx = (offset.x / stepX).toInt().coerceIn(0, points.size - 1)
                        selectedIndex = idx
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val paddingVertical = 20f

            val usableHeight = height - (paddingVertical * 2)

            // Draw subtle horizontal grid lines
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = paddingVertical + (usableHeight / gridLines) * i
                drawLine(
                    color = Slate800,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.size == 1) {
                val y = height / 2
                drawCircle(
                    color = Emerald500,
                    radius = 6.dp.toPx(),
                    center = Offset(width / 2, y)
                )
                return@Canvas
            }

            val stepX = width / (points.size - 1)

            val coords = points.mapIndexed { index, pt ->
                val x = index * stepX
                val normalizedY = (pt.netIncomeSantim - minVal) / range
                // Inverted Y: higher value -> lower Y pixel
                val y = height - paddingVertical - (normalizedY * usableHeight * progress.value)
                Offset(x, y)
            }

            // Construct smooth cubic bezier path
            val strokePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlX1 = p0.x + (p1.x - p0.x) / 2
                    val controlY1 = p0.y
                    val controlX2 = p0.x + (p1.x - p0.x) / 2
                    val controlY2 = p1.y
                    cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }
            }

            // Fill under curve
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(coords.last().x, height)
                lineTo(coords.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Emerald500.copy(alpha = 0.35f),
                        Emerald500.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw curve line
            drawPath(
                path = strokePath,
                color = Emerald400,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw coordinate circles
            coords.forEachIndexed { idx, offset ->
                val isSelected = idx == selectedIndex
                drawCircle(
                    color = if (isSelected) Color.White else Emerald500,
                    radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                    center = offset
                )
            }
        }
    }
}
