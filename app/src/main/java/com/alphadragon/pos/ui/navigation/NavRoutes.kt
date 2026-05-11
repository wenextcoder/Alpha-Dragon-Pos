package com.alphadragon.pos.ui.navigation

import android.net.Uri

object NavRoutes {
    const val SETUP = "setup"
    const val LOGIN = "login"
    const val HOME = "home"
    /** Sale with optional product id to add/select after scan. */
    const val SALE = "sale?productId={productId}"
    const val PRODUCTS = "products"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    /** NavHost route pattern only. For navigation use [PRODUCT_ADD_ENTRY] or [productAddWithBarcode]; never pass this constant to NavController.navigate. */
    const val PRODUCT_ADD = "product_add?barcode={barcode}"

    /** Safe target when opening Add Product with no preset barcode (avoids literal "{barcode}" in the field). */
    const val PRODUCT_ADD_ENTRY = "product_add"
    const val CATEGORIES = "categories"
    const val CATEGORY_DETAIL = "category_detail/{categoryId}"
    const val CATEGORY_ADD = "category_add"
    const val CUSTOMERS = "customers"
    const val SHOP_DETAILS = "shop_details"
    const val IMPORT_COMING_SOON = "import_coming_soon"
    const val EXPORT_COMING_SOON = "export_coming_soon"
    const val MERCHANT_COMING_SOON = "merchant_coming_soon"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    fun productDetail(productId: String) = "product_detail/$productId"

    fun saleWithProduct(productId: String) = "sale?productId=${Uri.encode(productId)}"

    fun productAddWithBarcode(barcode: String) = "product_add?barcode=${Uri.encode(barcode)}"
    fun categoryDetail(categoryId: String) = "category_detail/$categoryId"
    fun transactionDetail(transactionId: String) = "transaction_detail/$transactionId"
}
