package com.shohankhan.bokeya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.CategoryEntity
import com.shohankhan.bokeya.data.db.GoalEntity
import com.shohankhan.bokeya.data.db.RecurringEntity
import com.shohankhan.bokeya.data.db.TransactionEntity
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.data.repo.SearchResults
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.CategoryKind
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.MonthSnapshot
import com.shohankhan.bokeya.domain.PaymentMethod
import com.shohankhan.bokeya.domain.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TxFilter(val label: String) {
    ALL("সব"),
    INCOME("আয়"),
    EXPENSE("খরচ"),
    PAYMENT("পরিশোধ"),
    DEBT("ধার"),
}

data class CashflowState(
    val month: LocalDate = Clocks.today().withDayOfMonth(1),
    val transactions: List<TransactionEntity> = emptyList(),
    val income: Money = Money.ZERO,
    val expense: Money = Money.ZERO,
    val categoryBreakdown: List<Pair<String, Money>> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val loading: Boolean = true,
) {
    val net: Money get() = income - expense
}

class CashflowViewModel(private val repository: BokeyaRepository) : ViewModel() {

    private val _month = MutableStateFlow(Clocks.today().withDayOfMonth(1))
    val month = _month.asStateFlow()

    private val _filter = MutableStateFlow(TxFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Long?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _breakdown = MutableStateFlow<List<Pair<String, Money>>>(emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.expenseCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = repository.incomeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<CashflowState> = combine(
        _month,
        _filter,
        _selectedCategory,
        repository.allTransactions,
        repository.allCategories(),
    ) { month, filter, category, allTx, categories ->
        val start = month.withDayOfMonth(1)
        val end = start.plusMonths(1).minusDays(1)
        val monthTx = allTx.filter { it.date >= start.toEpochDay() && it.date <= end.toEpochDay() }
        val filtered = monthTx
            .filter { matches(it, filter) }
            .filter { category == null || it.categoryId == category }

        val categoryMap = categories.associateBy { it.id }
        val breakdown = monthTx
            .filter { it.type == TxType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (id, list) ->
                (id?.let { categoryMap[it]?.name } ?: "অন্যান্য") to Money(list.sumOf { it.amount })
            }
            .sortedByDescending { it.second.poisha }

        CashflowState(
            month = month,
            transactions = filtered,
            income = Money(monthTx.filter { it.type == TxType.INCOME }.sumOf { it.amount }),
            expense = Money(monthTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }),
            categoryBreakdown = breakdown,
            categories = categoryMap,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashflowState())

    private fun matches(tx: TransactionEntity, filter: TxFilter) = when (filter) {
        TxFilter.ALL -> true
        TxFilter.INCOME -> tx.type == TxType.INCOME
        TxFilter.EXPENSE -> tx.type == TxType.EXPENSE
        TxFilter.PAYMENT -> tx.type in setOf(TxType.PAYMENT, TxType.LOAN_PAYMENT, TxType.EMI_PAYMENT)
        TxFilter.DEBT -> tx.type in setOf(TxType.BORROWED, TxType.LENT, TxType.SHOP_PURCHASE, TxType.RECEIVED)
    }

    fun setMonth(value: LocalDate) { _month.value = value.withDayOfMonth(1) }
    fun previousMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
    fun setFilter(value: TxFilter) { _filter.value = value }
    fun selectCategory(id: Long?) { _selectedCategory.value = id }

    fun addIncome(
        amount: Money,
        source: String,
        date: LocalDate,
        categoryId: Long?,
        note: String?,
        onDone: () -> Unit,
    ) = viewModelScope.launch {
        repository.addTransaction(TxType.INCOME, amount, source, date, categoryId, note = note)
        onDone()
    }

    fun addExpense(
        amount: Money,
        where: String,
        date: LocalDate,
        categoryId: Long?,
        method: PaymentMethod?,
        note: String?,
        onDone: () -> Unit,
    ) = viewModelScope.launch {
        repository.addTransaction(TxType.EXPENSE, amount, where, date, categoryId, method, note)
        onDone()
    }

    fun deleteTransaction(id: Long) = viewModelScope.launch { repository.deleteTransaction(id) }

    fun deleteTransactions(ids: List<Long>) = viewModelScope.launch { repository.deleteTransactions(ids) }

    suspend fun suggestCategory(title: String): Long? = repository.suggestCategory(title)

    fun addCategory(name: String, kind: CategoryKind) = viewModelScope.launch {
        repository.addCategory(name, kind)
    }

    fun deleteCategory(id: Long) = viewModelScope.launch { repository.deleteCategory(id) }
}

class SearchViewModel(private val repository: BokeyaRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResults())
    val results = _results.asStateFlow()

    private val _minAmount = MutableStateFlow("")
    val minAmount = _minAmount.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
        viewModelScope.launch {
            _results.value = if (value.isBlank()) SearchResults() else repository.search(value)
        }
    }

    fun setMinAmount(value: String) { _minAmount.value = value }
}

class GoalsViewModel(private val repository: BokeyaRepository) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> = repository.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val debtProgress: StateFlow<Triple<Money, Money, Float>> = repository.summaries
        .map { list ->
            val owed = list.filter { it.direction == Direction.I_OWE }
            val total = Money(owed.sumOf { it.total.poisha })
            val paid = Money(owed.sumOf { it.paid.poisha })
            val progress = if (total.isPositive) {
                (paid.poisha.toFloat() / total.poisha.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Triple(total, paid, progress)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Triple(Money.ZERO, Money.ZERO, 0f),
        )

    fun addGoal(title: String, target: Money, kind: String, note: String?) = viewModelScope.launch {
        repository.upsertGoal(
            GoalEntity(
                title = title,
                targetAmount = target.poisha,
                kind = kind,
                note = note,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    fun contribute(goalId: Long, amount: Money) = viewModelScope.launch {
        repository.addToGoal(goalId, amount)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.deleteGoal(id) }
}

class RecurringViewModel(private val repository: BokeyaRepository) : ViewModel() {

    val items: StateFlow<List<RecurringEntity>> = repository.recurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(
        title: String,
        type: TxType,
        amount: Money,
        frequency: Frequency,
        anchor: LocalDate,
        categoryId: Long?,
        autoCreate: Boolean,
    ) = viewModelScope.launch {
        repository.upsertRecurring(
            RecurringEntity(
                title = title,
                type = type,
                amount = amount.poisha,
                categoryId = categoryId,
                frequency = frequency,
                anchorDate = anchor.toEpochDay(),
                nextRun = anchor.toEpochDay(),
                autoCreate = autoCreate,
                createdAt = 0,
            ),
        )
    }

    fun toggle(item: RecurringEntity) = viewModelScope.launch {
        repository.upsertRecurring(item.copy(active = !item.active))
    }

    fun delete(id: Long) = viewModelScope.launch { repository.deleteRecurring(id) }

    fun runNow() = viewModelScope.launch { repository.runDueRecurring() }
}
