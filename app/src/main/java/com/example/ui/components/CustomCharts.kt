package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentRose
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.viewmodel.MonthlyDataPoint

@Composable
fun IncomeExpenseBarChart(
    data: List<MonthlyDataPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income vs Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(PrimaryEmerald, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(8.dp).background(AccentRose, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty() || data.all { it.income == 0.0 && it.expense == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data for this period yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = maxOf(100.0, data.maxOf { maxOf(it.income, it.expense) })

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val w = size.width
                    val h = size.height - 24.dp.toPx()
                    val groupWidth = w / data.size
                    val barWidth = (groupWidth * 0.28f).coerceIn(8.dp.toPx(), 22.dp.toPx())

                    // Baseline
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1.dp.toPx()
                    )

                    data.forEachIndexed { i, dp ->
                        val groupCenter = i * groupWidth + (groupWidth / 2)
                        val incomeH = ((dp.income / maxVal) * h).toFloat().coerceAtLeast(3f)
                        val expenseH = ((dp.expense / maxVal) * h).toFloat().coerceAtLeast(3f)

                        // Income Bar (Left)
                        drawRoundRect(
                            color = PrimaryEmerald,
                            topLeft = Offset(groupCenter - barWidth - 2.dp.toPx(), h - incomeH),
                            size = Size(barWidth, incomeH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Expense Bar (Right)
                        drawRoundRect(
                            color = AccentRose,
                            topLeft = Offset(groupCenter + 2.dp.toPx(), h - expenseH),
                            size = Size(barWidth, expenseH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                // Month labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEach { dp ->
                        Text(
                            text = dp.monthLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitTrendLineChart(
    data: List<MonthlyDataPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Profit Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val totalPrf = data.sumOf { it.profit }
                Text(
                    text = "$currency ${String.format("%.0f", totalPrf)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (totalPrf >= 0) PrimaryEmerald else AccentRose
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty() || data.all { it.profit == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No profit trend recorded yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = maxOf(100.0, data.maxOf { it.profit }.coerceAtLeast(0.0))
                val minVal = minOf(0.0, data.minOf { it.profit })
                val range = (maxVal - minVal).coerceAtLeast(50.0)
                val dotBgColor = MaterialTheme.colorScheme.surface

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val w = size.width
                    val h = size.height - 24.dp.toPx()
                    val stepX = w / (data.size - 1).coerceAtLeast(1)

                    val zeroY = (h - ((0.0 - minVal) / range * h)).toFloat()
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx()
                    )

                    val points = data.mapIndexed { index, point ->
                        val x = index * stepX
                        val normalizedY = (h - ((point.profit - minVal) / range * h)).toFloat()
                        Offset(x, normalizedY.coerceIn(0f, h))
                    }

                    // Fill under curve
                    if (points.size > 1) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, zeroY)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, zeroY)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(EmeraldLight.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // Draw line
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (j in 1 until points.size) {
                                lineTo(points[j].x, points[j].y)
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = PrimaryEmerald,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw dots
                        points.forEach { pt ->
                            drawCircle(color = dotBgColor, radius = 5.dp.toPx(), center = pt)
                            drawCircle(color = PrimaryEmerald, radius = 3.5f.dp.toPx(), center = pt)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEach { dp ->
                        Text(
                            text = dp.monthLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
