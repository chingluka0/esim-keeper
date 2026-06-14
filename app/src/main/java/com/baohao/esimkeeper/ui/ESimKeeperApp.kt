package com.baohao.esimkeeper.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baohao.esimkeeper.R
import com.baohao.esimkeeper.data.CardSortOrder
import com.baohao.esimkeeper.data.Countries
import com.baohao.esimkeeper.data.CountryOption
import com.baohao.esimkeeper.data.DeviceSubscriptionInfo
import com.baohao.esimkeeper.data.DeviceSubscriptionReader
import com.baohao.esimkeeper.data.ESimCard
import com.baohao.esimkeeper.domain.ExpiryCalculator
import com.baohao.esimkeeper.domain.ExpiryStatus
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

private enum class CardFilter(@StringRes val labelRes: Int) {
    All(R.string.filter_all),
    Warning(R.string.filter_warning),
    Expired(R.string.filter_expired),
    LongTerm(R.string.filter_long_term),
}

private fun Context.preferredLocale(): Locale =
    resources.configuration.locales[0] ?: Locale.getDefault()

private fun Context.isChineseLanguage(): Boolean =
    preferredLocale().language == Locale.CHINESE.language

private fun LocalDate.formatForLocale(context: Context): String =
    format(DateTimeFormatter.ofPattern(context.getString(R.string.date_pattern), context.preferredLocale()))

@Composable
fun ESimKeeperApp(viewModel: MainViewModel) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selectedFilter by remember { mutableStateOf(CardFilter.All) }
    var showDonationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            today = LocalDate.now()
        }
    }

    val query = viewModel.searchQuery.trim()
    val filteredCards = remember(cards, query, selectedFilter, today) {
        cards.filter { card ->
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                card.name.contains(query, ignoreCase = true) ||
                    card.phoneNumber.contains(query, ignoreCase = true) ||
                    card.countryName.contains(query, ignoreCase = true) ||
                    card.balanceText.contains(query, ignoreCase = true)
            }
            val status = ExpiryCalculator.status(card.startDate, card.expiryDate, today)
            val matchesFilter = when (selectedFilter) {
                CardFilter.All -> true
                CardFilter.Warning -> status.isWarning
                CardFilter.Expired -> status.isExpired
                CardFilter.LongTerm -> card.cycleDays != null
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openAdd,
                shape = CircleShape,
                containerColor = KeeperBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(padding)
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 20.dp),
        ) {
            HomeHeader(
                cardCount = cards.size,
                isDarkMode = viewModel.isDarkMode,
                sortOrder = sortOrder,
                onToggleTheme = viewModel::toggleDarkMode,
                onOpenDonation = { showDonationDialog = true },
                onSelectSortOrder = viewModel::setSortOrder,
            )
            SearchField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.card_count, filteredCards.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCards.isEmpty()) {
                EmptyState(hasCards = cards.isNotEmpty(), modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp,
                    ),
                ) {
                    items(filteredCards, key = { it.id }) { card ->
                        ESimCardItem(
                            card = card,
                            today = today,
                            onEdit = { viewModel.openEdit(card) },
                            onDelete = { viewModel.deleteCard(card) },
                            onRenew = { viewModel.renewCard(card, today) },
                        )
                    }
                }
            }
        }

        if (viewModel.isEditorOpen) {
            ESimEditorDialog(
                card = viewModel.editorTarget,
                onClose = viewModel::closeEditor,
                onSave = viewModel::saveCard,
            )
        }

        if (showDonationDialog) {
            DonationDialog(onDismiss = { showDonationDialog = false })
        }
    }
}

@Composable
private fun HomeHeader(
    cardCount: Int,
    isDarkMode: Boolean,
    sortOrder: CardSortOrder,
    onToggleTheme: () -> Unit,
    onOpenDonation: () -> Unit,
    onSelectSortOrder: (CardSortOrder) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (cardCount == 0) {
                    stringResource(R.string.app_subtitle_empty)
                } else {
                    stringResource(R.string.app_subtitle_count, cardCount)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                GlassSurface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showSortMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.sort_menu_content_description),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    CardSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(stringResource(order.labelRes())) },
                            leadingIcon = {
                                if (order == sortOrder) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = KeeperBlue,
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOrder(order)
                                showSortMenu = false
                            },
                        )
                    }
                }
            }
            GlassSurface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onOpenDonation),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = KeeperRed,
                    )
                }
            }
            GlassSurface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isDarkMode) "☼" else "☾",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@StringRes
private fun CardSortOrder.labelRes(): Int =
    when (this) {
        CardSortOrder.ExpiryDate -> R.string.sort_by_expiry_date
        CardSortOrder.CreatedAt -> R.string.sort_by_created_at
        CardSortOrder.Name -> R.string.sort_by_name
    }

@Composable
private fun FilterBar(
    selectedFilter: CardFilter,
    onFilterSelected: (CardFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(CardFilter.entries.toList(), key = { it.name }) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = stringResource(filter.labelRes),
                        maxLines = 1,
                        fontSize = 13.sp,
                    )
                },
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = KeeperMuted)
        },
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun EmptyState(hasCards: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GlassSurface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("＋", color = KeeperBlue, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (hasCards) {
                    stringResource(R.string.empty_filtered_title)
                } else {
                    stringResource(R.string.empty_title)
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasCards) {
                    stringResource(R.string.empty_filtered_subtitle)
                } else {
                    stringResource(R.string.empty_subtitle)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun DonationDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val qrResId = remember(context) {
        context.resources.getIdentifier("donation_qr", "drawable", context.packageName)
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = KeeperRed,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    text = stringResource(R.string.donation_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.donation_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(232.dp)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (qrResId != 0) {
                            Image(
                                painter = painterResource(qrResId),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.donation_missing_qr),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.donation_disclaimer),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ESimCardItem(
    card: ESimCard,
    today: LocalDate,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRenew: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val status = ExpiryCalculator.status(card.startDate, card.expiryDate, today)
    val stateColor = when {
        status.isExpired || status.isWarning -> KeeperRed
        else -> KeeperGreen
    }

    Box {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onEdit,
                    onLongClick = { menuExpanded = true },
                ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Text(card.flagEmoji, fontSize = 34.sp, modifier = Modifier.width(52.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = card.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (card.cycleDays != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = KeeperBlue.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = stringResource(R.string.long_term_badge),
                                        color = KeeperBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = stateColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${card.expiryDate.formatForLocale(context)} · ${remainingText(context, status)}",
                                color = stateColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = stateColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = KeeperMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = card.phoneNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = card.balanceText,
                        color = KeeperBlue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${card.countryName} · ${card.countryCode}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (card.cycleDays != null) {
                        TextButton(onClick = onRenew) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.renew_action))
                        }
                    }
                }
                card.reminderDaysBefore?.let { days ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = KeeperMuted,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (days == 0) {
                                stringResource(R.string.reminder_today)
                            } else {
                                stringResource(R.string.reminder_days_before, days)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_edit)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_copy_phone)) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    clipboard.setText(AnnotatedString(card.phoneNumber))
                    menuExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_add_calendar)) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    addCardToCalendar(context, card)
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_delete), color = KeeperRed) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = KeeperRed) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

private fun remainingText(context: Context, status: ExpiryStatus): String {
    return when {
        status.isExpired -> context.getString(R.string.remaining_expired, status.remainingDays.absoluteValue)
        status.remainingDays == 0L -> context.getString(R.string.remaining_today)
        else -> context.getString(R.string.remaining_days, status.remainingDays)
    }
}

private fun addCardToCalendar(context: Context, card: ESimCard) {
    val beginMillis = card.expiryDate
        .atTime(9, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val endMillis = card.expiryDate
        .atTime(10, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val description = buildString {
        appendLine(context.getString(R.string.calendar_desc_phone, card.phoneNumber))
        appendLine(context.getString(R.string.calendar_desc_country, card.countryName, card.countryCode))
        appendLine(context.getString(R.string.calendar_desc_balance, card.balanceText))
        card.cycleDays?.let { appendLine(context.getString(R.string.calendar_desc_cycle, it)) }
        append(context.getString(R.string.calendar_desc_source))
    }
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.Events.TITLE, context.getString(R.string.calendar_title, card.name))
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
        .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)

    card.reminderDaysBefore?.let { days ->
        intent
            .putExtra(CalendarContract.Events.HAS_ALARM, true)
            .putExtra(CalendarContract.Reminders.MINUTES, days * 24 * 60)
    }

    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, context.getString(R.string.calendar_app_missing), Toast.LENGTH_SHORT).show()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ESimEditorDialog(
    card: ESimCard?,
    onClose: () -> Unit,
    onSave: (CardEditorInput) -> Unit,
) {
    val context = LocalContext.current
    val useChinese = context.isChineseLanguage()
    val valueNotSet = stringResource(R.string.value_not_set)
    val today = remember { LocalDate.now() }
    var name by remember(card?.id) { mutableStateOf(card?.name.orEmpty()) }
    var phone by remember(card?.id) { mutableStateOf(card?.phoneNumber.orEmpty()) }
    var balance by remember(card?.id, valueNotSet) {
        mutableStateOf(card?.balanceText?.takeUnless { it == valueNotSet || it == "未填写" }.orEmpty())
    }
    var selectedCountry by remember(card?.id) {
        mutableStateOf(
            card?.let {
                val template = Countries.findByIso(it.countryCode)
                template?.copy(countryName = it.countryName) ?: CountryOption(
                    countryName = it.countryName,
                    englishName = it.countryName,
                    countryCode = it.countryCode,
                    dialCode = "",
                    flagEmoji = it.flagEmoji,
                )
            } ?: Countries.common.first(),
        )
    }
    var startDate by remember(card?.id) { mutableStateOf(card?.startDate ?: today) }
    var useCycle by remember(card?.id) { mutableStateOf(card?.cycleDays != null) }
    var cycleDaysText by remember(card?.id) { mutableStateOf(card?.cycleDays?.toString() ?: "30") }
    var expiryDate by remember(card?.id) {
        mutableStateOf(
            card?.expiryDate ?: ExpiryCalculator.expiryFromCycle(today, 30),
        )
    }
    var reminderEnabled by remember(card?.id) { mutableStateOf(card?.reminderDaysBefore != null || card == null) }
    var reminderDaysText by remember(card?.id) { mutableStateOf(card?.reminderDaysBefore?.toString() ?: "3") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var deviceSubscriptions by remember { mutableStateOf<List<DeviceSubscriptionInfo>>(emptyList()) }
    var showSubscriptionPicker by remember { mutableStateOf(false) }

    fun applyDeviceSubscription(info: DeviceSubscriptionInfo) {
        val title = info.displayTitle(
            defaultEsimTitle = context.getString(R.string.device_esim),
            defaultSimTitle = context.getString(R.string.device_sim),
        )
        name = title
        if (info.phoneNumber.isNotBlank()) {
            phone = info.phoneNumber
        }
        Countries.findByIso(info.countryIso)?.let { selectedCountry = it }
        importMessage = if (info.phoneNumber.isBlank()) {
            context.getString(R.string.import_read_with_missing_phone, title)
        } else {
            context.getString(R.string.import_read_success, title)
        }
    }

    fun handleDeviceSubscriptions(infos: List<DeviceSubscriptionInfo>) {
        deviceSubscriptions = infos
        when {
            infos.isEmpty() -> importMessage = context.getString(R.string.import_empty)
            infos.size == 1 -> applyDeviceSubscription(infos.first())
            else -> showSubscriptionPicker = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants.values.all { it }
        if (granted) {
            handleDeviceSubscriptions(DeviceSubscriptionReader.read(context))
        } else {
            importMessage = context.getString(R.string.import_permission_denied)
        }
    }

    fun readDeviceSubscriptions() {
        if (DeviceSubscriptionReader.hasPermission(context)) {
            handleDeviceSubscriptions(DeviceSubscriptionReader.read(context))
        } else {
            permissionLauncher.launch(DeviceSubscriptionReader.requiredPermissions)
        }
    }

    val parsedCycleDays = cycleDaysText.toIntOrNull()
    val parsedReminderDays = reminderDaysText.toIntOrNull()
    LaunchedEffect(startDate, parsedCycleDays, useCycle) {
        if (useCycle && parsedCycleDays != null && parsedCycleDays > 0) {
            expiryDate = ExpiryCalculator.expiryFromCycle(startDate, parsedCycleDays)
        }
    }

    val isValid = phone.trim().isNotEmpty() &&
        if (useCycle) {
            parsedCycleDays != null && parsedCycleDays > 0
        } else {
            !expiryDate.isBefore(startDate)
        } &&
        (!reminderEnabled || (parsedReminderDays != null && parsedReminderDays >= 0))

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 20.dp),
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 14.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.close))
                        }
                        Text(
                            text = if (card == null) {
                                stringResource(R.string.editor_add_title)
                            } else {
                                stringResource(R.string.editor_edit_title)
                            },
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(
                            onClick = {
                                onSave(
                                    CardEditorInput(
                                        name = name,
                                        phoneNumber = phone,
                                        country = selectedCountry,
                                        countryName = selectedCountry.displayName(useChinese),
                                        balanceText = balance,
                                        startDate = startDate,
                                        cycleDays = if (useCycle) parsedCycleDays else null,
                                        expiryDate = expiryDate,
                                        reminderDaysBefore = if (reminderEnabled) parsedReminderDays else null,
                                    ),
                                )
                            },
                            enabled = isValid,
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .imePadding()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item {
                        SimImportPanel(
                            message = importMessage,
                            onRead = { readDeviceSubscriptions() },
                        )
                    }
                    item {
                        FormSection(title = stringResource(R.string.section_basic)) {
                            RoundedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = stringResource(R.string.field_name),
                                placeholder = stringResource(R.string.field_name_placeholder),
                            )
                            RoundedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = stringResource(R.string.field_phone),
                                placeholder = stringResource(R.string.field_phone_placeholder),
                                keyboardType = KeyboardType.Phone,
                            )
                            RoundedTextField(
                                value = balance,
                                onValueChange = { balance = it },
                                label = stringResource(R.string.field_balance),
                                placeholder = stringResource(R.string.field_balance_placeholder),
                            )
                            PickerRow(
                                label = stringResource(R.string.field_country),
                                value = "${selectedCountry.flagEmoji}  ${selectedCountry.displayName(useChinese)}  ${selectedCountry.dialCode}",
                                onClick = { showCountryPicker = true },
                            )
                        }
                    }
                    item {
                        FormSection(title = stringResource(R.string.section_rule)) {
                            PickerRow(
                                label = stringResource(R.string.field_start_date),
                                value = startDate.formatForLocale(context),
                                onClick = { dateTarget = DateTarget.Start },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = useCycle,
                                    onClick = { useCycle = true },
                                    label = { Text(stringResource(R.string.rule_cycle)) },
                                )
                                FilterChip(
                                    selected = !useCycle,
                                    onClick = { useCycle = false },
                                    label = { Text(stringResource(R.string.rule_expiry_date)) },
                                )
                            }
                            if (useCycle) {
                                RoundedTextField(
                                    value = cycleDaysText,
                                    onValueChange = { value: String ->
                                        cycleDaysText = value.filter(Char::isDigit)
                                    },
                                    label = stringResource(R.string.field_cycle_days),
                                    placeholder = stringResource(R.string.field_cycle_days_placeholder),
                                    keyboardType = KeyboardType.Number,
                                )
                                ReadOnlyInfoRow(
                                    label = stringResource(R.string.field_auto_expiry),
                                    value = expiryDate.formatForLocale(context),
                                )
                            } else {
                                PickerRow(
                                    label = stringResource(R.string.field_expiry_date),
                                    value = expiryDate.formatForLocale(context),
                                    onClick = { dateTarget = DateTarget.Expiry },
                                )
                            }
                        }
                    }
                    item {
                        FormSection(title = stringResource(R.string.section_reminder)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = reminderEnabled,
                                    onClick = { reminderEnabled = true },
                                    label = { Text(stringResource(R.string.reminder_enabled)) },
                                )
                                FilterChip(
                                    selected = !reminderEnabled,
                                    onClick = { reminderEnabled = false },
                                    label = { Text(stringResource(R.string.reminder_disabled)) },
                                )
                            }
                            if (reminderEnabled) {
                                RoundedTextField(
                                    value = reminderDaysText,
                                    onValueChange = { value: String ->
                                        reminderDaysText = value.filter(Char::isDigit)
                                    },
                                    label = stringResource(R.string.field_reminder_days),
                                    placeholder = stringResource(R.string.field_reminder_days_placeholder),
                                    keyboardType = KeyboardType.Number,
                                )
                                ReadOnlyInfoRow(
                                    label = stringResource(R.string.field_calendar_reminder),
                                    value = if (parsedReminderDays == 0) {
                                        stringResource(R.string.reminder_today)
                                    } else {
                                        stringResource(R.string.reminder_days_before, parsedReminderDays ?: 0)
                                    },
                                )
                            }
                            Button(
                                onClick = {
                                    val now = Instant.now()
                                    addCardToCalendar(
                                        context,
                                        ESimCard(
                                            id = card?.id ?: 0,
                                            name = name.trim().ifBlank {
                                                context.getString(
                                                    R.string.default_esim_name,
                                                    selectedCountry.displayName(useChinese),
                                                )
                                            },
                                            phoneNumber = phone.trim(),
                                            countryName = selectedCountry.displayName(useChinese),
                                            countryCode = selectedCountry.countryCode,
                                            flagEmoji = selectedCountry.flagEmoji,
                                            balanceText = balance.trim().ifBlank {
                                                context.getString(R.string.value_not_set)
                                            },
                                            startDate = startDate,
                                            cycleDays = if (useCycle) parsedCycleDays else null,
                                            expiryDate = expiryDate,
                                            reminderDaysBefore = if (reminderEnabled) parsedReminderDays else null,
                                            createdAt = card?.createdAt ?: now,
                                            updatedAt = now,
                                        ),
                                    )
                                },
                                enabled = isValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KeeperBlue,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.calendar_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            selected = selectedCountry,
            onDismiss = { showCountryPicker = false },
            onSelected = {
                selectedCountry = it
                showCountryPicker = false
            },
        )
    }

    if (showSubscriptionPicker) {
        DeviceSubscriptionPickerDialog(
            subscriptions = deviceSubscriptions,
            onDismiss = { showSubscriptionPicker = false },
            onSelected = {
                applyDeviceSubscription(it)
                showSubscriptionPicker = false
            },
        )
    }

    dateTarget?.let { target ->
        DatePickerModal(
            initialDate = if (target == DateTarget.Start) startDate else expiryDate,
            onDismiss = { dateTarget = null },
            onSelected = { selected ->
                if (target == DateTarget.Start) {
                    startDate = selected
                    if (!useCycle && expiryDate.isBefore(selected)) {
                        expiryDate = selected
                    }
                } else {
                    expiryDate = selected
                }
                dateTarget = null
            },
        )
    }
}

private enum class DateTarget {
    Start,
    Expiry,
}

@Composable
private fun SimImportPanel(
    message: String?,
    onRead: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = KeeperBlue,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sim_import_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.sim_import_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
            TextButton(onClick = onRead) {
                Text(stringResource(R.string.sim_import_action))
            }
            message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun DeviceSubscriptionPickerDialog(
    subscriptions: List<DeviceSubscriptionInfo>,
    onDismiss: () -> Unit,
    onSelected: (DeviceSubscriptionInfo) -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.subscription_picker_title), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                subscriptions.forEach { info ->
                    val title = info.displayTitle(
                        defaultEsimTitle = stringResource(R.string.device_esim),
                        defaultSimTitle = stringResource(R.string.device_sim),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelected(info) }
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (info.isEmbedded) "eSIM" else "SIM", color = KeeperBlue, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(
                                    info.phoneNumber.ifBlank { null },
                                    info.countryIso.ifBlank { null },
                                    context.getString(R.string.slot_label, info.slotIndex + 1),
                                ).joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 12.dp),
    )
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun RoundedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(value, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp)
        }
    }
}

@Composable
private fun ReadOnlyInfoRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CountryPickerDialog(
    selected: CountryOption,
    onDismiss: () -> Unit,
    onSelected: (CountryOption) -> Unit,
) {
    val context = LocalContext.current
    val useChinese = context.isChineseLanguage()
    var query by remember { mutableStateOf("") }
    val results = remember(query, useChinese) { Countries.search(query, useChinese) }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column {
                Text(stringResource(R.string.country_picker_title), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                SearchField(value = query, onValueChange = { query = it })
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(results, key = { it.countryCode }) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelected(option) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(option.flagEmoji, fontSize = 26.sp, modifier = Modifier.width(44.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(option.displayName(useChinese), fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${option.countryCode} · ${option.dialCode}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                )
                            }
                            if (option.countryCode == selected.countryCode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = KeeperBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSelected: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.toPickerMillis())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onSelected(it.toLocalDate()) }
                },
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
