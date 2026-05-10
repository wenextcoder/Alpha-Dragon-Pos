package com.alphadragon.pos.ui.navigation

object NavRoutes {
    const val SETUP = "setup"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SALE = "sale"
    const val PRODUCTS = "products"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val PRODUCT_ADD = "product_add"
    const val CATEGORIES = "categories"
    const val CATEGORY_DETAIL = "category_detail/{categoryId}"
    const val CATEGORY_ADD = "category_add"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    fun productDetail(productId: String) = "product_detail/$productId"
    fun categoryDetail(categoryId: String) = "category_detail/$categoryId"
    fun transactionDetail(transactionId: String) = "transaction_detail/$transactionId"
}
