package com.alphadragon.pos.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alphadragon.pos.ui.auth.LoginScreen
import com.alphadragon.pos.ui.products.CategoryDetailScreen
import com.alphadragon.pos.ui.products.CategoryListScreen
import com.alphadragon.pos.ui.products.ProductDetailScreen
import com.alphadragon.pos.ui.products.ProductListScreen
import com.alphadragon.pos.ui.reports.ReportsScreen
import com.alphadragon.pos.ui.sale.SaleScreen
import com.alphadragon.pos.ui.settings.SettingsScreen
import com.alphadragon.pos.ui.setup.SetupScreen
import com.alphadragon.pos.ui.transactions.TransactionDetailScreen
import com.alphadragon.pos.ui.transactions.TransactionHistoryScreen
import com.alphadragon.pos.ui.home.HomeScreen
import com.alphadragon.pos.ui.home.HomeViewModel

@Composable
fun AlphaDragonNavHost() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val startDestination by homeViewModel.startDestination.collectAsState()
    val ready by homeViewModel.startDestinationReady.collectAsState()

    if (!ready) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToSale = { navController.navigate(NavRoutes.SALE) },
                onNavigateToProducts = { navController.navigate(NavRoutes.PRODUCTS) },
                onNavigateToTransactions = { navController.navigate(NavRoutes.TRANSACTIONS) },
                onNavigateToReports = { navController.navigate(NavRoutes.REPORTS) },
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onSessionExpired = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SALE) {
            SaleScreen(onBack = { navController.popBackStack() })
        }

        // ── Products ──────────────────────────────────────────────────────
        composable(NavRoutes.PRODUCTS) {
            ProductListScreen(
                onAddProduct = { navController.navigate(NavRoutes.PRODUCT_ADD) },
                onProductClick = { id -> navController.navigate(NavRoutes.productDetail(id)) },
                onManageCategories = { navController.navigate(NavRoutes.CATEGORIES) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            ProductDetailScreen(
                productId = backStackEntry.arguments?.getString("productId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PRODUCT_ADD) {
            ProductDetailScreen(productId = null, onBack = { navController.popBackStack() })
        }

        // ── Categories ────────────────────────────────────────────────────
        composable(NavRoutes.CATEGORIES) {
            CategoryListScreen(
                onAddCategory = { navController.navigate(NavRoutes.CATEGORY_ADD) },
                onCategoryClick = { id -> navController.navigate(NavRoutes.categoryDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.CATEGORY_ADD) {
            CategoryDetailScreen(categoryId = null, onBack = { navController.popBackStack() })
        }

        composable(
            route = NavRoutes.CATEGORY_DETAIL,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            CategoryDetailScreen(
                categoryId = backStackEntry.arguments?.getString("categoryId"),
                onBack = { navController.popBackStack() }
            )
        }

        // ── Transactions ──────────────────────────────────────────────────
        composable(NavRoutes.TRANSACTIONS) {
            TransactionHistoryScreen(
                onTransactionClick = { id -> navController.navigate(NavRoutes.transactionDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            TransactionDetailScreen(
                transactionId = backStackEntry.arguments?.getString("transactionId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
