package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FlashcardRepository
import com.example.notifications.ReminderScheduler
import com.example.ui.AppScreen
import com.example.ui.MemoFlowViewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DeckCardsBrowserScreen
import com.example.ui.screens.DecksScreen
import com.example.ui.screens.ReminderSettingsScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.MemoFlowTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ReminderScheduler.createNotificationChannel(applicationContext)
        val database = AppDatabase.getInstance(applicationContext)
        val repository = FlashcardRepository(database.flashcardDao())
        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val viewModel: MemoFlowViewModel = viewModel(
                factory = MemoFlowViewModel.Factory(repository, preferencesRepository)
            )
            val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Ensure system status bar icons (time, battery, network) always contrast properly
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                    insetsController.isAppearanceLightNavigationBars = !useDarkTheme
                }
            }

            // Automatically register the background daily reminder schedule on launch
            LaunchedEffect(preferences.reminderEnabled, preferences.reminderHour, preferences.reminderMinute) {
                ReminderScheduler.scheduleDailyReminder(
                    context = applicationContext,
                    enabled = preferences.reminderEnabled,
                    hour = preferences.reminderHour,
                    minute = preferences.reminderMinute
                )
            }

            MemoFlowTheme(darkTheme = useDarkTheme) {
                MemoFlowAppRoot(
                    viewModel = viewModel,
                    useDarkTheme = useDarkTheme
                )
            }
        }
    }
}

@Composable
fun MemoFlowAppRoot(
    viewModel: MemoFlowViewModel,
    useDarkTheme: Boolean
) {
    val context = LocalContext.current
    val decks by viewModel.decksState.collectAsStateWithLifecycle()
    val allCards by viewModel.allCardsState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val studyState by viewModel.studyState.collectAsStateWithLifecycle()
    val browserCards by viewModel.browserDeckCards.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(3400)
            viewModel.dismissBanner()
        }
    }

    val totalDueToday = decks.sumOf { it.newCount + it.dueCount }
    val showBottomBar = currentScreen is AppScreen.DecksHome ||
        currentScreen is AppScreen.Analytics ||
        currentScreen is AppScreen.ReminderSettings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            if (showBottomBar) {
                MemoFlowTopAppBar(
                    useDarkTheme = useDarkTheme,
                    onToggleQuickTheme = {
                        val nextMode = if (useDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK
                        viewModel.setThemeMode(nextMode)
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.DecksHome,
                        onClick = { viewModel.navigateTo(AppScreen.DecksHome) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is AppScreen.DecksHome) {
                                    Icons.Filled.Style
                                } else {
                                    Icons.Outlined.Style
                                },
                                contentDescription = stringResource(R.string.tab_decks)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_decks)) },
                        modifier = Modifier.testTag("nav_tab_decks")
                    )

                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Analytics,
                        onClick = { viewModel.navigateTo(AppScreen.Analytics) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is AppScreen.Analytics) {
                                    Icons.Filled.AutoGraph
                                } else {
                                    Icons.Outlined.AutoGraph
                                },
                                contentDescription = stringResource(R.string.tab_analytics)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_analytics)) },
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )

                    NavigationBarItem(
                        selected = currentScreen is AppScreen.ReminderSettings,
                        onClick = { viewModel.navigateTo(AppScreen.ReminderSettings) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is AppScreen.ReminderSettings) {
                                    Icons.Filled.Notifications
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                contentDescription = stringResource(R.string.tab_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_settings)) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is AppScreen.DecksHome -> {
                    DecksScreen(
                        decks = decks,
                        hapticEnabled = preferences.hapticEnabled,
                        onStartDeckStudy = { deckId, deckTitle ->
                            viewModel.startStudySession(deckId, deckTitle)
                        },
                        onOpenDeckBrowser = { deckId, deckTitle ->
                            viewModel.navigateTo(AppScreen.DeckCardsBrowser(deckId, deckTitle))
                        },
                        onCreateDeck = { name -> viewModel.createDeck(name) },
                        onDeleteDeck = { deckId, name -> viewModel.deleteDeck(deckId, name) },
                        onImportAnkiCards = { targetDeckId, newDeckName, cards ->
                            viewModel.importParsedAnkiCards(targetDeckId, newDeckName, cards)
                        },
                        onReadAnkiUri = { uri, delimiter ->
                            viewModel.readAndPreviewAnkiUri(context, uri, delimiter)
                        }
                    )
                }

                is AppScreen.StudySession -> {
                    StudyScreen(
                        studyState = studyState,
                        hapticEnabled = preferences.hapticEnabled,
                        onFlipCard = { viewModel.flipCurrentCard() },
                        onRateCard = { rating -> viewModel.submitCardRating(rating) },
                        onCustomShiftDays = { days ->
                            viewModel.scheduleCurrentStudyCardCustomDays(days)
                        },
                        onRestartSession = {
                            viewModel.startStudySession(screen.deckId, screen.deckTitle)
                        },
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DecksHome) }
                    )
                }

                is AppScreen.DeckCardsBrowser -> {
                    DeckCardsBrowserScreen(
                        deckId = screen.deckId,
                        deckTitle = screen.deckTitle,
                        cards = browserCards,
                        hapticEnabled = preferences.hapticEnabled,
                        onAddCard = { id, front, back, initialDays ->
                            viewModel.addCardToDeck(id, front, back, initialDays)
                        },
                        onScheduleCardCustomDays = { card, days ->
                            viewModel.scheduleCardCustomDays(card, days)
                        },
                        onDeleteCard = { card -> viewModel.deleteCard(card) },
                        onStartStudy = {
                            viewModel.startStudySession(screen.deckId, screen.deckTitle)
                        },
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DecksHome) }
                    )
                }

                is AppScreen.Analytics -> {
                    AnalyticsScreen(
                        allCards = allCards,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DecksHome) }
                    )
                }

                is AppScreen.ReminderSettings -> {
                    ReminderSettingsScreen(
                        preferences = preferences,
                        dueTodayCount = totalDueToday,
                        onUpdateReminder = { enabled, hour, minute ->
                            viewModel.updateReminderSchedule(context, enabled, hour, minute)
                        },
                        onSelectThemeMode = { mode -> viewModel.setThemeMode(mode) },
                        onToggleHaptic = { enabled -> viewModel.setHapticEnabled(enabled) },
                        onNavigateBack = { viewModel.navigateTo(AppScreen.DecksHome) }
                    )
                }
            }

            // Floating status toast banner
            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .clickable { viewModel.dismissBanner() }
                        .testTag("status_banner_toast")
                ) {
                    Text(
                        text = bannerMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top app bar with explicit `statusBarsPadding()`, distinct surface background, and bottom border
 * so it cleanly separates from the system status bar without overlapping or blending.
 */
@Composable
private fun MemoFlowTopAppBar(
    useDarkTheme: Boolean,
    onToggleQuickTheme: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.subtitle_spaced_repetition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleQuickTheme,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("quick_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (useDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Переключить тему оформления",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
        }
    }
}
