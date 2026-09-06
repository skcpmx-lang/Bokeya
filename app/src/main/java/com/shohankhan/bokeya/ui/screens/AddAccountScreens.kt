package com.shohankhan.bokeya.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shohankhan.bokeya.core.CurrencyFormatter
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.core.ScheduleEngine
import com.shohankhan.bokeya.data.db.ShopItemEntity
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.PaymentMethod
import com.shohankhan.bokeya.domain.Relationship
import com.shohankhan.bokeya.domain.Unit as BokeyaUnit
import com.shohankhan.bokeya.ui.AccountsViewModel
import com.shohankhan.bokeya.ui.DetailViewModel
import com.shohankhan.bokeya.ui.components.BokeyaCard
import com.shohankhan.bokeya.ui.components.BokeyaTextField
import com.shohankhan.bokeya.ui.components.ChipSelector
import com.shohankhan.bokeya.ui.components.DateSelector
import com.shohankhan.bokeya.ui.components.ExpandableSection
import com.shohankhan.bokeya.ui.components.InfoRow
import com.shohankhan.bokeya.ui.components.MoneyInput
import com.shohankhan.bokeya.ui.components.MoneyText
import com.shohankhan.bokeya.ui.components.PrimaryButton
import com.shohankhan.bokeya.ui.components.SecondaryButton
import com.shohankhan.bokeya.ui.components.SegmentedToggle
import com.shohankhan.bokeya.ui.components.SelectorOption
import com.shohankhan.bokeya.ui.theme.bokeya
import java.time.LocalDate

@Composable
private fun FormScaffold(
    title: String,
    onBack: () -> Unit,
    saveLabel: String = "সংরক্ষণ করুন",
    saveEnabled: Boolean = true,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
            Spacer(Modifier.height(6.dp))
            PrimaryButton(
                text = saveLabel,
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AddShopScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var hasDue by rememberSaveable { mutableStateOf(false) }
    var dueDate by rememberSaveable(stateSaver = LocalDateSaver) {
        mutableStateOf(Clocks.today().plusDays(30))
    }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    FormScaffold(
        title = "নতুন দোকান",
        onBack = onBack,
        saveLabel = "দোকান যোগ করুন",
        saveEnabled = name.isNotBlank(),
        onSave = {
            if (name.isBlank()) {
                error = "দোকানের নাম লিখুন।"
            } else {
                viewModel.createShop(name.trim(), note.takeIf { it.isNotBlank() }, dueDate.takeIf { hasDue }, onCreated)
            }
        },
    ) {
        Text(
            "দোকান তৈরি করার পর সেখানে বাকির জিনিসপত্র যোগ করতে পারবেন।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.bokeya.muted,
        )
        BokeyaTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = "দোকানের নাম",
            isError = error != null,
            supportingText = error,
        )
        ExpandableSection("আরও তথ্য", advanced, { advanced = !advanced }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("পরিশোধের তারিখ ঠিক করুন", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasDue, onCheckedChange = { hasDue = it })
                }
                if (hasDue) DateSelector(dueDate, { dueDate = it }, label = "কবে দিতে হবে")
                BokeyaTextField(note, { note = it }, "নোট (optional)", singleLine = false)
            }
        }
    }
}

@Composable
fun AddPurchaseScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    data class ItemDraft(
        var name: String = "",
        var qty: String = "1",
        var unit: String = BokeyaUnit.PIECE.label,
        var price: String = "",
    )

    val items = remember { mutableStateListOf(ItemDraft()) }
    var date by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(Clocks.today()) }
    var note by rememberSaveable { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }

    fun totalOf(draft: ItemDraft): Money {
        val qty = draft.qty.toDoubleOrNull() ?: 0.0
        val price = Money.parseOrNull(draft.price) ?: Money.ZERO
        return Money((price.poisha * qty).toLong())
    }

    val grandTotal = Money(items.sumOf { totalOf(it).poisha })
    val valid = items.any { it.name.isNotBlank() && totalOf(it).isPositive }

    FormScaffold(
        title = "বাকি যোগ করুন",
        onBack = onBack,
        saveLabel = "সংরক্ষণ করুন · " + CurrencyFormatter.format(grandTotal),
        saveEnabled = valid,
        onSave = {
            val entities = items
                .filter { it.name.isNotBlank() && totalOf(it).isPositive }
                .map {
                    ShopItemEntity(
                        purchaseId = 0,
                        name = it.name.trim(),
                        quantity = it.qty.toDoubleOrNull() ?: 1.0,
                        unit = it.unit,
                        unitPrice = (Money.parseOrNull(it.price) ?: Money.ZERO).poisha,
                        total = totalOf(it).poisha,
                    )
                }
            viewModel.addPurchase(entities, date, note.takeIf { it.isNotBlank() }, onSaved)
        },
    ) {
        DateSelector(date, { date = it })

        items.forEachIndexed { index, draft ->
            key(index, tick) {
                BokeyaCard(contentPadding = 14.dp) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "জিনিস ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.bokeya.muted,
                        )
                        if (items.size > 1) {
                            IconButton(onClick = { items.removeAt(index); tick++ }) {
                                Icon(Icons.Filled.Delete, "মুছুন", Modifier.height(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    BokeyaTextField(
                        value = draft.name,
                        onValueChange = { items[index] = draft.copy(name = it); tick++ },
                        label = "কী কিনেছেন",
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BokeyaTextField(
                            value = draft.qty,
                            onValueChange = { items[index] = draft.copy(qty = it); tick++ },
                            label = "পরিমাণ",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                        BokeyaTextField(
                            value = draft.price,
                            onValueChange = { items[index] = draft.copy(price = it); tick++ },
                            label = "দর (প্রতি একক)",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ChipSelector(
                        options = BokeyaUnit.entries.map { SelectorOption(it.label, it.label) },
                        selectedId = draft.unit,
                        onSelect = { items[index] = draft.copy(unit = it); tick++ },
                        label = "একক",
                    )
                    Spacer(Modifier.height(10.dp))
                    InfoRow("এই জিনিসের মোট", CurrencyFormatter.format(totalOf(draft)))
                }
            }
        }

        SecondaryButton(
            text = "আরেকটি জিনিস",
            onClick = { items.add(ItemDraft()); tick++ },
            modifier = Modifier.fillMaxWidth(),
        )

        BokeyaTextField(note, { note = it }, "নোট (optional)", singleLine = false)

        BokeyaCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("সর্বমোট", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                MoneyText(grandTotal, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
fun AddPersonalScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    var directionIndex by rememberSaveable { mutableStateOf(0) }
    var amount by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var relationship by rememberSaveable { mutableStateOf(Relationship.FRIEND.name) }
    var phone by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(Clocks.today()) }
    var hasDue by rememberSaveable { mutableStateOf(false) }
    var dueDate by rememberSaveable(stateSaver = LocalDateSaver) {
        mutableStateOf(Clocks.today().plusMonths(1))
    }
    var note by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsed = Money.parseOrNull(amount)
    val direction = if (directionIndex == 0) Direction.I_OWE else Direction.THEY_OWE

    FormScaffold(
        title = "ব্যক্তিগত ধার",
        onBack = onBack,
        saveEnabled = parsed?.isPositive == true && name.isNotBlank(),
        onSave = {
            when {
                parsed == null || !parsed.isPositive -> error = "টাকার পরিমাণ সঠিকভাবে লিখুন।"
                name.isBlank() -> error = "নাম লিখুন।"
                else -> viewModel.createPersonalDebt(
                    personName = name.trim(),
                    relationship = Relationship.valueOf(relationship),
                    phone = phone.takeIf { it.isNotBlank() },
                    amount = parsed,
                    direction = direction,
                    date = date,
                    dueDate = dueDate.takeIf { hasDue },
                    note = note.takeIf { it.isNotBlank() },
                    onDone = { onCreated() },
                )
            }
        },
    ) {
        SegmentedToggle(
            options = listOf("আমি ধার নিয়েছি", "আমি ধার দিয়েছি"),
            selectedIndex = directionIndex,
            onSelect = { directionIndex = it },
        )
        Text(
            if (direction == Direction.I_OWE) {
                "এই টাকা আপনাকে ফেরত দিতে হবে।"
            } else {
                "এই টাকা আপনি ফেরত পাবেন।"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.bokeya.muted,
        )

        MoneyInput(amount, { amount = it; error = null }, isError = error != null, errorText = error)

        BokeyaTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = if (direction == Direction.I_OWE) "কার কাছ থেকে?" else "কাকে দিয়েছেন?",
        )

        ChipSelector(
            options = Relationship.entries.map { SelectorOption(it.name, it.label) },
            selectedId = relationship,
            onSelect = { relationship = it },
            label = "সম্পর্ক",
        )

        DateSelector(date, { date = it })

        ExpandableSection("আরও তথ্য", advanced, { advanced = !advanced }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BokeyaTextField(phone, { phone = it }, "ফোন নম্বর (optional)", keyboardType = KeyboardType.Phone)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("ফেরতের তারিখ", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasDue, onCheckedChange = { hasDue = it })
                }
                if (hasDue) DateSelector(dueDate, { dueDate = it }, label = "কবে")
                BokeyaTextField(note, { note = it }, "নোট (optional)", singleLine = false)
            }
        }
    }
}

@Composable
fun AddLoanScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    var institution by rememberSaveable { mutableStateOf("") }
    var loanName by rememberSaveable { mutableStateOf("") }
    var principal by rememberSaveable { mutableStateOf("") }
    var interest by rememberSaveable { mutableStateOf("") }
    var installmentAmount by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf(Frequency.MONTHLY.name) }
    var startDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(Clocks.today()) }
    var note by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val principalMoney = Money.parseOrNull(principal)
    val interestMoney = Money.parseOrNull(interest) ?: Money.ZERO
    val installmentMoney = Money.parseOrNull(installmentAmount) ?: Money.ZERO
    val installments = count.toIntOrNull() ?: 0
    val totalPayable = (principalMoney ?: Money.ZERO) + interestMoney

    // Live preview of the generated schedule so the user sees exactly what will be created.
    val preview = if (installments in 1..600 && totalPayable.isPositive) {
        ScheduleEngine.installmentPlan(
            totalPayable,
            installments,
            startDate,
            Frequency.valueOf(frequency),
        ).take(3)
    } else {
        emptyList()
    }

    FormScaffold(
        title = "নতুন Loan",
        onBack = onBack,
        saveEnabled = principalMoney?.isPositive == true && loanName.isNotBlank(),
        onSave = {
            when {
                principalMoney == null || !principalMoney.isPositive -> error = "Loan-এর পরিমাণ লিখুন।"
                loanName.isBlank() -> error = "Loan-এর নাম লিখুন।"
                else -> viewModel.createLoan(
                    institution = institution.trim().ifBlank { loanName.trim() },
                    loanName = loanName.trim(),
                    principal = principalMoney,
                    interest = interestMoney,
                    installmentAmount = installmentMoney,
                    installmentCount = installments,
                    frequency = Frequency.valueOf(frequency),
                    startDate = startDate,
                    note = note.takeIf { it.isNotBlank() },
                    onDone = { onCreated() },
                )
            }
        },
    ) {
        MoneyInput(
            principal,
            { principal = it; error = null },
            label = "Loan-এর পরিমাণ",
            isError = error != null,
            errorText = error,
        )
        BokeyaTextField(loanName, { loanName = it; error = null }, "Loan-এর নাম (যেমন BRAC Loan)")
        BokeyaTextField(institution, { institution = it }, "Institution / NGO")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BokeyaTextField(
                installmentAmount,
                { installmentAmount = it },
                "কিস্তির টাকা",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            BokeyaTextField(
                count,
                { count = it.filter { c -> c.isDigit() }.take(3) },
                "কত কিস্তি",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }

        ChipSelector(
            options = listOf(
                Frequency.WEEKLY, Frequency.BIWEEKLY, Frequency.MONTHLY, Frequency.QUARTERLY,
            ).map { SelectorOption(it.name, it.label) },
            selectedId = frequency,
            onSelect = { frequency = it },
            label = "কিস্তির ধরন",
        )

        DateSelector(startDate, { startDate = it }, label = "প্রথম কিস্তির তারিখ")

        ExpandableSection("আরও তথ্য", advanced, { advanced = !advanced }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BokeyaTextField(interest, { interest = it }, "Interest / সার্ভিস চার্জ", keyboardType = KeyboardType.Decimal)
                BokeyaTextField(note, { note = it }, "নোট (optional)", singleLine = false)
            }
        }

        if (totalPayable.isPositive) {
            BokeyaCard {
                Text("সারসংক্ষেপ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                InfoRow("মোট পরিশোধযোগ্য", CurrencyFormatter.format(totalPayable))
                if (installments > 0) {
                    InfoRow(
                        "কিস্তি",
                        com.shohankhan.bokeya.core.BanglaNumbers.toBanglaDigits(installments.toString()) + "টি",
                    )
                }
                preview.forEach {
                    InfoRow(
                        "কিস্তি " + com.shohankhan.bokeya.core.BanglaNumbers.toBanglaDigits(it.number.toString()),
                        com.shohankhan.bokeya.core.BanglaDate.full(it.dueDate) + " · " +
                            CurrencyFormatter.format(it.amount),
                    )
                }
            }
        }
    }
}

@Composable
fun AddEmiScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    var product by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var seller by rememberSaveable { mutableStateOf("") }
    var totalPrice by rememberSaveable { mutableStateOf("") }
    var downPayment by rememberSaveable { mutableStateOf("") }
    var installmentAmount by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf(Frequency.MONTHLY.name) }
    var startDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(Clocks.today()) }
    var method by rememberSaveable { mutableStateOf(PaymentMethod.CASH.name) }
    var note by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val price = Money.parseOrNull(totalPrice)
    val down = Money.parseOrNull(downPayment) ?: Money.ZERO
    val financed = ((price ?: Money.ZERO) - down).clampAtZero()
    val installments = count.toIntOrNull() ?: 0
    val perInstallment = Money.parseOrNull(installmentAmount)
        ?: if (installments > 0) financed.splitInto(installments).first() else Money.ZERO

    FormScaffold(
        title = "নতুন EMI",
        onBack = onBack,
        saveEnabled = price?.isPositive == true && product.isNotBlank(),
        onSave = {
            when {
                price == null || !price.isPositive -> error = "পণ্যের দাম লিখুন।"
                product.isBlank() -> error = "পণ্যের নাম লিখুন।"
                down > price -> error = "Down payment দামের চেয়ে বেশি হতে পারে না।"
                else -> viewModel.createEmi(
                    product = product.trim(),
                    brand = brand.takeIf { it.isNotBlank() },
                    seller = seller.takeIf { it.isNotBlank() },
                    totalPrice = price,
                    downPayment = down,
                    installmentAmount = perInstallment,
                    installmentCount = installments,
                    frequency = Frequency.valueOf(frequency),
                    startDate = startDate,
                    method = PaymentMethod.valueOf(method),
                    note = note.takeIf { it.isNotBlank() },
                    onDone = { onCreated() },
                )
            }
        },
    ) {
        BokeyaTextField(
            product,
            { product = it; error = null },
            "পণ্যের নাম (যেমন Samsung Refrigerator)",
        )
        MoneyInput(
            totalPrice,
            { totalPrice = it; error = null },
            label = "মোট দাম",
            isError = error != null,
            errorText = error,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BokeyaTextField(
                downPayment,
                { downPayment = it },
                "Down payment",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            BokeyaTextField(
                count,
                { count = it.filter { c -> c.isDigit() }.take(3) },
                "কত কিস্তি",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        BokeyaTextField(
            installmentAmount,
            { installmentAmount = it },
            "EMI-এর টাকা (খালি রাখলে হিসাব করে নেবে)",
            keyboardType = KeyboardType.Decimal,
        )
        ChipSelector(
            options = listOf(Frequency.WEEKLY, Frequency.MONTHLY, Frequency.QUARTERLY)
                .map { SelectorOption(it.name, it.label) },
            selectedId = frequency,
            onSelect = { frequency = it },
            label = "কিস্তির ধরন",
        )
        DateSelector(startDate, { startDate = it }, label = "প্রথম কিস্তির তারিখ")

        ExpandableSection("আরও তথ্য", advanced, { advanced = !advanced }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BokeyaTextField(brand, { brand = it }, "Brand")
                BokeyaTextField(seller, { seller = it }, "Seller / দোকান")
                ChipSelector(
                    options = PaymentMethod.entries.map { SelectorOption(it.name, it.label) },
                    selectedId = method,
                    onSelect = { method = it },
                    label = "Payment method",
                )
                BokeyaTextField(note, { note = it }, "নোট (optional)", singleLine = false)
            }
        }

        if (financed.isPositive) {
            BokeyaCard {
                Text("সারসংক্ষেপ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                InfoRow("মোট দাম", CurrencyFormatter.format(price ?: Money.ZERO))
                InfoRow("Down payment", CurrencyFormatter.format(down))
                InfoRow("Financed", CurrencyFormatter.format(financed))
                if (installments > 0) {
                    InfoRow(
                        "EMI",
                        CurrencyFormatter.format(perInstallment) + " × " +
                            com.shohankhan.bokeya.core.BanglaNumbers.toBanglaDigits(installments.toString()),
                    )
                }
            }
        }
    }
}

@Composable
fun AddIncomeScreen(
    viewModel: com.shohankhan.bokeya.ui.CashflowViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val categories by androidx.lifecycle.compose.collectAsStateWithLifecycle(
        viewModel.incomeCategories,
    )
    var amount by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var date by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(Clocks.today()) }
    var note by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsed = Money.parseOrNull(amount)

    FormScaffold(
        title = "নতুন আয়",
        onBack = onBack,
        saveEnabled = parsed?.isPositive == true && source.isNotBlank(),
        onSave = {
            when {
                parsed == null || !parsed.isPositive -> error = "টাকার পরিমাণ সঠিকভাবে লিখুন।"
                source.isBlank() -> error = "কোথা থেকে এসেছে লিখুন।"
                else -> viewModel.addIncome(
                    parsed, source.trim(), date, categoryId, note.takeIf { it.isNotBlank() }, onSaved,
                )
            }
        },
    ) {
        MoneyInput(amount, { amount = it; error = null }, isError = error != null, errorText = error)
        BokeyaTextField(source, { source = it; error = null }, "কোথা থেকে টাকা এসেছে?")
        ChipSelector(
            options = categories.map { SelectorOption(it.id.toString(), it.name) },
            selectedId = categoryId?.toString(),
            onSelect = { categoryId = it.toLongOrNull() },
            label = "Category",
        )
        DateSelector(date, { date = it })
        ExpandableSection("আরও তথ্য", advanced, { advanced = !advanced }) {
            BokeyaTextField(note, { note = it }, "নোট (optional)", singleLine = false)
        }
    }
}

@Composable
fun AddExpenseScreen(
    viewModel: com.shohankhan.bokeya.ui.CashflowViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val categories by androidx.lifecycle.compose.collectAsStateWithLifecycle(
        viewModel.expenseCategories,
    )
    var amount by rememberSaveable { mutableStateOf("") }
    var where by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var suggestedId by remember { mutableStateOf<Long?>(null) }
    var date by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(Clocks.today()) }
    var method by rememberSaveable { mutableStateOf(PaymentMethod.CASH.name) }
    var note by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsed = Money.parseOrNull(amount)

    // Suggestion only — never auto-assigns a category without the user tapping it.
    androidx.compose.runtime.LaunchedEffect(where) {
        suggestedId = if (where.length >= 3 && categoryId == null) {
            viewModel.suggestCategory(where)
        } else {
            null
        }
    }

    FormScaffold(
        title = "নতুন খরচ",
        onBack = onBack,
        saveEnabled = parsed?.isPositive == true && where.isNotBlank(),
        onSave = {
            when {
                parsed == null || !parsed.isPositive -> error = "টাকার পরিমাণ সঠিকভাবে লিখুন।"
                where.isBlank() -> error = "কোথায় খরচ হয়েছে লিখুন।"
                else -> viewModel.addExpense(
                    parsed,
                    where.trim(),
                    date,
                    categoryId,
                    PaymentMethod.valueOf(method),
                    note.takeIf { it.isNotBlank() },
                    onSaved,
                )
            }
        },
    ) {
        MoneyInput(amount, { amount = it; error = null }, isError = error != null, errorText = error)
        BokeyaTextField(where, { where = it; error = null }, "কোথায় খরচ হয়েছে?")

        suggestedId?.let { id ->
            categories.firstOrNull { it.id == id }?.let { suggestion ->
                SecondaryButton(
                    text = "Category হিসেবে \"${suggestion.name}\" ব্যবহার করব?",
                    onClick = { categoryId = id; suggestedId = null },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        ChipSelector(
            options = categories.map { SelectorOption(it.id.toString(), it.name) },
            selectedId = categoryId?.toString(),
            onSelect = { categoryId = it.toLongOrNull(); suggestedId = null },
            label = "Category",
        )
        DateSelector(date, { date = it })
        ExpandableSection("আরও তথ্য", advanced, { advanced = !advanced }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipSelector(
                    options = PaymentMethod.entries.map { SelectorOption(it.name, it.label) },
                    selectedId = method,
                    onSelect = { method = it },
                    label = "Payment method",
                )
                BokeyaTextField(note, { note = it }, "নোট (optional)", singleLine = false)
            }
        }
    }
}
