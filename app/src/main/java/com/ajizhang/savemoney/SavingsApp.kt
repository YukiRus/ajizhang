package com.ajizhang.savemoney

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ajizhang.savemoney.di.LlmEntryPoint
import com.ajizhang.savemoney.ui.budget.BudgetScreen
import com.ajizhang.savemoney.ui.budget.BudgetViewModel
import com.ajizhang.savemoney.ui.editor.TransactionEditorScreen
import com.ajizhang.savemoney.ui.editor.TransactionEditorViewModel
import com.ajizhang.savemoney.ui.home.HomeScreen
import com.ajizhang.savemoney.ui.home.HomeViewModel
import com.ajizhang.savemoney.ui.navigation.Routes
import com.ajizhang.savemoney.ui.settings.SettingsViewModel
import com.ajizhang.savemoney.ui.theme.SaveMoneyTheme
import com.ajizhang.savemoney.ui.trend.TrendScreen
import com.ajizhang.savemoney.ui.trend.TrendViewModel
import dagger.hilt.EntryPoints

private data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavTabs = listOf(
    BottomNavTab(Routes.HOME, "首页", Icons.Rounded.Home),
    BottomNavTab(Routes.BUDGET, "预算", Icons.Rounded.AccountBalance),
    BottomNavTab(Routes.TREND, "趋势", Icons.AutoMirrored.Rounded.TrendingUp),
)

@Composable
fun SavingsApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomNavTabs.any { it.route == currentDestination?.route }

    SaveMoneyTheme {
        Surface(modifier = Modifier) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            bottomNavTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.route == tab.route
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.padding(paddingValues),
                ) {
                    composable(route = Routes.HOME) {
                        val viewModel: HomeViewModel = hiltViewModel()
                        val settingsViewModel: SettingsViewModel = hiltViewModel()
                        val context = LocalContext.current
                        val recognizer = remember {
                            EntryPoints.get(
                                context.applicationContext,
                                LlmEntryPoint::class.java,
                            ).llmExpenseRecognizer()
                        }
                        HomeScreen(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            llmExpenseRecognizer = recognizer,
                            onAddTransaction = {
                                navController.navigate(Routes.transactionEditor())
                            },
                            onEditTransaction = { transactionId ->
                                navController.navigate(Routes.transactionEditor(transactionId))
                            },
                            onOpenTrend = {
                                navController.navigate(Routes.TREND)
                            },
                        )
                    }

                    composable(route = Routes.BUDGET) {
                        val viewModel: BudgetViewModel = hiltViewModel()
                        BudgetScreen(viewModel = viewModel)
                    }

                    composable(route = Routes.TREND) {
                        val viewModel: TrendViewModel = hiltViewModel()
                        TrendScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.TRANSACTION_EDITOR_ROUTE,
                        arguments = listOf(
                            navArgument(Routes.ARG_TRANSACTION_ID) {
                                type = NavType.LongType
                                defaultValue = Routes.NEW_TRANSACTION_ID
                            },
                        ),
                    ) {
                        val viewModel: TransactionEditorViewModel = hiltViewModel()
                        TransactionEditorScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
