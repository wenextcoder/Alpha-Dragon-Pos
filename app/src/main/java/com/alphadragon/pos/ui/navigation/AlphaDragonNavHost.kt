package com.alphadragon.pos.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alphadragon.pos.ui.auth.LoginScreen
import com.alphadragon.pos.ui.common.ComingSoonScreen
import com.alphadragon.pos.ui.customers.CustomersScreen
import com.alphadragon.pos.ui.home.HomeScreen
import com.alphadragon.pos.ui.home.HomeViewModel
import com.alphadragon.pos.ui.products.CategoryDetailScreen
import com.alphadragon.pos.ui.products.CategoryListScreen
import com.alphadragon.pos.ui.products.ProductDetailScreen
import com.alphadragon.pos.ui.products.ProductListScreen
import com.alphadragon.pos.ui.reports.ReportsScreen
import com.alphadragon.pos.ui.sale.SaleScreen
import com.alphadragon.pos.ui.scan.ScanBarcodeResult
import com.alphadragon.pos.ui.scan.ScanViewModel
import com.alphadragon.pos.ui.scanner.BarcodeScannerDialog
import com.alphadragon.pos.ui.settings.SettingsScreen
import com.alphadragon.pos.ui.shop.ShopDetailsScreen
import com.alphadragon.pos.ui.setup.SetupScreen
import com.alphadragon.pos.ui.transactions.TransactionDetailScreen
import com.alphadragon.pos.ui.transactions.TransactionHistoryScreen
import com.alphadragon.pos.ui.theme.BrandRedDark
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphaDragonNavHost() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val scanViewModel: ScanViewModel = hiltViewModel()
    val startDestination by homeViewModel.startDestination.collectAsState()
    val ready by homeViewModel.startDestinationReady.collectAsState()
    val scope = rememberCoroutineScope()
    var showScanDialog by remember { mutableStateOf(false) }

    if (!ready) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = isMainTabRoute(currentRoute)

    if (showScanDialog) {
        BarcodeScannerDialog(
            title = "Scan product",
            helperText = "Align the barcode in the frame. If it exists, we open Sale with it selected. Otherwise, Add Product opens with the code filled in.",
            onBarcodeScanned = { code ->
                showScanDialog = false
                scope.launch {
                    when (val r = scanViewModel.resolveBarcode(code)) {
                        is ScanBarcodeResult.Found ->
                            navController.navigate(NavRoutes.saleWithProduct(r.productId)) {
                                popUpTo(NavRoutes.HOME)
                                launchSingleTop = true
                            }
                        is ScanBarcodeResult.NotFound ->
                            if (r.barcode.isNotBlank()) {
                                navController.navigate(NavRoutes.productAddWithBarcode(r.barcode)) {
                                    launchSingleTop = true
                                }
                            }
                    }
                }
            },
            onDismiss = { showScanDialog = false }
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MainBottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(NavRoutes.HOME)
                                launchSingleTop = true
                            }
                        }
                    },
                    onScanClick = { showScanDialog = true }
                )
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { mainPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = if (showBottomBar) Modifier.padding(mainPadding) else Modifier
        ) {

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
                    onNavigateToAddProduct = { navController.navigate(NavRoutes.PRODUCT_ADD_ENTRY) },
                    onNavigateToShop = { navController.navigate(NavRoutes.SHOP_DETAILS) },
                    onNavigateToCategories = { navController.navigate(NavRoutes.CATEGORIES) },
                    onNavigateToCustomers = { navController.navigate(NavRoutes.CUSTOMERS) },
                    onNavigateToImport = { navController.navigate(NavRoutes.IMPORT_COMING_SOON) },
                    onNavigateToExport = { navController.navigate(NavRoutes.EXPORT_COMING_SOON) },
                    onNavigateToMerchant = { navController.navigate(NavRoutes.MERCHANT_COMING_SOON) },
                    onNavigateToTransactions = { navController.navigate(NavRoutes.TRANSACTIONS) },
                    onNavigateToReports = { navController.navigate(NavRoutes.REPORTS) },
                    onSessionExpired = {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = NavRoutes.SALE,
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId").orEmpty()
                    .takeIf { it.isNotBlank() }
                SaleScreen(initialProductId = productId)
            }

            composable(NavRoutes.PRODUCTS) {
                ProductListScreen(
                    onAddProduct = { navController.navigate(NavRoutes.PRODUCT_ADD_ENTRY) },
                    onProductClick = { id -> navController.navigate(NavRoutes.productDetail(id)) },
                    onManageCategories = { navController.navigate(NavRoutes.CATEGORIES) }
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

            composable(
                route = NavRoutes.PRODUCT_ADD,
                arguments = listOf(
                    navArgument("barcode") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                ProductDetailScreen(productId = null, onBack = { navController.popBackStack() })
            }

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

            composable(NavRoutes.CUSTOMERS) {
                CustomersScreen(onBack = { navController.popBackStack() })
            }

            composable(NavRoutes.SHOP_DETAILS) {
                ShopDetailsScreen(onBack = { navController.popBackStack() })
            }

            composable(NavRoutes.IMPORT_COMING_SOON) {
                ComingSoonScreen(title = "Import", onBack = { navController.popBackStack() })
            }

            composable(NavRoutes.EXPORT_COMING_SOON) {
                ComingSoonScreen(title = "Export", onBack = { navController.popBackStack() })
            }

            composable(NavRoutes.MERCHANT_COMING_SOON) {
                ComingSoonScreen(title = "Merchant", onBack = { navController.popBackStack() })
            }

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
                SettingsScreen(
                    onShopProfile = { navController.navigate(NavRoutes.SHOP_DETAILS) }
                )
            }
        }
    }
}

private fun isMainTabRoute(route: String?): Boolean = when (route) {
    NavRoutes.HOME, NavRoutes.PRODUCTS, NavRoutes.SETTINGS -> true
    else -> route != null && (route == NavRoutes.SALE || route.startsWith("sale?"))
}

@Composable
private fun bottomNavItemColors(): NavigationBarItemColors =
    NavigationBarItemDefaults.colors(
        selectedIconColor = BrandRedDark,
        selectedTextColor = BrandRedDark,
        indicatorColor = BrandRedDark.copy(alpha = 0.22f),
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

@Composable
private fun MainBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onScanClick: () -> Unit
) {
    val saleSelected = currentRoute == NavRoutes.SALE || currentRoute?.startsWith("sale?") == true
    val itemColors = bottomNavItemColors()
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == NavRoutes.HOME,
            onClick = { onNavigate(NavRoutes.HOME) },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
            label = { Text("Sale") },
            selected = saleSelected,
            onClick = { onNavigate(NavRoutes.SALE) },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") },
            label = { Text("Scan") },
            selected = false,
            onClick = onScanClick,
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
            label = { Text("Products") },
            selected = currentRoute == NavRoutes.PRODUCTS,
            onClick = { onNavigate(NavRoutes.PRODUCTS) },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = currentRoute == NavRoutes.SETTINGS,
            onClick = { onNavigate(NavRoutes.SETTINGS) },
            colors = itemColors
        )
    }
}
