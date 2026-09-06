package com.shohankhan.bokeya.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.bokeya.core.Clocks
import com.shohankhan.bokeya.core.Money
import com.shohankhan.bokeya.data.db.AccountEntity
import com.shohankhan.bokeya.data.db.InstallmentEntity
import com.shohankhan.bokeya.data.db.PaymentEntity
import com.shohankhan.bokeya.data.db.ReminderEntity
import com.shohankhan.bokeya.data.db.ShopItemEntity
import com.shohankhan.bokeya.data.db.ShopPurchaseEntity
import com.shohankhan.bokeya.data.repo.BokeyaRepository
import com.shohankhan.bokeya.data.repo.PaymentResult
import com.shohankhan.bokeya.domain.AccountSummary
import com.shohankhan.bokeya.domain.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DetailState(
    val loading: Boolean = true,
    val account: AccountEntity? = null,
    val summary: AccountSummary? = null,
    val installments: List<InstallmentEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val purchases: List<Pair<ShopPurchaseEntity, List<ShopItemEntity>>> = emptyList(),
)

class DetailViewModel(
    private val repository: BokeyaRepository,
    private val accountId: Long,
) : ViewModel() {

    private val purchaseItems = MutableStateFlow<Map<Long, List<ShopItemEntity>>>(emptyMap())

    private val _paymentResult = MutableStateFlow<PaymentResult?>(null)
    val paymentResult = _paymentResult.asStateFlow()

    val state: StateFlow<DetailState> = combine(
        repository.observeAccount(accountId),
        repository.summaries.map { list -> list.firstOrNull { it.id == accountId } },
        repository.observeInstallments(accountId),
        repository.observePayments(accountId),
        combine(repository.observePurchases(accountId), purchaseItems) { purchases, items ->
            purchases to items
        },
    ) { account, summary, installments, payments, (purchases, items) ->
        DetailState(
            loading = false,
            account = account,
            summary = summary,
            installments = installments,
            payments = payments,
            purchases = purchases.map { it to (items[it.id] ?: emptyList()) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    init {
        viewModelScope.launch {
            repository.refreshInstallmentStatuses(accountId)
            repository.observePurchases(accountId).collect { purchases ->
                purchaseItems.value = purchases.associate { it.id to repository.purchaseItems(it.id) }
            }
        }
    }

    fun pay(
        amount: Money,
        date: LocalDate = Clocks.today(),
        method: PaymentMethod = PaymentMethod.CASH,
        note: String? = null,
        onResult: (PaymentResult) -> Unit = {},
    ) = viewModelScope.launch {
        val result = repository.recordPayment(accountId, amount, date, method, note)
        _paymentResult.value = result
        onResult(result)
    }

    fun clearPaymentResult() { _paymentResult.value = null }

    fun deletePayment(paymentId: Long) = viewModelScope.launch {
        repository.deletePayment(paymentId, accountId)
    }

    fun archive(archived: Boolean) = viewModelScope.launch {
        repository.setArchived(accountId, archived)
    }

    fun setPaused(paused: Boolean) = viewModelScope.launch {
        repository.setPaused(accountId, paused)
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteAccount(accountId)
        onDone()
    }

    fun updateTitle(title: String, note: String?) = viewModelScope.launch {
        repository.account(accountId)?.let {
            repository.updateAccount(it.copy(title = title, note = note))
        }
    }

    fun addReminder(title: String, message: String, at: Long) = viewModelScope.launch {
        repository.upsertReminder(
            ReminderEntity(
                accountId = accountId,
                title = title,
                message = message,
                triggerAt = at,
                createdAt = 0,
            ),
        )
    }

    fun addPurchase(items: List<ShopItemEntity>, date: LocalDate, note: String?, onDone: () -> Unit) =
        viewModelScope.launch {
            repository.addPurchase(accountId, items, date, note)
            onDone()
        }

    fun deletePurchase(purchaseId: Long) = viewModelScope.launch {
        repository.deletePurchase(purchaseId, accountId)
    }
}
