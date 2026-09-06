package com.shohankhan.bokeya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Frequency
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.AccountEntity
import com.shohankhan.bokeya.data.db.InstallmentEntity
import com.shohankhan.bokeya.data.db.PaymentEntity
import com.shohankhan.bokeya.data.db.PersonEntity
import com.shohankhan.bokeya.data.db.ShopItemEntity
import com.shohankhan.bokeya.data.db.ShopPurchaseEntity
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.data.repo.PaymentResult
import com.shohankhan.bokeya.domain.AccountStatus
import com.shohankhan.bokeya.domain.AccountSummary
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.domain.Direction
import com.shohankhan.bokeya.domain.PaymentMethod
import com.shohankhan.bokeya.domain.Relationship
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

enum class AccountFilter(val label: String) {
    ALL("সব"),
    I_OWE("আমি দেব"),
    THEY_OWE("আমি পাব"),
    UNPAID("বাকি"),
    OVERDUE("পেরিয়েছে"),
    PAID("পরিশোধ"),
}

enum class SortOrder(val label: String) {
    NEWEST("নতুন আগে"),
    OLDEST("পুরনো আগে"),
    HIGHEST("বেশি টাকা"),
    LOWEST("কম টাকা"),
    NEAREST_DUE("কাছের তারিখ"),
}

class AccountsViewModel(private val repository: BokeyaRepository) : ViewModel() {

    private val _selectedType = MutableStateFlow<AccountType?>(null)
    val selectedType = _selectedType.asStateFlow()

    private val _filter = MutableStateFlow(AccountFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _sort = MutableStateFlow(SortOrder.NEWEST)
    val sort = _sort.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived = _showArchived.asStateFlow()

    val people: StateFlow<List<PersonEntity>> = repository.people
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summaries: StateFlow<List<AccountSummary>> = combine(
        repository.summaries,
        _selectedType,
        _filter,
        _sort,
        _showArchived,
    ) { all, type, filter, sort, archived ->
        all.asSequence()
            .filter { it.archived == archived }
            .filter { type == null || it.type == type }
            .filter { matches(it, filter) }
            .sortedWith(comparator(sort))
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSummaries: StateFlow<List<AccountSummary>> = repository.summaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val counts: StateFlow<Map<AccountType, Int>> = repository.summaries
        .map { list -> list.filter { !it.archived }.groupingBy { it.type }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private fun matches(summary: AccountSummary, filter: AccountFilter) = when (filter) {
        AccountFilter.ALL -> true
        AccountFilter.I_OWE -> summary.direction == Direction.I_OWE
        AccountFilter.THEY_OWE -> summary.direction == Direction.THEY_OWE
        AccountFilter.UNPAID -> !summary.remaining.isZero
        AccountFilter.OVERDUE -> summary.status == AccountStatus.OVERDUE
        AccountFilter.PAID -> summary.remaining.isZero && summary.total.isPositive
    }

    private fun comparator(sort: SortOrder): Comparator<AccountSummary> = when (sort) {
        SortOrder.NEWEST -> compareByDescending { it.id }
        SortOrder.OLDEST -> compareBy { it.id }
        SortOrder.HIGHEST -> compareByDescending { it.remaining.poisha }
        SortOrder.LOWEST -> compareBy { it.remaining.poisha }
        SortOrder.NEAREST_DUE -> compareBy(
            { it.nextDueDate?.toEpochDay() ?: Long.MAX_VALUE },
            { -it.remaining.poisha },
        )
    }

    fun selectType(type: AccountType?) { _selectedType.value = type }
    fun setFilter(value: AccountFilter) { _filter.value = value }
    fun setSort(value: SortOrder) { _sort.value = value }
    fun toggleArchived() { _showArchived.value = !_showArchived.value }

    fun archive(id: Long, archived: Boolean) = viewModelScope.launch {
        repository.setArchived(id, archived)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.deleteAccount(id) }

    // ------------------------------------------------------------------ creation

    fun createShop(
        name: String,
        note: String?,
        dueDate: LocalDate?,
        onDone: (Long) -> Unit,
    ) = viewModelScope.launch {
        val id = repository.createAccount(
            AccountEntity(
                type = AccountType.SHOP,
                direction = Direction.I_OWE,
                title = name,
                note = note,
                dueDate = dueDate?.toEpochDay(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        onDone(id)
    }

    fun createPersonalDebt(
        personName: String,
        relationship: Relationship,
        phone: String?,
        amount: Money,
        direction: Direction,
        date: LocalDate,
        dueDate: LocalDate?,
        note: String?,
        onDone: (Long) -> Unit,
    ) = viewModelScope.launch {
        val existing = repository.people.let { null }
        val personId = repository.upsertPerson(
            PersonEntity(
                name = personName,
                relationship = relationship,
                phone = phone,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val id = repository.createAccount(
            AccountEntity(
                type = AccountType.PERSONAL,
                direction = direction,
                title = personName,
                subtitle = relationship.label,
                principal = amount.poisha,
                personId = personId,
                startDate = date.toEpochDay(),
                dueDate = dueDate?.toEpochDay(),
                note = note,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        repository.addTransaction(
            type = if (direction == Direction.I_OWE) TxType.BORROWED else TxType.LENT,
            amount = amount,
            title = personName,
            date = date,
            accountId = id,
            personId = personId,
            note = note,
        )
        onDone(id)
    }

    fun createLoan(
        institution: String,
        loanName: String,
        principal: Money,
        interest: Money,
        installmentAmount: Money,
        installmentCount: Int,
        frequency: Frequency,
        startDate: LocalDate,
        note: String?,
        onDone: (Long) -> Unit,
    ) = viewModelScope.launch {
        val id = repository.createAccount(
            AccountEntity(
                type = AccountType.LOAN,
                direction = Direction.I_OWE,
                title = loanName,
                subtitle = institution,
                institution = institution,
                principal = principal.poisha,
                interestAmount = interest.poisha,
                installmentAmount = installmentAmount.poisha,
                installmentCount = installmentCount,
                frequency = frequency,
                startDate = startDate.toEpochDay(),
                dueDate = startDate.toEpochDay(),
                note = note,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        repository.addTransaction(
            type = TxType.LOAN,
            amount = principal,
            title = loanName,
            date = startDate,
            accountId = id,
            note = note,
        )
        onDone(id)
    }

    fun createEmi(
        product: String,
        brand: String?,
        seller: String?,
        totalPrice: Money,
        downPayment: Money,
        installmentAmount: Money,
        installmentCount: Int,
        frequency: Frequency,
        startDate: LocalDate,
        method: PaymentMethod?,
        note: String?,
        onDone: (Long) -> Unit,
    ) = viewModelScope.launch {
        val financed = (totalPrice - downPayment).clampAtZero()
        val id = repository.createAccount(
            AccountEntity(
                type = AccountType.EMI,
                direction = Direction.I_OWE,
                title = product,
                subtitle = brand,
                brand = brand,
                seller = seller,
                principal = financed.poisha,
                downPayment = downPayment.poisha,
                installmentAmount = installmentAmount.poisha,
                installmentCount = installmentCount,
                frequency = frequency,
                startDate = startDate.toEpochDay(),
                dueDate = startDate.toEpochDay(),
                paymentMethod = method,
                note = note,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        repository.addTransaction(
            type = TxType.EMI,
            amount = financed,
            title = product,
            date = startDate,
            accountId = id,
            note = note,
        )
        onDone(id)
    }

    fun addPurchase(
        accountId: Long,
        items: List<ShopItemEntity>,
        date: LocalDate,
        note: String?,
        onDone: () -> Unit,
    ) = viewModelScope.launch {
        repository.addPurchase(accountId, items, date, note)
        onDone()
    }
}
