package com.shohankhan.bokeya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.TransactionEntity
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.data.repo.BokeyaSettings
import com.shohankhan.bokeya.data.repo.SettingsStore
import com.shohankhan.bokeya.domain.AccountStatus
import com.shohankhan.bokeya.domain.AccountSummary
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.HealthLevel
import com.shohankhan.bokeya.domain.Insight
import com.shohankhan.bokeya.domain.InsightEngine
import com.shohankhan.bokeya.domain.MonthSnapshot
import com.shohankhan.bokeya.domain.MoneyFlow
import com.shohankhan.bokeya.domain.TodaySnapshot
import com.shohankhan.bokeya.domain.TxType
import com.shohankhan.bokeya.domain.UpcomingPayment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardState(
    val loading: Boolean = true,
    val userName: String = "",
    val totalIOwe: Money = Money.ZERO,
    val totalTheyOwe: Money = Money.ZERO,
    val breakdown: Map<AccountType, Money> = emptyMap(),
    val today: TodaySnapshot = TodaySnapshot(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO),
    val month: MonthSnapshot = MonthSnapshot(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, null),
    val upcoming: List<UpcomingPayment> = emptyList(),
    val overdue: List<UpcomingPayment> = emptyList(),
    val insights: List<Insight> = emptyList(),
    val health: HealthLevel = HealthLevel.GOOD,
    val healthMessage: String = "",
    val recent: List<TransactionEntity> = emptyList(),
    val hasAnyData: Boolean = false,
    val debtFreeProgress: Float = 0f,
    val totalObligations: Money = Money.ZERO,
    val totalPaid: Money = Money.ZERO,
) {
    val overdueTotal: Money get() = Money(overdue.sumOf { it.amount.poisha })
    val weekTotal: Money
        get() {
            val end = Clocks.today().plusDays(7)
            return Money(upcoming.filter { !it.dueDate.isAfter(end) }.sumOf { it.amount.poisha })
        }
}

class DashboardViewModel(
    private val repository: BokeyaRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0L)

    val settings: StateFlow<BokeyaSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BokeyaSettings())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DashboardState> = combine(
        repository.summaries,
        repository.allTransactions,
        repository.allPayments,
        settingsStore.settings,
        refreshTrigger,
    ) { summaries, transactions, _, settings, _ ->
        build(summaries, transactions, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private suspend fun build(
        summaries: List<AccountSummary>,
        transactions: List<TransactionEntity>,
        settings: BokeyaSettings,
    ): DashboardState {
        val today = Clocks.today()
        val active = summaries.filter { !it.archived }

        val iOwe = active.filter { it.direction == Direction.I_OWE }
        val theyOwe = active.filter { it.direction == Direction.THEY_OWE }

        val totalIOwe = Money(iOwe.sumOf { it.remaining.poisha })
        val totalTheyOwe = Money(theyOwe.sumOf { it.remaining.poisha })

        val breakdown = AccountType.entries.associateWith { type ->
            Money(iOwe.filter { it.type == type }.sumOf { it.remaining.poisha })
        }

        val upcoming = repository.upcomingPayments(today, today.plusMonths(2))
        val overdue = repository.overduePayments(today)

        val todayTx = transactions.filter { it.date == today.toEpochDay() }
        val todaySnapshot = TodaySnapshot(
            toPay = Money(upcoming.filter { it.dueDate == today }.sumOf { it.amount.poisha }),
            toReceive = Money(
                theyOwe.filter { it.nextDueDate == today }.sumOf { it.remaining.poisha },
            ),
            income = Money(todayTx.filter { it.type == TxType.INCOME }.sumOf { it.amount }),
            expense = Money(todayTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }),
        )

        val monthStart = today.withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)
        val monthTx = transactions.filter {
            it.date >= monthStart.toEpochDay() && it.date <= monthEnd.toEpochDay()
        }
        val prevStart = monthStart.minusMonths(1)
        val prevEnd = monthStart.minusDays(1)
        val prevExpense = Money(
            transactions.filter {
                it.type == TxType.EXPENSE &&
                    it.date >= prevStart.toEpochDay() && it.date <= prevEnd.toEpochDay()
            }.sumOf { it.amount },
        )

        val monthIncome = Money(monthTx.filter { it.type == TxType.INCOME }.sumOf { it.amount })
        val monthExpense = Money(monthTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amount })
        val monthDebtPayments = Money(
            monthTx.filter {
                it.type == TxType.PAYMENT || it.type == TxType.LOAN_PAYMENT || it.type == TxType.EMI_PAYMENT
            }.sumOf { it.amount },
        )
        val monthNewDebt = Money(
            monthTx.filter {
                it.type == TxType.BORROWED || it.type == TxType.SHOP_PURCHASE || it.type == TxType.LOAN
            }.sumOf { it.amount },
        )

        val categoryTotals = repository.categoryTotals(TxType.EXPENSE, monthStart, monthEnd)
        val topCategory = categoryTotals.firstOrNull()
        val topCategoryName = topCategory?.name
        val topCategoryAmount = Money(topCategory?.total ?: 0L)

        val monthSnapshot = MonthSnapshot(
            income = monthIncome,
            expense = monthExpense,
            debtPayments = monthDebtPayments,
            newDebt = monthNewDebt,
            topExpenseCategory = topCategoryName,
        )

        val next7 = upcoming.filter { !it.dueDate.isAfter(today.plusDays(7)) }
        val next7Total = Money(next7.sumOf { it.amount.poisha })
        val nearestDays = upcoming.minByOrNull { it.dueDate }
            ?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it.dueDate) }

        val totalObligations = Money(iOwe.sumOf { it.total.poisha })
        val totalPaid = Money(iOwe.sumOf { it.paid.poisha })

        val insights = InsightEngine.generate(
            InsightEngine.Input(
                today = today,
                totalIOwe = totalIOwe,
                totalTheyOwe = totalTheyOwe,
                breakdown = breakdown,
                overdueCount = overdue.size,
                overdueAmount = Money(overdue.sumOf { it.amount.poisha }),
                next7DaysAmount = next7Total,
                next7DaysCount = next7.size,
                monthIncome = monthIncome,
                monthExpense = monthExpense,
                previousMonthExpense = prevExpense,
                topExpenseCategory = topCategoryName,
                topExpenseAmount = topCategoryAmount,
                nearestDueDays = nearestDays,
                totalPaidAllTime = totalPaid,
            ),
        )

        val (health, healthMessage) = InsightEngine.health(
            totalIOwe, overdue.size, next7Total, monthIncome, monthExpense,
        )

        return DashboardState(
            loading = false,
            userName = settings.userName,
            totalIOwe = totalIOwe,
            totalTheyOwe = totalTheyOwe,
            breakdown = breakdown,
            today = todaySnapshot,
            month = monthSnapshot,
            upcoming = InsightEngine.prioritize(upcoming, today).take(12),
            overdue = overdue,
            insights = insights,
            health = health,
            healthMessage = healthMessage,
            recent = transactions.take(5),
            hasAnyData = summaries.isNotEmpty() || transactions.isNotEmpty(),
            debtFreeProgress = if (totalObligations.isPositive) {
                (totalPaid.poisha.toFloat() / totalObligations.poisha.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            },
            totalObligations = totalObligations,
            totalPaid = totalPaid,
        )
    }

    fun refresh() {
        viewModelScope.launch {
            repository.runDueRecurring()
            refreshTrigger.value = System.currentTimeMillis()
        }
    }
}
