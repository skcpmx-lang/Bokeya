package com.shohankhan.bokeya.data

import com.shohankhan.bokeya.data.db.CategoryEntity
import com.shohankhan.bokeya.domain.CategoryKind

/** Built-in categories seeded once on first launch. These are labels only — no financial data. */
object DefaultData {

    val expenseCategories = listOf(
        "খাবার" to "restaurant",
        "বাজার" to "shopping",
        "যাতায়াত" to "transport",
        "বাসা ভাড়া" to "home",
        "ওষুধ" to "medicine",
        "শিক্ষা" to "school",
        "বিল" to "receipt",
        "কেনাকাটা" to "bag",
        "বিনোদন" to "movie",
        "পরিবার" to "family",
        "Loan payment" to "bank",
        "EMI payment" to "card",
        "অন্যান্য" to "wallet",
    )

    val incomeCategories = listOf(
        "বেতন" to "salary",
        "ব্যবসা" to "business",
        "Freelance" to "laptop",
        "বোনাস" to "gift",
        "উপহার" to "gift",
        "ফেরত" to "refund",
        "অন্যান্য" to "wallet",
    )

    fun seed(): List<CategoryEntity> {
        val expense = expenseCategories.mapIndexed { index, (name, icon) ->
            CategoryEntity(
                name = name,
                kind = CategoryKind.EXPENSE,
                icon = icon,
                builtIn = true,
                sortOrder = index,
            )
        }
        val income = incomeCategories.mapIndexed { index, (name, icon) ->
            CategoryEntity(
                name = name,
                kind = CategoryKind.INCOME,
                icon = icon,
                builtIn = true,
                sortOrder = index,
            )
        }
        return expense + income
    }
}
