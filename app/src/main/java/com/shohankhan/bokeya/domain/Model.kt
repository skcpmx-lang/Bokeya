package com.shohankhan.bokeya.domain

import com.shohankhan.bokeya.core.Money
import java.time.LocalDate

/** Every financial obligation in Bokeya is one of these four kinds. */
enum class AccountType(val label: String, val plural: String) {
    SHOP("দোকান", "দোকান"),
    LOAN("Loan", "Loan"),
    EMI("EMI", "EMI"),
    PERSONAL("ব্যক্তিগত ধার", "মানুষ"),
}

/** Whether the user owes money or is owed money. Kept explicit everywhere. */
enum class Direction(val label: String, val shortLabel: String) {
    I_OWE("আমি দেব", "দেব"),
    THEY_OWE("আমি পাব", "পাব"),
}

enum class AccountStatus(val label: String) {
    ACTIVE("চলমান"),
    DUE_SOON("সামনে দিতে হবে"),
    DUE_TODAY("আজ দিতে হবে"),
    OVERDUE("তারিখ পেরিয়েছে"),
    PARTIALLY_PAID("আংশিক পরিশোধ"),
    PAID("পরিশোধ হয়েছে"),
    PAUSED("বন্ধ আছে"),
    ARCHIVED("আর্কাইভ"),
}

enum class InstallmentStatus(val label: String) {
    UPCOMING("সামনে"),
    DUE_TODAY("আজ"),
    PARTIALLY_PAID("আংশিক"),
    PAID("পরিশোধ"),
    OVERDUE("পেরিয়েছে"),
}

/** Unified transaction taxonomy. */
enum class TxType(val label: String, val flow: MoneyFlow) {
    INCOME("আয়", MoneyFlow.MONEY_IN),
    EXPENSE("খরচ", MoneyFlow.MONEY_OUT),
    BORROWED("ধার নিয়েছি", MoneyFlow.MONEY_IN),
    LENT("ধার দিয়েছি", MoneyFlow.MONEY_OUT),
    SHOP_PURCHASE("দোকানে বাকি", MoneyFlow.NONE),
    PAYMENT("পরিশোধ", MoneyFlow.MONEY_OUT),
    RECEIVED("ফেরত পেয়েছি", MoneyFlow.MONEY_IN),
    LOAN("Loan নিয়েছি", MoneyFlow.MONEY_IN),
    LOAN_PAYMENT("Loan কিস্তি", MoneyFlow.MONEY_OUT),
    EMI("EMI শুরু", MoneyFlow.NONE),
    EMI_PAYMENT("EMI কিস্তি", MoneyFlow.MONEY_OUT),
    SAVING("সঞ্চয়", MoneyFlow.MONEY_OUT),
}

enum class MoneyFlow { MONEY_IN, MONEY_OUT, NONE }

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    BANK("Bank"),
    BKASH("bKash"),
    NAGAD("Nagad"),
    ROCKET("Rocket"),
    CARD("Card"),
    OTHER("অন্যান্য"),
}

enum class CategoryKind { INCOME, EXPENSE }

enum class Relationship(val label: String) {
    BROTHER("ভাই"),
    SISTER("বোন"),
    FRIEND("বন্ধু"),
    RELATIVE("আত্মীয়"),
    COLLEAGUE("কলিগ"),
    NEIGHBOUR("প্রতিবেশী"),
    OTHER("অন্যান্য"),
}

enum class Unit(val label: String) {
    KG("কেজি"),
    GRAM("গ্রাম"),
    LITRE("লিটার"),
    PIECE("পিস"),
    DOZEN("ডজন"),
    PACKET("প্যাকেট"),
    BOTTLE("বোতল"),
    CUSTOM("অন্য"),
}

enum class HealthLevel(val label: String) {
    GOOD("ভালো"),
    ATTENTION("খেয়াল রাখুন"),
    HIGH_LOAD("চাপ বেশি"),
}

/** Aggregated view of one obligation, used by lists, dashboard and detail screens. */
data class AccountSummary(
    val id: Long,
    val type: AccountType,
    val direction: Direction,
    val title: String,
    val subtitle: String?,
    val total: Money,
    val paid: Money,
    val remaining: Money,
    val nextDueDate: LocalDate?,
    val nextDueAmount: Money?,
    val status: AccountStatus,
    val archived: Boolean,
) {
    val progress: Float
        get() = if (total.poisha <= 0L) {
            if (remaining.isZero) 1f else 0f
        } else {
            (paid.poisha.toFloat() / total.poisha.toFloat()).coerceIn(0f, 1f)
        }

    val progressPercent: Int get() = (progress * 100).toInt()
}

data class UpcomingPayment(
    val accountId: Long,
    val type: AccountType,
    val title: String,
    val amount: Money,
    val dueDate: LocalDate,
    val installmentId: Long?,
)

data class TodaySnapshot(
    val toPay: Money,
    val toReceive: Money,
    val income: Money,
    val expense: Money,
) {
    val net: Money get() = income - expense
}

data class MonthSnapshot(
    val income: Money,
    val expense: Money,
    val debtPayments: Money,
    val newDebt: Money,
    val topExpenseCategory: String?,
) {
    val net: Money get() = income - expense
}

data class Insight(
    val id: String,
    val text: String,
    val tone: InsightTone,
)

enum class InsightTone { NEUTRAL, POSITIVE, WARNING }
