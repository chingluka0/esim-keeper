package com.baohao.esimkeeper.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
import com.baohao.esimkeeper.data.Countries
import com.baohao.esimkeeper.data.CountryOption
import com.baohao.esimkeeper.data.DeviceSubscriptionInfo
import com.baohao.esimkeeper.data.DeviceSubscriptionReader
import com.baohao.esimkeeper.data.ESimCard
import com.baohao.esimkeeper.data.SortOrder
import com.baohao.esimkeeper.domain.ExpiryCalculator
import com.baohao.esimkeeper.domain.ExpiryStatus
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

private enum class CardFilter(val label: String) {
    All("全部"),
    Warning("即将到期"),
    Expired("已过期"),
    LongTerm("长期保号"),
}

@Composable
fun ESimKeeperApp(viewModel: MainViewModel) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selectedFilter by remember { mutableStateOf(CardFilter.All) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val currentSortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { exportCardsToJson(context, it, cards) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val imported = importCardsFromJson(context, it)
            imported.forEach { card -> viewModel.saveCardDirect(card) }
            if (imported.isNotEmpty()) {
                Toast.makeText(context, "已导入 ${imported.size} 张卡片", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                text = { Text("添加") },
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
                sortOrder = currentSortOrder,
                onToggleTheme = viewModel::toggleDarkMode,
                onOpenDonation = { showDonationDialog = true },
                onToggleSort = { showSortMenu = !showSortMenu },
                onExport = { exportLauncher.launch("esim_cards_backup.json") },
                onImport = { importLauncher.launch(arrayOf("application/json")) },
                showSortMenu = showSortMenu,
                onDismissSortMenu = { showSortMenu = false },
                onSelectSortOrder = { viewModel.setSortOrder(it) },
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
                Text("${filteredCards.size} 张卡片", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
    sortOrder: SortOrder,
    onToggleTheme: () -> Unit,
    onOpenDonation: () -> Unit,
    onToggleSort: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    showSortMenu: Boolean,
    onDismissSortMenu: () -> Unit,
    onSelectSortOrder: (SortOrder) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "eSIM 保号管家",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (cardCount == 0) "离线记录你的 eSIM 保号时间" else "已记录 $cardCount 张 eSIM",
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
                            .clickable(onClick = onToggleSort),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "排序",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = onDismissSortMenu,
                ) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            leadingIcon = {
                                if (order == sortOrder) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = KeeperBlue)
                                }
                            },
                            onClick = {
                                onSelectSortOrder(order)
                                onDismissSortMenu()
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
                        .clickable(onClick = onExport),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "导出",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
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
                        .clickable(onClick = onImport),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "导入",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
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
                        text = filter.label,
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
        placeholder = { Text("搜索运营商、国家或号码") },
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
                text = if (hasCards) "没有找到匹配的卡片" else "还没有 eSIM 卡片",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasCards) "换个关键词试试" else "点击右下角加号开始记录",
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
                    text = "感谢您的 Token",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "软件免费使用。如果它帮你省心，可以自愿付给我一点点，就当是 token 了。",
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
                                text = "将收款码图片放到\napp/src/main/res/drawable/donation_qr.jpg",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Text(
                    text = "灵感来自 SIMHUB。本应用为独立作品，非官方、无隶属关系。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("关闭")
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
                                        text = "长期保号",
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
                                text = "${card.expiryDate.format(dateFormatter)} · ${remainingText(status)}",
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
                            Text("已保号/续期")
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
                            text = if (days == 0) "到期当天提醒" else "提前 $days 天提醒",
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
                text = { Text("编辑") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("复制手机号") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    clipboard.setText(AnnotatedString(card.phoneNumber))
                    menuExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("加入系统日历") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    addCardToCalendar(context, card)
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("删除", color = KeeperRed) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = KeeperRed) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

private fun remainingText(status: ExpiryStatus): String {
    return when {
        status.isExpired -> "已过期 ${status.remainingDays.absoluteValue} 天"
        status.remainingDays == 0L -> "今天到期"
        else -> "${status.remainingDays} 天后到期"
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
        appendLine("号码：${card.phoneNumber}")
        appendLine("地区：${card.countryName} · ${card.countryCode}")
        appendLine("余额：${card.balanceText}")
        card.cycleDays?.let { appendLine("保号周期：$it 天") }
        append("来自 eSIM 保号管家")
    }
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.Events.TITLE, "eSIM 保号提醒：${card.name}")
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
        .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)

    card.reminderDaysBefore?.let { days ->
        intent
            .putExtra(CalendarContract.Events.HAS_ALARM, true)
            .putExtra(CalendarContract.Reminders.MINUTES, days * 24 * 60)
    }

    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "没有找到可用的日历应用", Toast.LENGTH_SHORT).show()
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
    val today = remember { LocalDate.now() }
    var name by remember(card?.id) { mutableStateOf(card?.name.orEmpty()) }
    var phone by remember(card?.id) { mutableStateOf(card?.phoneNumber.orEmpty()) }
    var balance by remember(card?.id) { mutableStateOf(card?.balanceText?.takeUnless { it == "未填写" }.orEmpty()) }
    var selectedCountry by remember(card?.id) {
        mutableStateOf(
            card?.let {
                CountryOption(it.countryName, it.countryCode, "", it.flagEmoji)
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
        name = info.displayTitle
        if (info.phoneNumber.isNotBlank()) {
            phone = info.phoneNumber
        }
        Countries.findByIso(info.countryIso)?.let { selectedCountry = it }
        importMessage = if (info.phoneNumber.isBlank()) {
            "已读取 ${info.displayTitle}，但系统未公开手机号，请手动填写。"
        } else {
            "已读取 ${info.displayTitle}。"
        }
    }

    fun handleDeviceSubscriptions(infos: List<DeviceSubscriptionInfo>) {
        deviceSubscriptions = infos
        when {
            infos.isEmpty() -> importMessage = "没有读取到可用的 SIM/eSIM 信息。"
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
            importMessage = "未授予电话权限，仍可手动填写 eSIM 信息。"
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
                            Text("关闭")
                        }
                        Text(
                            text = if (card == null) "添加 eSIM" else "编辑 eSIM",
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
                            Text("保存")
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
                        FormSection(title = "基础信息") {
                            RoundedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "运营商/名称",
                                placeholder = "例如 US Mobile",
                            )
                            RoundedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = "手机号",
                                placeholder = "例如 +13020000000",
                                keyboardType = KeyboardType.Phone,
                            )
                            RoundedTextField(
                                value = balance,
                                onValueChange = { balance = it },
                                label = "余额",
                                placeholder = "例如 4.5万、10元、$3.2",
                            )
                            PickerRow(
                                label = "国家/地区",
                                value = "${selectedCountry.flagEmoji}  ${selectedCountry.countryName}  ${selectedCountry.dialCode}",
                                onClick = { showCountryPicker = true },
                            )
                        }
                    }
                    item {
                        FormSection(title = "保号规则") {
                            PickerRow(
                                label = "开始日期",
                                value = startDate.format(dateFormatter),
                                onClick = { dateTarget = DateTarget.Start },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = useCycle,
                                    onClick = { useCycle = true },
                                    label = { Text("按周期") },
                                )
                                FilterChip(
                                    selected = !useCycle,
                                    onClick = { useCycle = false },
                                    label = { Text("直接到期日") },
                                )
                            }
                            if (useCycle) {
                                RoundedTextField(
                                    value = cycleDaysText,
                                    onValueChange = { value: String ->
                                        cycleDaysText = value.filter(Char::isDigit)
                                    },
                                    label = "保号周期天数",
                                    placeholder = "例如 30",
                                    keyboardType = KeyboardType.Number,
                                )
                                ReadOnlyInfoRow(label = "自动到期日", value = expiryDate.format(dateFormatter))
                            } else {
                                PickerRow(
                                    label = "到期日",
                                    value = expiryDate.format(dateFormatter),
                                    onClick = { dateTarget = DateTarget.Expiry },
                                )
                            }
                        }
                    }
                    item {
                        FormSection(title = "提醒") {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = reminderEnabled,
                                    onClick = { reminderEnabled = true },
                                    label = { Text("开启提醒") },
                                )
                                FilterChip(
                                    selected = !reminderEnabled,
                                    onClick = { reminderEnabled = false },
                                    label = { Text("不提醒") },
                                )
                            }
                            if (reminderEnabled) {
                                RoundedTextField(
                                    value = reminderDaysText,
                                    onValueChange = { value: String ->
                                        reminderDaysText = value.filter(Char::isDigit)
                                    },
                                    label = "提前提醒天数",
                                    placeholder = "例如 3",
                                    keyboardType = KeyboardType.Number,
                                )
                                ReadOnlyInfoRow(
                                    label = "日历提醒",
                                    value = if (parsedReminderDays == 0) {
                                        "到期当天"
                                    } else {
                                        "提前 ${parsedReminderDays ?: 0} 天"
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
                                            name = name.trim().ifBlank { "${selectedCountry.countryName} eSIM" },
                                            phoneNumber = phone.trim(),
                                            countryName = selectedCountry.countryName,
                                            countryCode = selectedCountry.countryCode,
                                            flagEmoji = selectedCountry.flagEmoji,
                                            balanceText = balance.trim().ifBlank { "未填写" },
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
                                    text = "加入系统日历",
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
                    Text("读取本机 SIM/eSIM", fontWeight = FontWeight.Bold)
                    Text(
                        "自动预填运营商、手机号和国家；读不到的字段可手动补充。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
            TextButton(onClick = onRead) {
                Text("授权并读取")
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
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("选择要导入的号码", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                subscriptions.forEach { info ->
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
                            Text(info.displayTitle, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(
                                    info.phoneNumber.ifBlank { null },
                                    info.countryIso.ifBlank { null },
                                    "卡槽 ${info.slotIndex + 1}",
                                ).joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("取消")
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
    var query by remember { mutableStateOf("") }
    val results = remember(query) { Countries.search(query) }

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(18.dp),
        ) {
            Column {
                Text("选择国家/地区", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                                Text(option.countryName, fontWeight = FontWeight.SemiBold)
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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

// ── JSON Export / Import ──────────────────────────────────────────────────────

private fun exportCardsToJson(context: Context, uri: Uri, cards: List<ESimCard>) {
    runCatching {
        val jsonArray = buildString {
            append("[")
            cards.forEachIndexed { index, card ->
                if (index > 0) append(",")
                append("{")
                append("\"name\":\"${card.name.jsonEscape()}",")
                append("\"phoneNumber\":\"${card.phoneNumber.jsonEscape()}",")
                append("\"countryName\":\"${card.countryName.jsonEscape()}",")
                append("\"countryCode\":\"${card.countryCode.jsonEscape()}",")
                append("\"flagEmoji\":\"${card.flagEmoji.jsonEscape()}",")
                append("\"balanceText\":\"${card.balanceText.jsonEscape()}",")
                append("\"startDate\":\"${card.startDate}",")
                append("\"cycleDays\":${card.cycleDays ?: "null"},")
                append("\"expiryDate\":\"${card.expiryDate}",")
                append("\"reminderDaysBefore\":${card.reminderDaysBefore ?: "null"},")
                append("\"createdAt\":\"${card.createdAt}",")
                append("\"updatedAt\":\"${card.updatedAt}")
                append("}")
            }
            append("]")
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonArray.toByteArray(Charsets.UTF_8))
        }
        Toast.makeText(context, "已导出 ${cards.size} 张卡片", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "导出失败: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun importCardsFromJson(context: Context, uri: Uri): List<ESimCard> {
    return runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: return emptyList()

        val items = mutableListOf<ESimCard>()
        val raw = text.trim()
        if (!raw.startsWith("[")) return emptyList()

        // Simple JSON array parser for our known format
        val entries = raw.removeSurrounding("[", "]").splitJsonObjects()
        for (entry in entries) {
            val name = entry.jsonField("name") ?: continue
            val phoneNumber = entry.jsonField("phoneNumber") ?: ""
            val countryName = entry.jsonField("countryName") ?: ""
            val countryCode = entry.jsonField("countryCode") ?: ""
            val flagEmoji = entry.jsonField("flagEmoji") ?: ""
            val balanceText = entry.jsonField("balanceText") ?: "未填写"
            val startDate = entry.jsonField("startDate")?.let { LocalDate.parse(it) } ?: continue
            val expiryDate = entry.jsonField("expiryDate")?.let { LocalDate.parse(it) } ?: continue
            val cycleDays = entry.jsonField("cycleDays")?.toIntOrNull()
            val reminderDaysBefore = entry.jsonField("reminderDaysBefore")?.toIntOrNull()
            val now = Instant.now()
            items.add(
                ESimCard(
                    id = 0,
                    name = name,
                    phoneNumber = phoneNumber,
                    countryName = countryName,
                    countryCode = countryCode,
                    flagEmoji = flagEmoji,
                    balanceText = balanceText,
                    startDate = startDate,
                    cycleDays = cycleDays,
                    expiryDate = expiryDate,
                    reminderDaysBefore = reminderDaysBefore,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
        items
    }.getOrElse {
        Toast.makeText(context, "导入失败: ${it.message}", Toast.LENGTH_SHORT).show()
        emptyList()
    }
}

private fun String.jsonEscape(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

private fun String.jsonField(key: String): String? {
    val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
    return pattern.find(this)?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\r", "\r")?.replace("\\\"", "\"")?.replace("\\\\", "\\")
        ?: run {
            val numPattern = Regex("\"$key\"\\s*:\\s*(null|\\d+)")
            numPattern.find(this)?.groupValues?.get(1)?.takeIf { it != "null" }
        }
}

private fun String.splitJsonObjects(): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = -1
    for (i in indices) {
        when (this[i]) {
            '{' -> { if (depth == 0) start = i; depth++ }
            '}' -> { depth--; if (depth == 0 && start >= 0) { result.add(substring(start, i + 1)); start = -1 } }
        }
    }
    return result
}
