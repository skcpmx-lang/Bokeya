package com.shohankhan.bokeya.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object Lock : Route("lock")
    data object Home : Route("home")
    data object Accounts : Route("accounts")
    data object Calendar : Route("calendar")
    data object Cashflow : Route("cashflow")
    data object More : Route("more")

    data object AddShop : Route("add/shop")
    data object AddLoan : Route("add/loan")
    data object AddEmi : Route("add/emi")
    data object AddPersonal : Route("add/personal")
    data object AddIncome : Route("add/income")
    data object AddExpense : Route("add/expense")
    data object Search : Route("search")
    data object Reports : Route("reports")
    data object Planner : Route("planner")
    data object Goals : Route("goals")
    data object Recurring : Route("recurring")
    data object Settings : Route("settings")
    data object About : Route("about")
    data object Backup : Route("backup")
    data object Categories : Route("categories")
    data object Archive : Route("archive")
    data object Overdue : Route("overdue")
    data object Timeline : Route("timeline")

    data object AccountDetail : Route("account/{id}") {
        fun of(id: Long) = "account/$id"
    }

    data object Pay : Route("pay/{id}") {
        fun of(id: Long) = "pay/$id"
    }

    data object AddPurchase : Route("purchase/{id}") {
        fun of(id: Long) = "purchase/$id"
    }
}

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomTabs = listOf(
    BottomTab(Route.Home.path, "হোম", Icons.Filled.Home),
    BottomTab(Route.Accounts.path, "হিসাব", Icons.Filled.AccountBalanceWallet),
    BottomTab(Route.Calendar.path, "ক্যালেন্ডার", Icons.Filled.CalendarMonth),
    BottomTab(Route.Cashflow.path, "আয়-ব্যয়", Icons.Filled.SwapVert),
    BottomTab(Route.More.path, "আরও", Icons.Filled.MoreHoriz),
)

object Motion {
    const val SHORT = 180
    const val MEDIUM = 280
    const val LONG = 400

    fun enter(scope: AnimatedContentTransitionScope<*>) =
        slideInHorizontally(tween(MEDIUM)) { it / 6 } + fadeIn(tween(MEDIUM))

    fun exit(scope: AnimatedContentTransitionScope<*>) =
        slideOutHorizontally(tween(MEDIUM)) { -it / 8 } + fadeOut(tween(SHORT))

    fun popEnter(scope: AnimatedContentTransitionScope<*>) =
        slideInHorizontally(tween(MEDIUM)) { -it / 8 } + fadeIn(tween(MEDIUM))

    fun popExit(scope: AnimatedContentTransitionScope<*>) =
        slideOutHorizontally(tween(MEDIUM)) { it / 6 } + fadeOut(tween(SHORT))
}
