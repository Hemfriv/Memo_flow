package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.RateEasyColor
import com.example.ui.theme.RateGoodColor
import com.example.ui.theme.geminiAmbientCard
import java.util.Calendar
import java.util.Locale

/**
 * Screen for configuring automatic background daily notifications, theme mode (Light/Dark/System),
 * and native haptic feedback — without any manual "send notification" buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderSettingsScreen(
    preferences: UserPreferences,
    dueTodayCount: Int,
    onUpdateReminder: (Boolean, Int, Int) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    var selectedHour by remember(preferences.reminderHour) {
        mutableIntStateOf(preferences.reminderHour)
    }
    var selectedMinute by remember(preferences.reminderMinute) {
        mutableIntStateOf(preferences.reminderMinute)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Request notification permission automatically on Android 13+ when reminders are enabled
    LaunchedEffect(preferences.reminderEnabled) {
        if (preferences.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val nextCheckDescription = remember(preferences.reminderEnabled, selectedHour, selectedMinute) {
        if (!preferences.reminderEnabled) {
            "Автоматические уведомления приостановлены"
        } else {
            val nowCal = Calendar.getInstance()
            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
            }
            val dayWord = if (targetCal.after(nowCal)) "Сегодня" else "Завтра"
            val timeFormatted = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
            "Следующая автоматическая проверка: $dayWord в $timeFormatted"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
        ) {
            Text(
                text = "Автоматические напоминания и Тема",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Приложение само отслеживает сроки карточек в фоне и присылает уведомление раз в день только тогда, когда подошел день повтора.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Automatic Daily Reminder Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .geminiAmbientCard(cornerRadius = 26.dp, isActive = preferences.reminderEnabled)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ежедневное авто-напоминание",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Работает в фоне через системный планировщик",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = preferences.reminderEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateReminder(enabled, selectedHour, selectedMinute)
                    },
                    modifier = Modifier.testTag("reminder_enable_switch")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // Quick time presets (Утро 09:00, День 14:00, Вечер 20:00)
            Text(
                text = "Удобное время для ежедневных занятий:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val timePresets = listOf(
                    Triple(9, 0, "Утро (09:00)"),
                    Triple(14, 0, "День (14:00)"),
                    Triple(19, 0, "Вечер (19:00)"),
                    Triple(21, 0, "Поздний вечер (21:00)")
                )
                timePresets.forEach { (h, m, label) ->
                    FilterChip(
                        selected = selectedHour == h && selectedMinute == m,
                        onClick = {
                            selectedHour = h
                            selectedMinute = m
                            onUpdateReminder(preferences.reminderEnabled, h, m)
                        },
                        label = { Text(label) }
                    )
                }
            }

            // Fine-grained Time selector (Hour & Minute step controls)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Точное время:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeStepperBox(
                        value = selectedHour,
                        label = "ЧЧ",
                        onDecrement = {
                            selectedHour = (selectedHour + 23) % 24
                            onUpdateReminder(preferences.reminderEnabled, selectedHour, selectedMinute)
                        },
                        onIncrement = {
                            selectedHour = (selectedHour + 1) % 24
                            onUpdateReminder(preferences.reminderEnabled, selectedHour, selectedMinute)
                        }
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TimeStepperBox(
                        value = selectedMinute,
                        label = "ММ",
                        onDecrement = {
                            selectedMinute = (selectedMinute + 45) % 60
                            onUpdateReminder(preferences.reminderEnabled, selectedHour, selectedMinute)
                        },
                        onIncrement = {
                            selectedMinute = (selectedMinute + 15) % 60
                            onUpdateReminder(preferences.reminderEnabled, selectedHour, selectedMinute)
                        }
                    )
                }
            }

            // Live Automatic Status Card (No manual button!)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (preferences.reminderEnabled) RateGoodColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = nextCheckDescription,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (dueTodayCount > 0) {
                            "На сегодня ждут повторения: $dueTodayCount карт. В назначенное время приложение автоматически пришлет напоминание."
                        } else {
                            "На сегодня все карточки изучены. Если к моменту проверки новых карточек не появится, приложение не потревожит вас лишним уведомлением."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // How smart notifications work with card intervals
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .geminiAmbientCard(cornerRadius = 26.dp, isActive = false)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = RateEasyColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Как работают интервалы и уведомления",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "• Когда вы оцениваете карточку по SM-2 или вручную сдвигаете ее на +1 день, +3 дня, +1 неделю или +1 месяц, в базе сохраняется точная дата следующего показа.\n• Раз в сутки в выбранный вами час приложение автоматически проверяет созревшие карточки и присылает одно сводное уведомление.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Theme & Haptics Card
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
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Тема оформления (Gemini Expressive)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = preferences.themeMode == mode,
                        onClick = { onSelectThemeMode(mode) },
                        label = { Text(mode.russianLabel) },
                        modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Тактильный отклик (Haptic Feedback)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Вибрация при 3D перевороте карточки и выборе интервала",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = preferences.hapticEnabled,
                    onCheckedChange = onToggleHaptic,
                    modifier = Modifier.testTag("haptic_enable_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun TimeStepperBox(
    value: Int,
    label: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(34.dp)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(34.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
