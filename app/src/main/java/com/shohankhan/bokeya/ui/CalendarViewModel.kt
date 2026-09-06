package com.shohankhan.bokeya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.TransactionEntity
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.domain.UpcomingPayment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayMarkers(
    val hasIncome: Boolean = false,
    val hasExpense: Boolean = false,
    val hasPayment: Boolean = false,
    val hasDue: Boolean = false,
) {
    val any: Boolean get() = hasIncome || hasExpense || hasPayment || hasDue
}

data class CalendarState(
    val month: LocalDate = Clocks.today().withDayOfMonth(1),
    val selectedDate: LocalDate = Clocks.today(),
    val markers: Map<LocalDate, DayMarkers> = emptyMap(),
    val dayTransactions: List<TransactionEntity> = emptyList(),
    val dayDues: List<UpcomingPayment> = emptyList(),
    val heatmap: Map<LocalDate, Int> = emptyMap(),
) {
    val dayIncome: Money get() = Money(dayTransactions.filter { it.type == TxType.INCOME }.sumOf { it.amount })
    val dayExpense: Money get() = Money(dayTransactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount })
    val dayDueTotal: Money get() = Money(dayDues.sumOf { it.amount.poisha })
}

class CalendarViewModel(private val repository: BokeyaRepository) : ViewModel() {

    private val _month = MutableStateFlow(Clocks.today().withDayOfMonth(1))
    private val _selected = MutableStateFlow(Clocks.today())
    private val _dues = MutableStateFlow<List<UpcomingPayment>>(emptyList())

    val state: StateFlow<CalendarState> = combine(
        _month,
        _selected,
        repository.allTransactions,
        _dues,
    ) { month, selected, transactions, dues ->
        val start = month.withDayOfMonth(1)
        val end = start.plusMonths(1).minusDays(1)

        val markers = mutableMapOf<LocalDate, DayMarkers>()
        val heat = mutableMapOf<LocalDate, Int>()

        transactions.filter { it.date >= start.toEpochDay() && it.date <= end.toEpochDay() }
            .forEach { tx ->
                val date = LocalDate.ofEpochDay(tx.date)
                val current = markers[date] ?: DayMarkers()
                markers[date] = when (tx.type) {
                    TxType.INCOME, TxType.RECEIVED, TxType.BORROWED -> current.copy(hasIncome = true)
                    TxType.EXPENSE, TxType.LENT -> current.copy(hasExpense = true)
                    TxType.PAYMENT, TxType.LOAN_PAYMENT, TxType.EMI_PAYMENT -> current.copy(hasPayment = true)
                    else -> current.copy(hasExpense = true)
                }
                heat[date] = (heat[date] ?: 0) + 1
            }

        dues.forEach { due ->
            if (due.dueDate in start..end) {
                val current = markers[due.dueDate] ?: DayMarkers()
                markers[due.dueDate] = current.copy(hasDue = true)
                heat[due.dueDate] = (heat[due.dueDate] ?: 0) + 1
            }
        }

        CalendarState(
            month = month,
            selectedDate = selected,
            markers = markers,
            dayTransactions = transactions.filter { it.date == selected.toEpochDay() },
            dayDues = dues.filter { it.dueDate == selected },
            heatmap = heat,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    init { loadDues() }

    private fun loadDues() = viewModelScope.launch {
        val start = _month.value.withDayOfMonth(1).minusMonths(1)
        val end = _month.value.plusMonths(2)
        _dues.value = repository.upcomingPayments(start, end) + repository.overduePayments()
    }

    fun setMonth(value: LocalDate) {
        _month.value = value.withDayOfMonth(1)
        loadDues()
    }

    fun previousMonth() = setMonth(_month.value.minusMonths(1))
    fun nextMonth() = setMonth(_month.value.plusMonths(1))

    fun select(date: LocalDate) {
        _selected.value = date
        if (date.withDayOfMonth(1) != _month.value) setMonth(date)
    }
}

data class PlannerBucket(
    val label: String,
    val payments: List<UpcomingPayment>,
) {
    val total: Money get() = Money(payments.sumOf { it.amount.poisha })
}

data class PlannerState(
    val today: PlannerBucket = PlannerBucket("আজ", emptyList()),
    val week: PlannerBucket = PlannerBucket("এই সপ্তাহে", emptyList()),
    val next7: PlannerBucket = PlannerBucket("আগামী ৭ দিন", emptyList()),
    val month: PlannerBucket = PlannerBucket("এই মাসে", emptyList()),
    val overdue: PlannerBucket = PlannerBucket("তারিখ পেরিয়েছে", emptyList()),
    val suggested: List<UpcomingPayment> = emptyList(),
    val loading: Boolean = true,
)

class PlannerViewModel(private val repository: BokeyaRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlannerState())
    val state = _state.asStateFlow()

    init { reload() }

    fun reload() = viewModelScope.launch {
        val today = Clocks.today()
        val overdue = repository.overduePayments(today)
        val todayList = repository.upcomingPayments(today, today)
        val weekEnd = today.plusDays((7 - today.dayOfWeek.value).toLong().coerceAtLeast(0))
        val week = repository.upcomingPayments(today, weekEnd)
        val next7 = repository.upcomingPayments(today, today.plusDays(7))
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val month = repository.upcomingPayments(today, monthEnd)

        _state.value = PlannerState(
            today = PlannerBucket("আজ", todayList),
            week = PlannerBucket("এই সপ্তাহে", week),
            next7 = PlannerBucket("আগামী ৭ দিন", next7),
            month = PlannerBucket("এই মাসে", month),
            overdue = PlannerBucket("তারিখ পেরিয়েছে", overdue),
            suggested = com.shohankhan.bokeya.domain.InsightEngine
                .prioritize(overdue + next7, today)
                .take(5),
            loading = false,
        )
    }
}
