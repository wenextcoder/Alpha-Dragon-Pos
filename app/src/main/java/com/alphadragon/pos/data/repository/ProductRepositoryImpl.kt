package com.alphadragon.pos.data.repository

import com.alphadragon.pos.data.local.dao.CategoryDao
import com.alphadragon.pos.data.local.dao.ProductDao
import com.alphadragon.pos.data.local.entity.CategoryEntity
import com.alphadragon.pos.data.local.entity.ProductEntity
import com.alphadragon.pos.domain.model.Category
import com.alphadragon.pos.domain.model.Product
import com.alphadragon.pos.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao
) : ProductRepository {

    override fun observeActiveProducts(): Flow<List<Product>> =
        productDao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeProductsByCategory(categoryId: String): Flow<List<Product>> =
        productDao.observeByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun searchProducts(query: String): Flow<List<Product>> =
        productDao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getProductByBarcode(barcode: String): Product? =
        withContext(Dispatchers.IO) { productDao.getByBarcode(barcode)?.toDomain() }

    override suspend fun getProductById(id: String): Product? =
        withContext(Dispatchers.IO) { productDao.getById(id)?.toDomain() }

    override fun observeLowStockProducts(): Flow<List<Product>> =
        productDao.observeLowStock().map { list -> list.map { it.toDomain() } }

    override suspend fun saveProduct(product: Product): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { productDao.insert(product.toEntity()) } }

    override suspend fun saveProducts(products: List<Product>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { productDao.insertAll(products.map { it.toEntity() }) }
        }

    override suspend fun updateProduct(product: Product): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { productDao.update(product.toEntity()) } }

    override suspend fun deactivateProduct(id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { productDao.deactivate(id) } }

    override suspend fun decrementStock(productId: String, qty: Int): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { productDao.decrementStock(productId, qty) } }

    override fun observeTopLevelCategories(): Flow<List<Category>> =
        categoryDao.observeTopLevel().map { list -> list.map { it.toDomain() } }

    override fun observeSubcategories(parentId: String): Flow<List<Category>> =
        categoryDao.observeSubcategories(parentId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveCategory(category: Category): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { categoryDao.insert(category.toEntity()) } }

    override suspend fun updateCategory(category: Category): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { categoryDao.update(category.toEntity()) } }

    override suspend fun deactivateCategory(id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { categoryDao.deactivate(id) } }

    // Mapping helpers
    private fun ProductEntity.toDomain() = Product(
        id = id, name = name, description = description, sku = sku, barcode = barcode,
        categoryId = categoryId, price = price, taxRate = taxRate,
        imagePath = imagePath, trackStock = trackStock, stockQty = stockQty,
        lowStockAlert = lowStockAlert, isActive = isActive,
        sortOrder = sortOrder, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun Product.toEntity() = ProductEntity(
        id = id, name = name, description = description, sku = sku, barcode = barcode,
        categoryId = categoryId, price = price, taxRate = taxRate,
        imagePath = imagePath, trackStock = trackStock, stockQty = stockQty,
        lowStockAlert = lowStockAlert, isActive = isActive,
        sortOrder = sortOrder, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun CategoryEntity.toDomain() = Category(
        id = id, name = name, parentId = parentId, taxRate = taxRate,
        displayOrder = displayOrder, isActive = isActive
    )

    private fun Category.toEntity() = CategoryEntity(
        id = id, name = name, parentId = parentId, taxRate = taxRate,
        displayOrder = displayOrder, isActive = isActive
    )
}
