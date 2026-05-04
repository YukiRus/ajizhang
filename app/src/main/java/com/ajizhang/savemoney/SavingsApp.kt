package com.ajizhang.savemoney

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ajizhang.savemoney.di.LlmEntryPoint
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

@Composable
fun SavingsApp() {
    val navController = rememberNavController()

    SaveMoneyTheme {
        Surface(modifier = Modifier) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
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
