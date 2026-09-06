package com.shohankhan.bokeya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.domain.AccountStatus
import com.shohankhan.bokeya.domain.AccountSummary
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.ui.AccountFilter
import com.shohankhan.bokeya.ui.AccountsViewModel
import com.shohankhan.bokeya.ui.SortOrder
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaProgress
import com.shohankhan.bokeya.ui.components.EmptyState
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.StatusBadge
import com.shohankhan.bokeya.ui.theme.bokeya

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onAccountClick: (Long) -> Unit,
    onAdd: (AccountType) -> Unit,
    onSearch: () -> Unit,
    onArchive: () -> Unit,
    contentPadding: PaddingValues,
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()

    var sortMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "হিসাব",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "খুঁজুন") }
            Box {
                IconButton(onClick = { filterMenu = true }) { Icon(Icons.Filled.FilterList, "Filter") }
                DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                    AccountFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { viewModel.setFilter(option); filterMenu = false },
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.SwapVert, "Sort") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortOrder.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { viewModel.setSort(option); sortMenu = false },
                        )
                    }
                }
            }
            IconButton(onClick = onArchive) { Icon(Icons.Filled.Archive, "Archive") }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TypeChip("সব", selectedType == null, null) { viewModel.selectType(null) }
            AccountType.entries.forEach { type ->
                TypeChip(
                    "${type.plural} ${BanglaNumbers.toBanglaDigits((counts[type] ?: 0).toString())}",
                    selectedType == type,
                    type,
                ) { viewModel.selectType(type) }
            }
        }

        if (filter != AccountFilter.ALL) {
            Text(
                "Filter: ${filter.label} · ${sort.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.bokeya.muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        if (summaries.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inbox,
                title = "এখনো কোনো হিসাব নেই",
                message = "প্রথম হিসাবটি যোগ করুন — দোকানের বাকি, ধার, Loan বা EMI।",
                ctaLabel = "হিসাব যোগ করুন",
                onCta = { onAdd(selectedType ?: AccountType.SHOP) },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(summaries, key = { it.id }) { summary ->
                    AccountCard(summary) { onAccountClick(summary.id) }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, type: AccountType?, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            type?.let {
                Icon(
                    iconFor(it),
                    null,
                    Modifier.size(15.dp),
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun AccountCard(summary: AccountSummary, onClick: () -> Unit) {
    val extras = MaterialTheme.bokeya
    BokeyaCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconFor(summary.type),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(summary.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    listOfNotNull(summary.subtitle, summary.direction.label).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.muted,
                    maxLines = 1,
                )
            }
            StatusBadge(summary.status)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    if (summary.direction == Direction.I_OWE) "বাকি" else "পাওনা",
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.muted,
                )
                MoneyText(
                    summary.remaining,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (summary.remaining.isZero) extras.success else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (summary.total.isPositive) {
                Text(
                    BanglaNumbers.toBanglaDigits(summary.progressPercent.toString()) + "% পরিশোধ",
                    style = MaterialTheme.typography.labelMedium,
                    color = extras.muted,
                )
            }
        }

        if (summary.total.isPositive) {
            Spacer(Modifier.height(9.dp))
            BokeyaProgress(
                summary.progress,
                color = if (summary.remaining.isZero) extras.success else MaterialTheme.colorScheme.primary,
                height = 6.dp,
            )
        }

        if (summary.nextDueDate != null && summary.nextDueAmount != null && !summary.remaining.isZero) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "পরবর্তী · " + BanglaDate.relative(summary.nextDueDate, Clocks.today()),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (summary.status == AccountStatus.OVERDUE) extras.danger else extras.muted,
                    )
                    MoneyText(summary.nextDueAmount, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ArchiveScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onAccountClick: (Long) -> Unit,
) {
    val all by viewModel.allSummaries.collectAsStateWithLifecycle()
    val archived = all.filter { it.archived }

    ScreenScaffold(title = "Archive", onBack = onBack) { padding ->
        if (archived.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Archive,
                title = "Archive খালি",
                message = "পরিশোধ হয়ে যাওয়া হিসাব এখানে রাখতে পারবেন।",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(archived, key = { it.id }) { summary ->
                    AccountCard(summary) { onAccountClick(summary.id) }
                }
            }
        }
    }
}
