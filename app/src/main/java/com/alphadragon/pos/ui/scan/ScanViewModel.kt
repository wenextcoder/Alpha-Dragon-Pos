package com.alphadragon.pos.ui.scan

import androidx.lifecycle.ViewModel
import com.alphadragon.pos.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class ScanBarcodeResult {
    data class Found(val productId: String) : ScanBarcodeResult()
    data class NotFound(val barcode: String) : ScanBarcodeResult()
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    suspend fun resolveBarcode(barcode: String): ScanBarcodeResult {
        val normalized = barcode.trim()
        if (normalized.isBlank()) return ScanBarcodeResult.NotFound("")
        val product = productRepository.getProductByBarcode(normalized)
        return if (product != null) {
            ScanBarcodeResult.Found(product.id)
        } else {
            ScanBarcodeResult.NotFound(normalized)
        }
    }
}
