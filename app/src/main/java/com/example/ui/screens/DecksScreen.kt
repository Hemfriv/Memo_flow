package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DeckWithCounts
import com.example.domain.AnkiDelimiter
import com.example.domain.AnkiTxtParser
import com.example.ui.theme.AmbientGlowBlue
import com.example.ui.theme.AmbientGlowCyan
import com.example.ui.theme.AmbientGlowViolet
import com.example.ui.theme.RateEasyColor
import com.example.ui.theme.RateGoodColor
import com.example.ui.theme.RateHardColor
import com.example.ui.theme.geminiAmbientCard
import com.example.ui.theme.springScaleClickable
import kotlinx.coroutines.launch

/**
 * Main Decks Screen showing:
 * - Gemini Ambient Hero Banner with global counters (Новые / На повторение сегодня) and "Учить все" action
 * - Quick actions for "Импорт Anki TXT" and "Новая колода"
 * - List of Decks with individual counters (Новые / На повторение сегодня / Всего)
 */
@Composable
fun DecksScreen(
    decks: List<DeckWithCounts>,
    hapticEnabled: Boolean,
    onStartDeckStudy: (Long?, String) -> Unit,
    onOpenDeckBrowser: (Long, String) -> Unit,
    onCreateDeck: (String) -> Unit,
    onDeleteDeck: (Long, String) -> Unit,
    onImportAnkiCards: (Long?, String, List<Pair<String, String>>) -> Unit,
    onReadAnkiUri: suspend (android.net.Uri, AnkiDelimiter) -> Pair<String, com.example.domain.AnkiParseResult>
) {
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var showAnkiImportDialog by remember { mutableStateOf(false) }

    val totalNew = remember(decks) { decks.sumOf { it.newCount } }
    val totalDueReview = remember(decks) { decks.sumOf { it.dueCount } }
    val totalToStudyToday = totalNew + totalDueReview

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                GeminiHeroSummaryCard(
                    totalNew = totalNew,
                    totalDueReview = totalDueReview,
                    totalDecks = decks.size,
                    hapticEnabled = hapticEnabled,
                    onStudyAllToday = {
                        onStartDeckStudy(null, "Все колоды на сегодня")
                    },
                    onOpenAnkiImport = { showAnkiImportDialog = true },
                    onOpenCreateDeck = { showCreateDeckDialog = true }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ваши колоды (${decks.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "К повторению: $totalToStudyToday",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (decks.isEmpty()) {
                item {
                    EmptyDecksPlaceholder(
                        hapticEnabled = hapticEnabled,
                        onCreateDeck = { showCreateDeckDialog = true },
                        onImportAnki = { showAnkiImportDialog = true }
                    )
                }
            } else {
                items(items = decks, key = { it.id }) { deck ->
                    DeckCardItem(
                        deck = deck,
                        hapticEnabled = hapticEnabled,
                        onStudyClick = { onStartDeckStudy(deck.id, deck.name) },
                        onBrowseClick = { onOpenDeckBrowser(deck.id, deck.name) },
                        onDeleteClick = { onDeleteDeck(deck.id, deck.name) }
                    )
                }
            }
        }
    }

    if (showCreateDeckDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDeckDialog = false },
            onConfirm = { name ->
                onCreateDeck(name)
                showCreateDeckDialog = false
            }
        )
    }

    if (showAnkiImportDialog) {
        AnkiTxtImportDialog(
            decks = decks,
            hapticEnabled = hapticEnabled,
            onDismiss = { showAnkiImportDialog = false },
            onReadAnkiUri = onReadAnkiUri,
            onConfirmImport = { targetDeckId, newDeckName, parsedPairs ->
                onImportAnkiCards(targetDeckId, newDeckName, parsedPairs)
                showAnkiImportDialog = false
            }
        )
    }
}

@Composable
private fun GeminiHeroSummaryCard(
    totalNew: Int,
    totalDueReview: Int,
    totalDecks: Int,
    hapticEnabled: Boolean,
    onStudyAllToday: () -> Unit,
    onOpenAnkiImport: () -> Unit,
    onOpenCreateDeck: () -> Unit
) {
    val totalToday = totalNew + totalDueReview

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .geminiAmbientCard(cornerRadius = 28.dp, isActive = true)
    ) {
        // Decorative Gemini ambient light orbs drawn via Canvas
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(28.dp))
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AmbientGlowBlue.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.22f),
                    radius = size.minDimension * 0.75f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.22f),
                radius = size.minDimension * 0.75f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AmbientGlowViolet.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.85f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.85f),
                radius = size.minDimension * 0.65f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "АЛГОРИТМ SUPERMEMO SM-2 • ОФЛАЙН",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "SQLite WAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = if (totalToday > 0) {
                    "Готово к сессии: $totalToday карт."
                } else {
                    "Все карточки на сегодня изучены!"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Counters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroCounterPill(
                    modifier = Modifier.weight(1f),
                    label = "Новые",
                    count = totalNew,
                    accentColor = RateEasyColor
                )
                HeroCounterPill(
                    modifier = Modifier.weight(1f),
                    label = "На повторение",
                    count = totalDueReview,
                    accentColor = RateGoodColor
                )
                HeroCounterPill(
                    modifier = Modifier.weight(1f),
                    label = "Колод",
                    count = totalDecks,
                    accentColor = AmbientGlowCyan
                )
            }

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .springScaleClickable(
                            pressedScale = 0.97f,
                            hapticEnabled = hapticEnabled,
                            onClick = onStudyAllToday
                        )
                        .padding(vertical = 14.dp, horizontal = 12.dp)
                        .testTag("hero_study_all_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Учить сегодня ($totalToday)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        )
                        .springScaleClickable(
                            pressedScale = 0.97f,
                            hapticEnabled = hapticEnabled,
                            onClick = onOpenAnkiImport
                        )
                        .padding(vertical = 14.dp, horizontal = 10.dp)
                        .testTag("hero_import_anki_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Импорт TXT",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        )
                        .springScaleClickable(
                            pressedScale = 0.97f,
                            hapticEnabled = hapticEnabled,
                            onClick = onOpenCreateDeck
                        )
                        .padding(vertical = 14.dp, horizontal = 14.dp)
                        .testTag("hero_create_deck_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Новая колода",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCounterPill(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accentColor.copy(alpha = 0.11f))
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Individual Deck Card with New / Due Today counters and Spring Scale (0.97) touch feedback.
 */
@Composable
private fun DeckCardItem(
    deck: DeckWithCounts,
    hapticEnabled: Boolean,
    onStudyClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val actionableCount = deck.newCount + deck.dueCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .geminiAmbientCard(cornerRadius = 26.dp, isActive = actionableCount > 0)
            .springScaleClickable(
                pressedScale = 0.97f,
                hapticEnabled = hapticEnabled,
                onClick = onStudyClick
            )
            .padding(20.dp)
            .testTag("deck_card_${deck.id}"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Всего карточек: ${deck.totalCards}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBrowseClick,
                    modifier = Modifier.testTag("browse_deck_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Список карточек колоды",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_deck_${deck.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Удалить колоду",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Counters row: Новые / В изучении / На повторение сегодня + Учить button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CounterBadge(
                    label = "Новые",
                    count = deck.newCount,
                    color = RateEasyColor
                )
                if (deck.learningCount > 0) {
                    CounterBadge(
                        label = "Учатся",
                        count = deck.learningCount,
                        color = RateHardColor
                    )
                }
                CounterBadge(
                    label = "На сегодня",
                    count = deck.dueCount,
                    color = RateGoodColor
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (actionableCount > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .springScaleClickable(
                        pressedScale = 0.97f,
                        hapticEnabled = hapticEnabled,
                        onClick = onStudyClick
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("study_deck_btn_${deck.id}")
            ) {
                Text(
                    text = if (actionableCount > 0) "Учить ($actionableCount)" else "Готово",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (actionableCount > 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CounterBadge(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyDecksPlaceholder(
    hapticEnabled: Boolean,
    onCreateDeck: () -> Unit,
    onImportAnki: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .geminiAmbientCard(cornerRadius = 26.dp, isActive = false)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Style,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = "Пока нет колод",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Создайте собственную колоду или импортируйте готовый текстовый файл Anki (.txt / .csv / .tsv) с поддержкой многострочных полей.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCreateDeck) {
                Text("Создать колоду")
            }
            TextButton(onClick = onImportAnki) {
                Text("Импорт Anki TXT")
            }
        }
    }
}

@Composable
private fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая колода") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Введите название колоды для интервального повторения:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название колоды") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_deck_name_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_deck_button")
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Interactive Anki TXT Import Dialog supporting:
 * - Local file picker (`ActivityResultContracts.OpenDocument`) for `.txt`, `.csv`, `.tsv`
 * - One-tap demo Anki TXT template with `#` comments and multiline double-quoted fields
 * - Delimiter selector (`Auto`, `\t`, `;`, `,`)
 * - Live parser diagnostics (parsed card count, detected delimiter, skipped `#` comment lines)
 * - Atomic SQLite Batch Insert into a new or existing deck
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnkiTxtImportDialog(
    decks: List<DeckWithCounts>,
    hapticEnabled: Boolean,
    onDismiss: () -> Unit,
    onReadAnkiUri: suspend (android.net.Uri, AnkiDelimiter) -> Pair<String, com.example.domain.AnkiParseResult>,
    onConfirmImport: (Long?, String, List<Pair<String, String>>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var targetDeckId by remember { mutableStateOf<Long?>(null) } // null = create new deck
    var newDeckName by remember { mutableStateOf("Импорт Anki: Сети и Протоколы") }
    var selectedDelimiter by remember { mutableStateOf(AnkiDelimiter.AUTO) }
    var rawTextContent by remember { mutableStateOf(AnkiTxtParser.SAMPLE_ANKI_TXT) }

    val parseResult = remember(rawTextContent, selectedDelimiter) {
        AnkiTxtParser.parse(rawTextContent, selectedDelimiter)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val (loadedText, _) = onReadAnkiUri(uri, selectedDelimiter)
                if (loadedText.isNotBlank()) {
                    rawTextContent = loadedText
                    val lastSegment = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                    if (!lastSegment.isNullOrBlank()) {
                        newDeckName = "Импорт: $lastSegment"
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Импорт Anki TXT (Batch SQLite)",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // File picker & Sample loader buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .springScaleClickable(
                                pressedScale = 0.97f,
                                hapticEnabled = hapticEnabled,
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("text/plain", "text/csv", "text/tab-separated-values", "*/*")
                                    )
                                }
                            )
                            .padding(vertical = 12.dp, horizontal = 10.dp)
                            .testTag("pick_anki_file_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Выбрать .TXT файл",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .springScaleClickable(
                                pressedScale = 0.97f,
                                hapticEnabled = hapticEnabled,
                                onClick = {
                                    rawTextContent = AnkiTxtParser.SAMPLE_ANKI_TXT
                                    newDeckName = "Импорт Anki: Сети и Протоколы"
                                }
                            )
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .testTag("load_sample_anki_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Демо-шаблон",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Target deck selection
                Text(
                    text = "Куда импортировать:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = targetDeckId == null,
                        onClick = { targetDeckId = null },
                        label = { Text("В новую колоду") }
                    )
                    decks.take(3).forEach { deck ->
                        FilterChip(
                            selected = targetDeckId == deck.id,
                            onClick = { targetDeckId = deck.id },
                            label = {
                                Text(
                                    text = deck.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                if (targetDeckId == null) {
                    OutlinedTextField(
                        value = newDeckName,
                        onValueChange = { newDeckName = it },
                        label = { Text("Название новой колоды") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Delimiter selection
                Text(
                    text = "Разделитель полей:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnkiDelimiter.entries.forEach { delim ->
                        FilterChip(
                            selected = selectedDelimiter == delim,
                            onClick = { selectedDelimiter = delim },
                            label = { Text(delim.russianLabel) }
                        )
                    }
                }

                // Raw Anki content editor / preview
                OutlinedTextField(
                    value = rawTextContent,
                    onValueChange = { rawTextContent = it },
                    label = { Text("Содержимое файла Anki TXT (поддержка # и \"...\")") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp, max = 180.dp)
                        .testTag("anki_raw_text_input"),
                    textStyle = MaterialTheme.typography.labelSmall
                )

                // Live parser diagnostics box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val delimName = when (parseResult.detectedDelimiter) {
                            '\t' -> "Табуляция (\\t)"
                            ';' -> "Точка с запятой (;)"
                            ',' -> "Запятая (,)"
                            else -> "'${parseResult.detectedDelimiter}'"
                        }
                        Text(
                            text = "Распознано карточек: ${parseResult.cards.size} шт.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Разделитель: $delimName • Пропущено строк #: ${parseResult.skippedCommentLines}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (parseResult.cards.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            val sample = parseResult.cards.first()
                            Text(
                                text = "Пример #1 — Вопрос: ${sample.first.replace("\n", " ")}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ответ: ${sample.second.replace("\n", " ↵ ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmImport(targetDeckId, newDeckName, parseResult.cards)
                },
                enabled = parseResult.cards.isNotEmpty(),
                modifier = Modifier.testTag("confirm_anki_import_button")
            ) {
                Text("Импортировать (${parseResult.cards.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
