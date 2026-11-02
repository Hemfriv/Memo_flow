package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CardEntity
import com.example.domain.CustomIntervalPreset
import com.example.domain.ReviewRating
import com.example.domain.Sm2Scheduler
import com.example.ui.SessionStats
import com.example.ui.StudySessionUiState
import com.example.ui.theme.AmbientGlowBlue
import com.example.ui.theme.AmbientGlowCyan
import com.example.ui.theme.AmbientGlowViolet
import com.example.ui.theme.RateAgainColor
import com.example.ui.theme.RateEasyColor
import com.example.ui.theme.RateGoodColor
import com.example.ui.theme.RateHardColor
import com.example.ui.theme.geminiAmbientCard
import com.example.ui.theme.springScaleClickable
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

/**
 * Study Session Screen featuring:
 * - 3D Y-axis card flip with perspective (`cameraDistance`) and dynamic shadow/glow modulation
 * - Soft Fade & Scale (0.94 -> 1.0) transition when moving to the next card
 * - Spring Scale (0.97) touch feedback and native HapticFeedback
 * - 4 SM-2 rating buttons («Снова», «Трудно», «Хорошо», «Легко») with next interval previews
 * - Quick custom interval shift bar (+1 дн, +3 дн, +1 нед, +2 нед, +1 мес) for specific cards
 * - Session Complete summary screen with detailed statistics
 */
@Composable
fun StudyScreen(
    studyState: StudySessionUiState,
    hapticEnabled: Boolean,
    onFlipCard: () -> Unit,
    onRateCard: (ReviewRating) -> Unit,
    onCustomShiftDays: (Int) -> Unit,
    onRestartSession: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("study_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад к колодам",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = studyState.deckTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!studyState.isSessionComplete) {
                    Text(
                        text = "Осталось: ${studyState.remainingCount} • Повторы «Снова»: ${studyState.stats.reQueuedCardsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!studyState.isSessionComplete && studyState.queue.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${studyState.currentIndex + 1} / ${studyState.queue.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Smooth progress bar
        val animatedProgress by animateFloatAsState(
            targetValue = if (studyState.isSessionComplete) 1f else studyState.progressFraction,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            label = "study_progress"
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (studyState.isSessionComplete || studyState.currentCard == null) {
            SessionCompleteView(
                deckTitle = studyState.deckTitle,
                stats = studyState.stats,
                hapticEnabled = hapticEnabled,
                onRestartSession = onRestartSession,
                onBackToDecks = onNavigateBack
            )
        } else {
            val currentCard = studyState.currentCard!!
            val intervalPreviews = remember(
                currentCard.id,
                currentCard.repetitions,
                currentCard.interval,
                currentCard.easeFactor
            ) {
                Sm2Scheduler.previewNextIntervals(currentCard)
            }

            // Smooth Fade & Scale (0.94 -> 1.0) transition when switching to the next card
            AnimatedContent(
                targetState = studyState.cardTransitionKey to currentCard,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.94f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(240))) togetherWith
                        (scaleOut(targetScale = 0.96f, animationSpec = tween(160)) +
                            fadeOut(animationSpec = tween(160)))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                label = "next_card_fade_scale"
            ) { (_, card) ->
                Flippable3DFlashcard(
                    card = card,
                    isFlipped = studyState.isFlipped,
                    hapticEnabled = hapticEnabled,
                    onFlip = {
                        if (hapticEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        onFlipCard()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom area: either a tap hint or the 4 SM-2 rating buttons + quick custom interval bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .heightIn(min = 124.dp),
                contentAlignment = Alignment.Center
            ) {
                this@Column.AnimatedVisibility(
                    visible = !studyState.isFlipped,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    Row(
                        modifier = Modifier
                            .geminiAmbientCard(cornerRadius = 22.dp, isActive = false)
                            .springScaleClickable(
                                pressedScale = 0.97f,
                                hapticEnabled = hapticEnabled,
                                onClick = onFlipCard
                            )
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                            .testTag("flip_card_hint_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Показать ответ",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Нажмите на карточку, чтобы увидеть ответ",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                this@Column.AnimatedVisibility(
                    visible = studyState.isFlipped,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(tween(220)),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(tween(140))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Sm2RatingButtonsRow(
                            intervalPreviews = intervalPreviews,
                            hapticEnabled = hapticEnabled,
                            onSelectRating = { rating ->
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                }
                                onRateCard(rating)
                            }
                        )

                        // Quick Custom Shift Row (+1 дн, +3 дн, +1 нед, +2 нед, +1 мес)
                        QuickShiftIntervalBar(
                            hapticEnabled = hapticEnabled,
                            onSelectDays = { days ->
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                }
                                onCustomShiftDays(days)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact horizontal bar allowing the user to manually schedule a specific card
 * for +1 day, +3 days, +1 week, +2 weeks, or +1 month.
 */
@Composable
private fun QuickShiftIntervalBar(
    hapticEnabled: Boolean,
    onSelectDays: (Int) -> Unit
) {
    val presets = remember {
        listOf(
            CustomIntervalPreset.ONE_DAY,
            CustomIntervalPreset.THREE_DAYS,
            CustomIntervalPreset.ONE_WEEK,
            CustomIntervalPreset.TWO_WEEKS,
            CustomIntervalPreset.ONE_MONTH
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventRepeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Свой интервал:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        presets.forEach { preset ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .springScaleClickable(
                        pressedScale = 0.97f,
                        hapticEnabled = hapticEnabled,
                        onClick = { onSelectDays(preset.days) }
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("custom_shift_${preset.days}d")
            ) {
                Text(
                    text = preset.chipLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 3D Flashcard that rotates 180 degrees around the Y-axis with perspective
 * (`cameraDistance = 14f * density`, analogous to Matrix4 setEntry(3, 2, 0.001))
 * and dynamically shifts its ambient shadow during the flip.
 */
@Composable
private fun Flippable3DFlashcard(
    card: CardEntity,
    isFlipped: Boolean,
    hapticEnabled: Boolean,
    onFlip: () -> Unit
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_3d_rotation_y"
    )

    val density = LocalDensity.current.density
    val flipRadians = Math.toRadians(rotationY.toDouble())
    val midFlipLift = abs(sin(flipRadians)).toFloat()

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .springScaleClickable(
                pressedScale = 0.97f,
                hapticEnabled = hapticEnabled,
                onClick = onFlip
            )
            .testTag("study_flashcard_box")
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 14f * density
                val dynamicScale = 1f + (midFlipLift * 0.025f)
                this.scaleX = dynamicScale
                this.scaleY = dynamicScale
                this.shadowElevation = (8f + midFlipLift * 22f) * density
                this.shape = shape
                this.clip = false
            }
            .drawBehind {
                val glowAlpha = 0.07f + (midFlipLift * 0.14f)
                val horizontalShift = ((rotationY / 180f) - 0.5f) * size.width * 0.18f
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (rotationY <= 90f) AmbientGlowBlue.copy(alpha = glowAlpha)
                            else AmbientGlowViolet.copy(alpha = glowAlpha),
                            AmbientGlowCyan.copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(
                            x = (size.width * 0.5f) + horizontalShift,
                            y = size.height * (0.52f + midFlipLift * 0.06f)
                        ),
                        radius = size.maxDimension * (0.72f + midFlipLift * 0.15f)
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx())
                )
            }
            .clip(shape)
            .background(surfaceColor, shape)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = if (rotationY <= 90f) {
                        listOf(
                            primaryColor.copy(alpha = 0.45f),
                            AmbientGlowViolet.copy(alpha = 0.20f),
                            primaryColor.copy(alpha = 0.35f)
                        )
                    } else {
                        listOf(
                            secondaryColor.copy(alpha = 0.55f),
                            AmbientGlowCyan.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.45f)
                        )
                    }
                ),
                shape = shape
            )
    ) {
        if (rotationY <= 90f) {
            // FRONT SIDE (Question)
            FlashcardFaceContent(
                badgeText = "ВОПРОС • ${card.state.russianLabel.uppercase()}",
                badgeColor = MaterialTheme.colorScheme.primary,
                mainText = card.front,
                footerText = "EF: ${String.format(Locale.US, "%.2f", card.easeFactor)} • Повторений: ${card.repetitions} • Интервал: ${card.interval} дн.",
                isBackSide = false
            )
        } else {
            // BACK SIDE (Answer) — counter-rotated 180° around Y so text is not mirrored
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.rotationY = 180f
                    }
            ) {
                FlashcardFaceContent(
                    badgeText = "ОТВЕТ",
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    mainText = card.back,
                    questionContext = card.front,
                    footerText = "Оцените по SM-2 или выберите точный сдвиг (+дни/недели) ниже",
                    isBackSide = true
                )
            }
        }
    }
}

@Composable
private fun FlashcardFaceContent(
    badgeText: String,
    badgeColor: Color,
    mainText: String,
    questionContext: String? = null,
    footerText: String,
    isBackSide: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.Autorenew,
                contentDescription = "3D переворот карточки",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isBackSide && !questionContext.isNullOrBlank()) {
                Text(
                    text = questionContext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier
                        .width(80.dp)
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }

            Text(
                text = mainText,
                style = if (mainText.length > 140) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                fontWeight = if (isBackSide) FontWeight.Medium else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = footerText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Row of 4 SM-2 evaluation buttons («Снова», «Трудно», «Хорошо», «Легко»)
 * showing the estimated next review time below each button.
 */
@Composable
private fun Sm2RatingButtonsRow(
    intervalPreviews: Map<ReviewRating, String>,
    hapticEnabled: Boolean,
    onSelectRating: (ReviewRating) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ReviewRating.entries.forEach { rating ->
            val accentColor = when (rating) {
                ReviewRating.AGAIN -> RateAgainColor
                ReviewRating.HARD -> RateHardColor
                ReviewRating.GOOD -> RateGoodColor
                ReviewRating.EASY -> RateEasyColor
            }
            val intervalText = intervalPreviews[rating] ?: ""

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.40f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .springScaleClickable(
                        pressedScale = 0.97f,
                        hapticEnabled = hapticEnabled,
                        onClick = { onSelectRating(rating) }
                    )
                    .padding(vertical = 12.dp, horizontal = 6.dp)
                    .testTag("rating_btn_${rating.name.lowercase()}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = rating.russianLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = intervalText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Celebratory Session Complete summary displaying SM-2 accuracy and rating breakdown.
 */
@Composable
private fun SessionCompleteView(
    deckTitle: String,
    stats: SessionStats,
    hapticEnabled: Boolean,
    onRestartSession: () -> Unit,
    onBackToDecks: () -> Unit
) {
    val elapsedSeconds = ((System.currentTimeMillis() - stats.startedAtMillis) / 1000L).coerceAtLeast(1L)
    val recallRate = if (stats.totalReviews > 0) {
        (((stats.goodCount + stats.easyCount + stats.hardCount).toFloat() / stats.totalReviews) * 100).toInt()
    } else 100

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 540.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Сессия завершена",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Сессия завершена!",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Колода «$deckTitle» полностью проработана на сегодня по алгоритму SM-2.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .geminiAmbientCard(cornerRadius = 26.dp, isActive = true)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetricColumn(label = "Ответов всего", value = "${stats.totalReviews}")
                StatMetricColumn(label = "Точность памяти", value = "$recallRate%")
                StatMetricColumn(label = "Время сессии", value = "${elapsedSeconds} сек")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Text(
                text = "Распределение ответов SM-2",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Снова",
                    count = stats.againCount,
                    color = RateAgainColor
                )
                RatingStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Трудно",
                    count = stats.hardCount,
                    color = RateHardColor
                )
                RatingStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Хорошо",
                    count = stats.goodCount,
                    color = RateGoodColor
                )
                RatingStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Легко",
                    count = stats.easyCount,
                    color = RateEasyColor
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .geminiAmbientCard(cornerRadius = 20.dp, isActive = false)
                    .springScaleClickable(
                        pressedScale = 0.97f,
                        hapticEnabled = hapticEnabled,
                        onClick = onRestartSession
                    )
                    .padding(vertical = 16.dp)
                    .testTag("session_restart_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Проверить еще",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .springScaleClickable(
                        pressedScale = 0.97f,
                        hapticEnabled = hapticEnabled,
                        onClick = onBackToDecks
                    )
                    .padding(vertical = 16.dp)
                    .testTag("session_done_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "К списку колод",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun StatMetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RatingStatPill(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
