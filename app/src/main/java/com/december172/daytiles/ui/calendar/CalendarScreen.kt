package com.december172.daytiles.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.december172.daytiles.R
import com.december172.daytiles.domain.model.DayContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun CalendarRoute(
    onOpenDay: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        uiState = uiState,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onToday = viewModel::showToday,
        onDateSelected = viewModel::selectDate,
        onOpenDay = onOpenDay,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenDay(uiState.selectedDate) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(bottom = 4.dp),
        ) {
            MonthHeader(
                month = uiState.visibleMonth,
                selectedDate = uiState.selectedDate,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onToday = onToday,
            )
            WeekdayHeader()
            MonthGrid(
                uiState = uiState,
                onDateSelected = onDateSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthName = remember(month, locale) {
        month.month.getDisplayName(TextStyle.FULL, locale)
    }
    val monthTitle = stringResource(R.string.month_title, monthName, month.year)
    val selectedLabel = remember(selectedDate, locale) {
        selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(
                label = stringResource(R.string.previous_month),
                symbol = "‹",
                onClick = onPreviousMonth,
            )
            Text(
                text = monthTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            HeaderIconButton(
                label = stringResource(R.string.next_month),
                symbol = "›",
                onClick = onNextMonth,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.selected_date, selectedLabel),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.today))
            }
        }
    }
}

@Composable
private fun HeaderIconButton(label: String, symbol: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = label },
    ) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun WeekdayHeader() {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        orderedWeekdays().forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, locale),
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                color = if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    uiState: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(uiState.visibleMonth) { buildMonthWeeks(uiState.visibleMonth) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                week.forEach { date ->
                    if (date == null) {
                        EmptyDayCell(modifier = Modifier.weight(1f))
                    } else {
                        DayCell(
                            date = date,
                            content = uiState.contentByDate[date],
                            isToday = date == uiState.today,
                            isSelected = date == uiState.selectedDate,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)),
    )
}

@Composable
private fun DayCell(
    date: LocalDate,
    content: DayContent?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateDescription = remember(date, locale) {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    val border = when {
        isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        isToday -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
        else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = dateDescription },
        shape = RoundedCornerShape(2.dp),
        color = containerColor,
        border = border,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 76.dp
            val spacious = maxHeight >= 98.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateNumber(date.dayOfMonth, isToday)
                    Spacer(modifier = Modifier.weight(1f))
                    content?.stickers?.take(if (compact) 1 else 2)?.forEach { sticker ->
                        val stickerDescription = stringResource(
                            R.string.sticker_accessibility,
                            sticker.symbol,
                        )
                        Text(
                            text = sticker.symbol,
                            modifier = Modifier.semantics {
                                contentDescription = stickerDescription
                            },
                            fontSize = if (compact) 12.sp else 15.sp,
                        )
                    }
                }
                if (content != null) {
                    DayPreview(content = content, compact = compact, spacious = spacious)
                }
            }
        }
    }
}

@Composable
private fun DateNumber(dayOfMonth: Int, isToday: Boolean) {
    Box(
        modifier = Modifier
            .size(23.dp)
            .then(
                if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayOfMonth.toString(),
            color = if (isToday) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DayPreview(content: DayContent, compact: Boolean, spacious: Boolean) {
    if (compact) {
        val summary = content.tasks.firstOrNull()?.title ?: content.journal?.text
        if (summary != null) {
            Text(
                text = summary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    content.tasks.take(if (spacious) 2 else 1).forEach { task ->
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = if (task.isCompleted) "✓" else "□",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                lineHeight = 12.sp,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = task.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                fontSize = 9.sp,
                lineHeight = 12.sp,
            )
        }
    }
    if (content.tasks.isEmpty() || spacious) {
        content.journal?.let { journal ->
            Text(
                text = journal.text,
                maxLines = if (spacious) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

private fun orderedWeekdays(): List<DayOfWeek> =
    (0 until DayOfWeek.entries.size).map { offset -> DayOfWeek.MONDAY.plus(offset.toLong()) }
