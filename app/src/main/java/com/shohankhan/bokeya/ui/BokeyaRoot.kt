package com.shohankhan.bokeya.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shohankhan.bokeya.AppContainer
import com.shohankhan.bokeya.data.repo.BokeyaSettings
import com.shohankhan.bokeya.domain.AccountType
import com.shohankhan.bokeya.notifications.DeepLinks
import com.shohankhan.bokeya.ui.screens.AboutScreen
import com.shohankhan.bokeya.ui.screens.AccountDetailScreen
import com.shohankhan.bokeya.ui.screens.AccountsScreen
import com.shohankhan.bokeya.ui.screens.AddEmiScreen
import com.shohankhan.bokeya.ui.screens.AddExpenseScreen
import com.shohankhan.bokeya.ui.screens.AddIncomeScreen
import com.shohankhan.bokeya.ui.screens.AddLoanScreen
import com.shohankhan.bokeya.ui.screens.AddPersonalScreen
import com.shohankhan.bokeya.ui.screens.AddPurchaseScreen
import com.shohankhan.bokeya.ui.screens.AddShopScreen
import com.shohankhan.bokeya.ui.screens.ArchiveScreen
import com.shohankhan.bokeya.ui.screens.BackupScreen
import com.shohankhan.bokeya.ui.screens.CalendarScreen
import com.shohankhan.bokeya.ui.screens.CashflowScreen
import com.shohankhan.bokeya.ui.screens.CategoriesScreen
import com.shohankhan.bokeya.ui.screens.DashboardScreen
import com.shohankhan.bokeya.ui.screens.GoalsScreen
import com.shohankhan.bokeya.ui.screens.LockScreen
import com.shohankhan.bokeya.ui.screens.MoreScreen
import com.shohankhan.bokeya.ui.screens.OnboardingScreen
import com.shohankhan.bokeya.ui.screens.OverdueScreen
import com.shohankhan.bokeya.ui.screens.PaymentScreen
import com.shohankhan.bokeya.ui.screens.PlannerScreen
import com.shohankhan.bokeya.ui.screens.RecurringScreen
import com.shohankhan.bokeya.ui.screens.ReportsScreen
import com.shohankhan.bokeya.ui.screens.SearchScreen
import com.shohankhan.bokeya.ui.screens.SettingsScreen
import com.shohankhan.bokeya.ui.screens.TimelineScreen
import com.shohankhan.bokeya.ui.theme.bokeya
import kotlinx.coroutines.launch

@Composable
fun BokeyaRoot(
    container: AppContainer,
    settings: BokeyaSettings,
    activity: FragmentActivity,
    deepLink: String?,
    onDeepLinkHandled: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    val needsLock = settings.appLockEnabled && settings.hasPin && !unlocked

    when {
        !settings.onboarded -> {
            val settingsVm: SettingsViewModel = viewModel(factory = BokeyaViewModels.Factory)
            OnboardingScreen(
                onFinish = { name ->
                    if (name.isNotBlank()) settingsVm.setName(name)
                    settingsVm.setOnboarded(true)
                },
            )
        }
        needsLock -> {
            LockScreen(
                biometricEnabled = settings.biometricEnabled,
                activity = activity,
                onVerify = { pin -> com.shohankhan.bokeya.security.PinCodec.verify(pin, settings.pinHash) },
                onUnlocked = { unlocked = true },
            )
        }
        else -> MainShell(container, settings, activity, deepLink, onDeepLinkHandled)
    }
}

@Composable
private fun MainShell(
    container: AppContainer,
    settings: BokeyaSettings,
    activity: FragmentActivity,
    deepLink: String?,
    onDeepLinkHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var fabExpanded by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    LaunchedEffect(deepLink) {
        val link = deepLink ?: return@LaunchedEffect
        val target = when {
            link.startsWith("bokeya://pay/") ->
                link.removePrefix("bokeya://pay/").toLongOrNull()?.let { Route.Pay.of(it) }
            link.startsWith("bokeya://account/") ->
                link.removePrefix("bokeya://account/").toLongOrNull()?.let { Route.AccountDetail.of(it) }
            link == DeepLinks.ADD_EXPENSE -> Route.AddExpense.path
            link == DeepLinks.ADD_INCOME -> Route.AddIncome.path
            link == DeepLinks.ADD_DEBT -> Route.AddShop.path
            link == DeepLinks.ACCOUNTS -> Route.Accounts.path
            link == DeepLinks.CALENDAR -> Route.Calendar.path
            else -> null
        }
        target?.let { navController.navigate(it) }
        onDeepLinkHandled()
    }

    fun go(route: String) {
        fabExpanded = false
        navController.navigate(route)
    }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(tween(Motion.SHORT)),
                exit = fadeOut(tween(Motion.SHORT)),
            ) {
                BokeyaBottomBar(navController, currentRoute)
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                ExpandableFab(
                    expanded = fabExpanded,
                    onToggle = { fabExpanded = !fabExpanded },
                    onAction = { go(it) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Route.Home.path,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                enterTransition = { Motion.enter(this) },
                exitTransition = { Motion.exit(this) },
                popEnterTransition = { Motion.popEnter(this) },
                popExitTransition = { Motion.popExit(this) },
            ) {
                composable(Route.Home.path) {
                    val vm: DashboardViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    val state by vm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) { vm.refresh() }
                    DashboardScreen(
                        state = state,
                        onSearch = { go(Route.Search.path) },
                        onQuickAction = { key -> go(routeForQuickAction(key)) },
                        onAccountClick = { go(Route.AccountDetail.of(it)) },
                        onSeeAllUpcoming = { go(Route.Planner.path) },
                        onSeeAllTransactions = { go(Route.Timeline.path) },
                        onOverdueClick = { go(Route.Overdue.path) },
                        contentPadding = padding,
                    )
                }

                composable(Route.Accounts.path) {
                    val vm: AccountsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    AccountsScreen(
                        viewModel = vm,
                        onAccountClick = { go(Route.AccountDetail.of(it)) },
                        onAdd = { type -> go(routeForType(type)) },
                        onSearch = { go(Route.Search.path) },
                        onArchive = { go(Route.Archive.path) },
                        contentPadding = padding,
                    )
                }

                composable(Route.Calendar.path) {
                    val vm: CalendarViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    CalendarScreen(
                        viewModel = vm,
                        onAccountClick = { go(Route.AccountDetail.of(it)) },
                        contentPadding = padding,
                    )
                }

                composable(Route.Cashflow.path) {
                    val vm: CashflowViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    CashflowScreen(
                        viewModel = vm,
                        onAddIncome = { go(Route.AddIncome.path) },
                        onAddExpense = { go(Route.AddExpense.path) },
                        onUndo = { notify(it) },
                        contentPadding = padding,
                    )
                }

                composable(Route.More.path) {
                    MoreScreen(
                        onNavigate = { go(it) },
                        contentPadding = padding,
                    )
                }

                composable(Route.Search.path) {
                    val vm: SearchViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    SearchScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onAccountClick = { go(Route.AccountDetail.of(it)) },
                    )
                }

                composable(Route.Planner.path) {
                    val vm: PlannerViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    PlannerScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onPay = { go(Route.Pay.of(it)) },
                    )
                }

                composable(Route.Overdue.path) {
                    val vm: PlannerViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    OverdueScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onPay = { go(Route.Pay.of(it)) },
                    )
                }

                composable(Route.Timeline.path) {
                    val vm: CashflowViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    TimelineScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Route.Reports.path) {
                    val vm: ReportsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    val settingsVm: SettingsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    ReportsScreen(
                        viewModel = vm,
                        settingsViewModel = settingsVm,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Route.Goals.path) {
                    val vm: GoalsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    GoalsScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Route.Recurring.path) {
                    val vm: RecurringViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    RecurringScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Route.Settings.path) {
                    val vm: SettingsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    SettingsScreen(
                        viewModel = vm,
                        activity = activity,
                        onBack = { navController.popBackStack() },
                        onNavigate = { go(it) },
                    )
                }

                composable(Route.Backup.path) {
                    val vm: SettingsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    BackupScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Route.Categories.path) {
                    val vm: CashflowViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    CategoriesScreen(viewModel = vm, onBack = { navController.popBackStack() })
                }

                composable(Route.Archive.path) {
                    val vm: AccountsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    ArchiveScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onAccountClick = { go(Route.AccountDetail.of(it)) },
                    )
                }

                composable(Route.About.path) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable(Route.AddShop.path) {
                    val vm: AccountsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    AddShopScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onCreated = { id ->
                            navController.popBackStack()
                            go(Route.AddPurchase.of(id))
                        },
                    )
                }

                composable(Route.AddLoan.path) {
                    val vm: AccountsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    AddLoanScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onCreated = {
                            navController.popBackStack()
                            notify("Loan যোগ হয়েছে।")
                        },
                    )
                }

                composable(Route.AddEmi.path) {
                    val vm: AccountsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    AddEmiScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onCreated = {
                            navController.popBackStack()
                            notify("EMI যোগ হয়েছে।")
                        },
                    )
                }

                composable(Route.AddPersonal.path) {
                    val vm: AccountsViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    AddPersonalScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onCreated = {
                            navController.popBackStack()
                            notify("হিসাব যোগ হয়েছে।")
                        },
                    )
                }

                composable(Route.AddIncome.path) {
                    val vm: CashflowViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    AddIncomeScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.popBackStack()
                            notify("আয় যোগ হয়েছে।")
                        },
                    )
                }

                composable(Route.AddExpense.path) {
                    val vm: CashflowViewModel = viewModel(factory = BokeyaViewModels.Factory)
                    AddExpenseScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.popBackStack()
                            notify("খরচ যোগ হয়েছে।")
                        },
                    )
                }

                composable(
                    Route.AccountDetail.path,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    val vm: DetailViewModel = viewModel(
                        factory = BokeyaViewModels.detailFactory(container, id),
                    )
                    AccountDetailScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onPay = { go(Route.Pay.of(id)) },
                        onAddPurchase = { go(Route.AddPurchase.of(id)) },
                        onDeleted = { navController.popBackStack() },
                    )
                }

                composable(
                    Route.Pay.path,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    val vm: DetailViewModel = viewModel(
                        factory = BokeyaViewModels.detailFactory(container, id),
                    )
                    PaymentScreen(
                        viewModel = vm,
                        confettiEnabled = settings.confettiEnabled,
                        hapticsEnabled = settings.hapticsEnabled,
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                    )
                }

                composable(
                    Route.AddPurchase.path,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    val vm: DetailViewModel = viewModel(
                        factory = BokeyaViewModels.detailFactory(container, id),
                    )
                    AddPurchaseScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.popBackStack()
                            notify("বাকি যোগ হয়েছে।")
                        },
                    )
                }
            }

            // Scrim behind the expanded FAB menu
            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(tween(Motion.SHORT)),
                exit = fadeOut(tween(Motion.SHORT)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { fabExpanded = false },
                )
            }
        }
    }
}

private fun routeForQuickAction(key: String) = when (key) {
    "shop" -> Route.AddShop.path
    "loan" -> Route.AddLoan.path
    "emi" -> Route.AddEmi.path
    "personal" -> Route.AddPersonal.path
    "income" -> Route.AddIncome.path
    else -> Route.AddExpense.path
}

private fun routeForType(type: AccountType) = when (type) {
    AccountType.SHOP -> Route.AddShop.path
    AccountType.LOAN -> Route.AddLoan.path
    AccountType.EMI -> Route.AddEmi.path
    AccountType.PERSONAL -> Route.AddPersonal.path
}

@Composable
private fun BokeyaBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.bokeya.elevatedSurface,
        tonalElevation = 0.dp,
    ) {
        bottomTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            popUpTo(Route.Home.path) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label, Modifier.size(22.dp)) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun ExpandableFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAction: (String) -> Unit,
) {
    val actions = listOf(
        Triple(Icons.Filled.Storefront, "বাকি", Route.AddShop.path),
        Triple(Icons.Filled.Payments, "পরিশোধ", Route.Accounts.path),
        Triple(Icons.Filled.AccountBalance, "Loan", Route.AddLoan.path),
        Triple(Icons.Filled.CreditCard, "EMI", Route.AddEmi.path),
        Triple(Icons.Filled.People, "ধার", Route.AddPersonal.path),
        Triple(Icons.Filled.TrendingUp, "আয়", Route.AddIncome.path),
        Triple(Icons.Filled.TrendingDown, "খরচ", Route.AddExpense.path),
    )

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(Motion.SHORT)) + scaleIn(tween(Motion.MEDIUM), initialScale = 0.85f),
            exit = fadeOut(tween(Motion.SHORT)) + scaleOut(tween(Motion.SHORT), targetScale = 0.85f),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                actions.forEach { (icon, label, route) ->
                    FabAction(icon, label) { onAction(route) }
                }
            }
        }
        FloatingActionButton(
            onClick = onToggle,
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "বন্ধ করুন" else "নতুন যোগ করুন",
            )
        }
    }
}

@Composable
private fun FabAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.bokeya.elevatedSurface,
            shadowElevation = 2.dp,
        ) {
            Text(
                label,
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .size(46.dp)
                .clickable(onClick = onClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
