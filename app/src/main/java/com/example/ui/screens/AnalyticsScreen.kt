package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CardEntity
import com.example.data.local.CardState
import com.example.domain.Sm2Scheduler
import com.example.ui.theme.AmbientGlowCyan
import com.example.ui.theme.RateEasyColor
import com.example.ui.theme.RateGoodColor
import com.example.ui.theme.RateHardColor
import com.example.ui.theme.geminiAmbientCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DayForecastBar(
    val dayLabel: String,
    val dateLabel: String,
    val count: Int,
    val isToday: Boolean
)

/**
 * Memory Analytics & 7-Day Review Forecast Screen replacing the technical code tab.
 * Displays:
 * - Circular Donut Chart of memory maturity (Новые / В изучении (<7 дн) / Закрепленные (7+ дн))
 * - Average Ease Factor (EF) and total retention health
 * - 7-Day Review Schedule Bar Chart showing when cards will mature and trigger notifications
 * - Ebbinghaus Spaced Repetition Optimal Cadence reference
 */
@Composable
fun AnalyticsScreen(
    allCards: List<CardEntity>,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val now = System.currentTimeMillis()
    val totalCards = allCards.size
    val newCardsCount = remember(allCards) { allCards.count { it.state == CardState.NEW } }
    val learningShortCount = remember(allCards) {
        allCards.count { it.state != CardState.NEW && it.interval < 7 }
    }
    val matureCount = remember(allCards) {
        allCards.count { it.state != CardState.NEW && it.interval >= 7 }
    }
    val averageEf = remember(allCards) {
        if (allCards.isEmpty()) 2.5 else allCards.map { it.easeFactor }.average()
    }
    val averageInterval = remember(allCards) {
        val reviewed = allCards.filter { it.state != CardState.NEW }
        if (reviewed.isEmpty()) 0 else reviewed.map { it.interval }.average().toInt()
    }

    // Compute 7-day review forecast bars
    val forecastBars = remember(allCards, now) {
        buildSevenDayForecast(allCards, now)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("analytics_screen_root"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Аналитика памяти и План повторений",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Распределение карточек по фазам удержания знаний и график предстоящих повторений по дням.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Card 1: Memory Distribution Donut Chart & Key SM-2 Metrics
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .geminiAmbientCard(cornerRadius = 26.dp, isActive = true)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Зрелость карточек в памяти",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Средний EF: ${String.format(Locale.US, "%.2f", averageEf)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Donut chart on Canvas
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(modifier = Modifier.size(116.dp)) {
                        val strokeWidth = 16.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(diameter, diameter)

                        if (totalCards == 0) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        } else {
                            val newSweep = (newCardsCount.toFloat() / totalCards) * 360f
                            val learningSweep = (learningShortCount.toFloat() / totalCards) * 360f
                            val matureSweep = (matureCount.toFloat() / totalCards) * 360f

                            var startAngle = -90f
                            if (newSweep > 0f) {
                                drawArc(
                                    color = RateEasyColor,
                                    startAngle = startAngle,
                                    sweepAngle = newSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += newSweep
                            }
                            if (learningSweep > 0f) {
                                drawArc(
                                    color = RateHardColor,
                                    startAngle = startAngle,
                                    sweepAngle = learningSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += learningSweep
                            }
                            if (matureSweep > 0f) {
                                drawArc(
                                    color = RateGoodColor,
                                    startAngle = startAngle,
                                    sweepAngle = matureSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalCards",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "всего карт",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Legend & counts
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MaturityLegendRow(
                        color = RateEasyColor,
                        title = "Новые карточки",
                        subtitle = "Еще не изучались",
                        count = newCardsCount
                    )
                    MaturityLegendRow(
                        color = RateHardColor,
                        title = "Активное закрепление",
                        subtitle = "Интервал от 1 до 6 дней",
                        count = learningShortCount
                    )
                    MaturityLegendRow(
                        color = RateGoodColor,
                        title = "Долгосрочная память",
                        subtitle = "Интервал от 1 недели и выше",
                        count = matureCount
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricMiniBox(
                    label = "Средний интервал",
                    value = "$averageInterval дн.",
                    accent = AmbientGlowCyan
                )
                MetricMiniBox(
                    label = "В изучении",
                    value = "${learningShortCount + matureCount} шт.",
                    accent = RateGoodColor
                )
                MetricMiniBox(
                    label = "Готовы сегодня",
                    value = "${forecastBars.firstOrNull()?.count ?: 0} шт.",
                    accent = RateEasyColor
                )
            }
        }

        // Card 2: 7-Day Review Forecast Bar Chart
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .geminiAmbientCard(cornerRadius = 26.dp, isActive = false)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Прогноз повторений на 7 дней",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "В какие дни созреют карточки и придут автоматические уведомления",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val maxCount = remember(forecastBars) {
                (forecastBars.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                forecastBars.forEach { bar ->
                    val fraction = (bar.count.toFloat() / maxCount.toFloat()).coerceIn(0.08f, 1f)
                    val barColor = when {
                        bar.isToday && bar.count > 0 -> MaterialTheme.colorScheme.primary
                        bar.count > 0 -> RateGoodColor
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "${bar.count}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (bar.count > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (bar.count > 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .height(92.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(if (bar.count == 0) 0.08f else fraction)
                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(barColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bar.dayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (bar.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = bar.dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Card 3: Scientific Spaced Repetition Cadence Guide (Ebbinghaus & SM-2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .geminiAmbientCard(cornerRadius = 26.dp, isActive = false)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Как часто нужно повторять карточки?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Согласно кривой забывания Германа Эббингауза и алгоритму SuperMemo SM-2, оптимальное закрепление информации происходит при расширяющихся интервалах:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CadenceStepItem(
                stepBadge = "1-й повтор",
                intervalTitle = "В день изучения и через 1 день",
                description = "Останавливает резкое падение кривой памяти в первые 24 часа."
            )
            CadenceStepItem(
                stepBadge = "2-й повтор",
                intervalTitle = "Через 3 – 6 дней",
                description = "Переводит знание из кратковременной памяти в оперативную."
            )
            CadenceStepItem(
                stepBadge = "3-й повтор",
                intervalTitle = "Через 1 – 2 недели (7–14 дней)",
                description = "Формирует устойчивую нейронную связь без необходимости зубрежки."
            )
            CadenceStepItem(
                stepBadge = "4-й повтор",
                intervalTitle = "Через 1 месяц (30 дней) и далее",
                description = "Фиксирует карточку в долгосрочной памяти. Вы можете сдвинуть любую карточку на +1 дн, +3 дн, +1 нед или +1 мес вручную."
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun MaturityLegendRow(
    color: Color,
    title: String,
    subtitle: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CadenceStepItem(
    stepBadge: String,
    intervalTitle: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = stepBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = intervalTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun buildSevenDayForecast(allCards: List<CardEntity>, nowMillis: Long): List<DayForecastBar> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val endOfToday = cal.timeInMillis
    val dayFmt = SimpleDateFormat("EE", Locale.forLanguageTag("ru"))
    val dateFmt = SimpleDateFormat("d.MM", Locale.forLanguageTag("ru"))

    val bars = mutableListOf<DayForecastBar>()
    for (dayOffset in 0..6) {
        val dayEnd = endOfToday + dayOffset * Sm2Scheduler.MILLIS_PER_DAY
        val dayStart = dayEnd - Sm2Scheduler.MILLIS_PER_DAY + 1L

        val count = if (dayOffset == 0) {
            // Due today includes overdue + due up to end of today
            allCards.count { it.nextReviewDate <= endOfToday }
        } else {
            allCards.count { it.nextReviewDate in dayStart..dayEnd }
        }

        val label = if (dayOffset == 0) {
            "Сег"
        } else {
            dayFmt.format(Date(dayEnd)).replaceFirstChar { it.uppercase() }
        }

        bars.add(
            DayForecastBar(
                dayLabel = label,
                dateLabel = dateFmt.format(Date(dayEnd)),
                count = count,
                isToday = dayOffset == 0
            )
        )
    }
    return bars
}
