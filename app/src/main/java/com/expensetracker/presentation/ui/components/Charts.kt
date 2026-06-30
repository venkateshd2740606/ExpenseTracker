package com.expensetracker.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.CategorySpending
import com.expensetracker.domain.model.MonthlySpending
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PieChart(categories: List<CategorySpending>, modifier: Modifier = Modifier) {
    if (categories.isEmpty()) {
        Box(modifier.height(180.dp), contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    val animated by animateFloatAsState(1f, label = "pie")
    Canvas(modifier = modifier.height(180.dp).fillMaxWidth()) {
        val total = categories.sumOf { it.amount }.toFloat()
        var startAngle = -90f
        val radius = size.minDimension / 2.5f
        val center = Offset(size.width / 2, size.height / 2)
        categories.forEach { cat ->
            val sweep = (cat.amount / total * 360f) * animated
            drawArc(
                color = Color(cat.colorArgb),
                startAngle = startAngle,
                sweepAngle = sweep.toFloat(),
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            startAngle += sweep.toFloat()
        }
    }
}

@Composable
fun BarChart(monthly: List<MonthlySpending>, modifier: Modifier = Modifier, barColor: Color = MaterialTheme.colorScheme.primary) {
    if (monthly.isEmpty()) return
    val max = monthly.maxOf { it.amount }.coerceAtLeast(1.0)
    val animated by animateFloatAsState(1f, label = "bar")
    Canvas(modifier = modifier.height(160.dp).fillMaxWidth()) {
        val barWidth = size.width / (monthly.size * 2f)
        monthly.forEachIndexed { i, m ->
            val barHeight = (m.amount / max * size.height * 0.8 * animated).toFloat()
            drawRoundRect(
                color = barColor,
                topLeft = Offset(barWidth + i * barWidth * 2, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
fun SpendingHeatMap(monthly: List<MonthlySpending>, modifier: Modifier = Modifier) {
    val max = monthly.maxOfOrNull { it.amount } ?: 1.0
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        monthly.forEach { m ->
            val intensity = (m.amount / max).coerceIn(0.0, 1.0)
            val color = MaterialTheme.colorScheme.primary.copy(alpha = (0.2f + intensity * 0.8f).toFloat())
            Box(modifier = Modifier.weight(1f).height(32.dp).padding(2.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(color, cornerRadius = CornerRadius(4f, 4f))
                }
            }
        }
    }
}

@Composable
fun HealthScoreRing(score: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(score / 100f, label = "health")
    val color = when {
        score >= 80 -> Color(0xFF43A047)
        score >= 50 -> Color(0xFFFFA726)
        else -> Color(0xFFE53935)
    }
    Box(modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(Color.LightGray.copy(0.3f), 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(8f))
            drawArc(color, -90f, 360f * animated, false, style = androidx.compose.ui.graphics.drawscope.Stroke(8f))
        }
        Text("$score", style = MaterialTheme.typography.titleMedium)
    }
}
