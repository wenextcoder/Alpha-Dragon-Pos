package com.alphadragon.pos.domain.usecase.product

import com.alphadragon.pos.domain.model.Product
import com.alphadragon.pos.domain.repository.ProductRepository
import java.util.UUID
import javax.inject.Inject

class SaveProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(product: Product): Result<Unit> {
        if (product.name.isBlank())
            return Result.failure(IllegalArgumentException("Product name cannot be empty"))
        if (product.price < 0)
            return Result.failure(IllegalArgumentException("Price cannot be negative"))
        return productRepository.saveProduct(product)
    }
}

class UpdateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(product: Product): Result<Unit> {
        if (product.name.isBlank())
            return Result.failure(IllegalArgumentException("Product name cannot be empty"))
        if (product.price < 0)
            return Result.failure(IllegalArgumentException("Price cannot be negative"))
        val now = System.currentTimeMillis()
        return productRepository.updateProduct(product.copy(updatedAt = now))
    }
}

class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Result<Unit> =
        productRepository.deactivateProduct(productId)
}
