package com.shohankhan.bokeya.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.bokeya.BuildConfig
import com.shohankhan.bokeya.core.BanglaDate
import com.shohankhan.bokeya.core.BanglaNumbers
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.ui.CashflowViewModel
import com.shohankhan.bokeya.ui.GoalsViewModel
import com.shohankhan.bokeya.ui.RecurringViewModel
import com.shohankhan.bokeya.ui.Route
import com.shohankhan.bokeya.ui.SearchViewModel
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaProgress
import com.shohankhan.bokeya.ui.components.BokeyaTextField
import com.shohankhan.bokeya.ui.components.ChipSelector
import com.shohankhan.bokeya.ui.components.DateSelector
import com.shohankhan.bokeya.ui.components.EmptyState
import com.shohankhan.bokeya.ui.components.InfoRow
import com.shohankhan.bokeya.ui.components.MoneyInput
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.PrimaryButton
import com.shohankhan.bokeya.ui.components.SectionHeader
import com.shohankhan.bokeya.ui.components.SelectorOption
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.LocalDate

// ---------------------------------------------------------------- More

@Composable
fun MoreScreen(onNavigate: (String) -> Unit, contentPadding: PaddingValues) {
    data class Entry(val icon: ImageVector, val title: String, val subtitle: String, val route: String)

    val entries = listOf(
        Entry(Icons.Filled.Assessment, "Reports", "আয়-ব্যয় ও বকেয়ার বিস্তারিত report", Route.Reports.path),
        Entry(Icons.Filled.Timeline, "Payment Planner", "কবে কত দিতে হবে", Route.Planner.path),
        Entry(Icons.Filled.Savings, "লক্ষ্য ও সঞ্চয়", "Goal তৈরি করুন", Route.Goals.path),
        Entry(Icons.Filled.Repeat, "Recurring", "নিয়মিত আয়-ব্যয় ও কিস্তি", Route.Recurring.path),
        Entry(Icons.Filled.Category, "Categories", "নিজের category যোগ করুন", Route.Categories.path),
        Entry(Icons.Filled.Archive, "Archive", "পুরনো হিসাব", Route.Archive.path),
        Entry(Icons.Filled.Backup, "Backup ও Export", "PDF, CSV, backup, restore", Route.Backup.path),
        Entry(Icons.Filled.Settings, "Settings", "Theme, notification, security", Route.Settings.path),
        Entry(Icons.Filled.Info, "About", "Bokeya সম্পর্কে", Route.About.path),
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            16.dp,
            16.dp,
            16.dp,
            contentPadding.calculateBottomPadding() + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item("title") {
            Text(
                "আরও",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        items(entries, key = { it.route }) { entry ->
            BokeyaCard(onClick = { onNavigate(entry.route) }, contentPadding = 15.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            entry.icon,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.bokeya.muted,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        tint = MaterialTheme.bokeya.muted,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Search

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onAccountClick: (Long) -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    ScreenScaffold(title = "খুঁজুন", onBack = onBack) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("নাম, দোকান, Loan, টাকা...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            if (query.isBlank()) {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = "কী খুঁজছেন?",
                    message = "মানুষ, দোকান, Loan, EMI, category বা নোট — সব একসাথে খুঁজতে পারবেন।",
                )
            } else if (results.isEmpty) {
                EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "কিছু পাওয়া যায়নি",
                    message = "\"$query\" দিয়ে কোনো হিসাব বা লেনদেন মেলেনি।",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (results.accounts.isNotEmpty()) {
                        item("a_h") { SectionHeader("হিসাব") }
                        items(results.accounts, key = { "a_${it.id}" }) { account ->
                            BokeyaCard(onClick = { onAccountClick(account.id) }, contentPadding = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        iconFor(account.type),
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(account.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            account.type.label + " · " + account.direction.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.bokeya.muted,
                                        )
                                    }
                                    MoneyText(
                                        Money(account.principal + account.interestAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                    }
                    if (results.people.isNotEmpty()) {
                        item("p_h") { SectionHeader("মানুষ") }
                        items(results.people, key = { "p_${it.id}" }) { person ->
                            BokeyaCard(contentPadding = 14.dp) {
                                Text(person.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    person.relationship.label +
                                        (person.phone?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.bokeya.muted,
                                )
                            }
                        }
                    }
                    if (results.transactions.isNotEmpty()) {
                        item("t_h") { SectionHeader("লেনদেন") }
                        item("t") {
                            BokeyaCard(contentPadding = 6.dp) {
                                results.transactions.forEach { tx ->
                                    TransactionRow(
                                        title = tx.title,
                                        subtitle = tx.type.label,
                                        amount = Money(tx.amount),
                                        type = tx.type,
                                        date = LocalDate.ofEpochDay(tx.date),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Goals

@Composable
fun GoalsScreen(viewModel: GoalsViewModel, onBack: () -> Unit) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val (total, paid, progress) = viewModel.debtProgress.collectAsStateWithLifecycle().value
    var showAdd by remember { mutableStateOf(false) }
    var contributeTo by remember { mutableStateOf<Long?>(null) }

    ScreenScaffold(
        title = "লক্ষ্য ও সঞ্চয়",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "নতুন লক্ষ্য") }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (total.isPositive) {
                item("debtfree") {
                    BokeyaCard {
                        Text(
                            "বকেয়া মুক্তির অগ্রগতি",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        BokeyaProgress(progress)
                        Spacer(Modifier.height(10.dp))
                        InfoRow("মোট", CurrencyFormatter.format(total))
                        InfoRow("পরিশোধ", CurrencyFormatter.format(paid))
                        InfoRow("বাকি", CurrencyFormatter.format((total - paid).clampAtZero()))
                    }
                }
            }

            if (goals.isEmpty()) {
                item("empty") {
                    EmptyState(
                        icon = Icons.Filled.Flag,
                        title = "এখনো কোনো লক্ষ্য নেই",
                        message = "\"জরুরি সঞ্চয়\" বা \"Loan শেষ করবো\" — নিজের একটি লক্ষ্য তৈরি করুন।",
                        ctaLabel = "লক্ষ্য যোগ করুন",
                        onCta = { showAdd = true },
                    )
                }
            } else {
                item("g_h") { SectionHeader("আপনার লক্ষ্য") }
                items(goals, key = { it.id }) { goal ->
                    val target = Money(goal.targetAmount)
                    val saved = Money(goal.savedAmount)
                    val goalProgress = if (target.isPositive) {
                        (saved.poisha.toFloat() / target.poisha.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    BokeyaCard(onClick = { contributeTo = goal.id }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(goal.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                BanglaNumbers.toBanglaDigits(((goalProgress * 100).toInt()).toString()) + "%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        BokeyaProgress(goalProgress)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "জমা",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.bokeya.muted,
                                )
                                MoneyText(saved, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "লক্ষ্য",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.bokeya.muted,
                                )
                                MoneyText(target, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { viewModel.delete(goal.id) }) {
                                Text("মুছুন", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { contributeTo = goal.id }) { Text("টাকা যোগ করুন") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("নতুন লক্ষ্য") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BokeyaTextField(title, { title = it }, "লক্ষ্যের নাম")
                    MoneyInput(amount, { amount = it }, label = "কত টাকা?")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && Money.parseOrNull(amount)?.isPositive == true,
                    onClick = {
                        viewModel.addGoal(title.trim(), Money.parseOrNull(amount)!!, "SAVING", null)
                        showAdd = false
                    },
                ) { Text("যোগ করুন") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } },
        )
    }

    contributeTo?.let { goalId ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { contributeTo = null },
            title = { Text("কত টাকা জমা করবেন?") },
            text = { MoneyInput(amount, { amount = it }) },
            confirmButton = {
                TextButton(
                    enabled = Money.parseOrNull(amount)?.isPositive == true,
                    onClick = {
                        viewModel.contribute(goalId, Money.parseOrNull(amount)!!)
                        contributeTo = null
                    },
                ) { Text("জমা করুন") }
            },
            dismissButton = { TextButton(onClick = { contributeTo = null }) { Text("বাতিল") } },
        )
    }
}

// ---------------------------------------------------------------- Recurring

@Composable
fun RecurringScreen(viewModel: RecurringViewModel, onBack: () -> Unit) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Recurring",
        subtitle = "নিয়মিত আয়, খরচ ও কিস্তি",
        onBack = onBack,
        actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "নতুন") } },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Filled.EventRepeat,
                    title = "কোনো recurring নেই",
                    message = "মাসিক বেতন, বাসা ভাড়া বা কিস্তি যোগ করে রাখলে বারবার লিখতে হবে না।",
                    ctaLabel = "যোগ করুন",
                    onCta = { showAdd = true },
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    BokeyaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    item.type.label + " · " + item.frequency.label + " · পরবর্তী " +
                                        BanglaDate.dayMonth(LocalDate.ofEpochDay(item.nextRun)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.bokeya.muted,
                                )
                            }
                            MoneyText(Money(item.amount), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = item.active, onCheckedChange = { viewModel.toggle(item) })
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (item.autoCreate) "নিজে থেকে যোগ হবে" else "শুধু মনে করিয়ে দেবে",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.bokeya.muted,
                                )
                            }
                            IconButton(onClick = { viewModel.delete(item.id) }) {
                                Icon(Icons.Filled.Delete, "মুছুন", Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var typeName by remember { mutableStateOf(TxType.EXPENSE.name) }
        var freqName by remember { mutableStateOf(Frequency.MONTHLY.name) }
        var autoCreate by remember { mutableStateOf(false) }
        var anchor by remember { mutableStateOf(Clocks.today()) }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("নতুন recurring") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BokeyaTextField(title, { title = it }, "কীসের জন্য")
                    MoneyInput(amount, { amount = it })
                    ChipSelector(
                        options = listOf(TxType.INCOME, TxType.EXPENSE)
                            .map { SelectorOption(it.name, it.label) },
                        selectedId = typeName,
                        onSelect = { typeName = it },
                        label = "ধরন",
                    )
                    ChipSelector(
                        options = listOf(
                            Frequency.WEEKLY, Frequency.BIWEEKLY, Frequency.MONTHLY, Frequency.YEARLY,
                        ).map { SelectorOption(it.name, it.label) },
                        selectedId = freqName,
                        onSelect = { freqName = it },
                        label = "কত দিন পর পর",
                    )
                    DateSelector(anchor, { anchor = it }, label = "শুরুর তারিখ")
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("নিজে থেকে entry তৈরি হোক", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autoCreate, onCheckedChange = { autoCreate = it })
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && Money.parseOrNull(amount)?.isPositive == true,
                    onClick = {
                        viewModel.add(
                            title.trim(),
                            TxType.valueOf(typeName),
                            Money.parseOrNull(amount)!!,
                            Frequency.valueOf(freqName),
                            anchor,
                            null,
                            autoCreate,
                        )
                        showAdd = false
                    },
                ) { Text("যোগ করুন") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } },
        )
    }
}

// ---------------------------------------------------------------- Categories

@Composable
fun CategoriesScreen(viewModel: CashflowViewModel, onBack: () -> Unit) {
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var kindIndex by remember { mutableStateOf(0) }

    ScreenScaffold(
        title = "Categories",
        onBack = onBack,
        actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "নতুন") } },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(CategoryKind.EXPENSE to "খরচের category", CategoryKind.INCOME to "আয়ের category")
                .forEach { (kind, label) ->
                    item("h_$kind") { SectionHeader(label) }
                    val list = categories.filter { it.kind == kind }
                    items(list, key = { "c_${it.id}" }) { category ->
                        BokeyaCard(contentPadding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                if (category.builtIn) {
                                    Text(
                                        "Built-in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.bokeya.muted,
                                    )
                                } else {
                                    IconButton(onClick = { viewModel.deleteCategory(category.id) }) {
                                        Icon(Icons.Filled.Delete, "মুছুন", Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("নতুন category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BokeyaTextField(name, { name = it }, "নাম")
                    ChipSelector(
                        options = listOf(
                            SelectorOption("0", "খরচ"),
                            SelectorOption("1", "আয়"),
                        ),
                        selectedId = kindIndex.toString(),
                        onSelect = { kindIndex = it.toInt() },
                        label = "ধরন",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.addCategory(
                            name.trim(),
                            if (kindIndex == 0) CategoryKind.EXPENSE else CategoryKind.INCOME,
                        )
                        showAdd = false
                    },
                ) { Text("যোগ করুন") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("বাতিল") } },
        )
    }
}

// ---------------------------------------------------------------- About

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    ScreenScaffold(title = "About", onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.bokeya.heroStart),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "ব",
                    style = MaterialTheme.typography.displaySmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("বকেয়া", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Bokeya", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.bokeya.muted)
            Text(
                "ধার, বকেয়া, কিস্তি ও আয়-ব্যয়ের সহজ হিসাব।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.bokeya.muted,
                textAlign = TextAlign.Center,
            )

            BokeyaCard {
                InfoRow("Developer", "Shohan Khan")
                InfoRow("Version", BuildConfig.VERSION_NAME)
                InfoRow("Package", BuildConfig.APPLICATION_ID)
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "helloiamshohan@gmail.com",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:helloiamshohan@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Bokeya feedback")
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            BokeyaCard {
                Text("Privacy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bokeya আপনার আর্থিক তথ্য কোনো server-এ পাঠায় না। সব তথ্য শুধু আপনার ফোনেই থাকে। " +
                        "কোনো বিজ্ঞাপন নেই, কোনো tracking নেই, কোনো internet permission নেই।",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            BokeyaCard {
                Text("Open source", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "এই app তৈরিতে ব্যবহৃত হয়েছে AndroidX, Jetpack Compose, Room, WorkManager এবং " +
                        "Kotlin — সবগুলোই Apache License 2.0-এর অধীনে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.bokeya.muted,
                )
            }

            BokeyaCard {
                Text(
                    "Bokeya কোনো পেশাদার আর্থিক পরামর্শ, credit score বা loan approval দেয় না। " +
                        "এটি শুধু আপনার নিজের হিসাব গুছিয়ে রাখার একটি টুল।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.bokeya.muted,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
