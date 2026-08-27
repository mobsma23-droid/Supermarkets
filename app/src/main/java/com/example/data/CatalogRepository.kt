package com.example.data

import kotlinx.coroutines.flow.Flow

class CatalogRepository(private val dao: CatalogDao) {

    fun getProductsByCatalog(catalogType: String): Flow<List<ProductEntity>> =
        dao.getProductsByCatalog(catalogType)

    fun getAllProducts(): Flow<List<ProductEntity>> =
        dao.getAllProducts()

    suspend fun importProducts(products: List<ProductEntity>) {
        dao.insertProducts(products)
        FirestoreCartService.syncProductsToFirestore(products)
    }

    suspend fun addProduct(product: ProductEntity): Long {
        return dao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        dao.updateProduct(product)
    }

    suspend fun deleteProduct(id: Int) {
        dao.deleteProductById(id)
    }

    suspend fun clearCatalog(catalogType: String) {
        dao.clearCatalog(catalogType)
    }

    suspend fun clearAllData() {
        dao.clearAllProducts()
        dao.clearCart()
        dao.clearSaleRecords()
        dao.clearWishlist()
        dao.clearPriceHistory()
    }

    // --- Cart ---
    fun getCartItems(): Flow<List<CartItemEntity>> = dao.getCartItems()

    suspend fun addToCart(product: ProductEntity, quantity: Int = 1) {
        val existing = dao.getCartItemByProduct(product.id, product.catalogType)
        if (existing != null) {
            val updated = existing.copy(quantity = existing.quantity + quantity)
            dao.updateCartItem(updated)
        } else {
            val newItem = CartItemEntity(
                catalogType = product.catalogType,
                productId = product.id,
                productName = product.name,
                category = product.category,
                unit = product.unit,
                unitPrice = product.price,
                unitCost = product.cost,
                quantity = quantity
            )
            dao.insertCartItem(newItem)
        }
    }

    suspend fun updateCartItemQuantity(item: CartItemEntity, newQuantity: Int) {
        if (newQuantity <= 0) {
            dao.deleteCartItem(item.id)
        } else {
            dao.updateCartItem(item.copy(quantity = newQuantity))
        }
    }

    suspend fun removeFromCart(cartItemId: Int) {
        dao.deleteCartItem(cartItemId)
    }

    suspend fun checkoutCart(cartItems: List<CartItemEntity>) {
        val sales = cartItems.map { item ->
            val total = item.unitPrice * item.quantity
            val cost = item.unitCost * item.quantity
            val profit = total - cost
            SaleRecordEntity(
                catalogType = item.catalogType,
                productName = item.productName,
                category = item.category,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                unitCost = item.unitCost,
                totalPrice = total,
                totalProfit = profit,
                timestamp = System.currentTimeMillis()
            )
        }
        dao.insertSaleRecords(sales)
        dao.clearCart()
    }

    // --- Sales ---
    fun getSaleRecords(): Flow<List<SaleRecordEntity>> = dao.getSaleRecords()

    // --- Wishlist ---
    fun getWishlistItems(): Flow<List<WishlistItemEntity>> = dao.getWishlistItems()

    suspend fun addToWishlist(product: ProductEntity) {
        val existing = dao.getWishlistItemByProduct(product.id, product.catalogType)
        if (existing == null) {
            val item = WishlistItemEntity(
                productId = product.id,
                catalogType = product.catalogType,
                productName = product.name,
                category = product.category,
                brand = product.brand,
                unit = product.unit,
                unitPrice = product.price,
                unitCost = product.cost
            )
            dao.insertWishlistItem(item)
        }
    }

    suspend fun removeFromWishlist(productId: Int, catalogType: String) {
        dao.deleteWishlistByProductId(productId, catalogType)
    }

    suspend fun removeWishlistItemById(id: Int) {
        dao.deleteWishlistItem(id)
    }

    suspend fun toggleWishlist(product: ProductEntity) {
        val existing = dao.getWishlistItemByProduct(product.id, product.catalogType)
        if (existing != null) {
            dao.deleteWishlistItem(existing.id)
        } else {
            val item = WishlistItemEntity(
                productId = product.id,
                catalogType = product.catalogType,
                productName = product.name,
                category = product.category,
                brand = product.brand,
                unit = product.unit,
                unitPrice = product.price,
                unitCost = product.cost
            )
            dao.insertWishlistItem(item)
        }
    }

    suspend fun clearWishlist() {
        dao.clearWishlist()
    }

    // --- Price History (Room Local Storage) ---
    fun getPriceHistory(productId: Int, productName: String): Flow<List<PriceHistoryEntity>> {
        return dao.getPriceHistoryForProduct(productId, productName)
    }

    suspend fun recordPricePoint(
        productId: Int,
        productName: String,
        catalogType: String,
        price: Double,
        cost: Double = 0.0,
        recordedDate: String = "Aujourd'hui"
    ) {
        val entity = PriceHistoryEntity(
            productId = productId,
            productName = productName,
            catalogType = catalogType,
            price = price,
            cost = cost,
            recordedDate = recordedDate,
            timestamp = System.currentTimeMillis()
        )
        dao.insertPriceHistoryRecord(entity)
    }

    suspend fun ensurePriceHistoryForProduct(product: ProductEntity) {
        val existing = dao.getPriceHistoryList(product.id, product.name)
        if (existing.isEmpty() && product.price > 0) {
            val basePrice = product.price
            val baseCost = product.cost
            val seedMonths = listOf(
                Pair("Mars 2026", basePrice * 0.92),
                Pair("Avril 2026", basePrice * 0.95),
                Pair("Mai 2026", basePrice * 0.98),
                Pair("Juin 2026", basePrice * 0.90),
                Pair("Juillet 2026", basePrice * 0.96),
                Pair("Août 2026 (Actuel)", basePrice)
            )

            val records = seedMonths.mapIndexed { index, (month, pr) ->
                val roundedPrice = (kotlin.math.round(pr * 100) / 100.0).coerceAtLeast(1.0)
                PriceHistoryEntity(
                    productId = product.id,
                    productName = product.name,
                    catalogType = product.catalogType,
                    price = roundedPrice,
                    cost = baseCost,
                    recordedDate = month,
                    timestamp = System.currentTimeMillis() - ((seedMonths.size - 1 - index) * 30L * 24L * 3600L * 1000L)
                )
            }
            dao.insertPriceHistory(records)
        }
    }
}
