package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    // --- Products ---
    @Query("SELECT * FROM products WHERE catalogType = :catalogType ORDER BY id DESC")
    fun getProductsByCatalog(catalogType: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Query("DELETE FROM products WHERE catalogType = :catalogType")
    suspend fun clearCatalog(catalogType: String)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    // --- Cart ---
    @Query("SELECT * FROM cart_items ORDER BY id ASC")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId AND catalogType = :catalogType LIMIT 1")
    suspend fun getCartItemByProduct(productId: Int, catalogType: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)

    @Update
    suspend fun updateCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // --- Sales & Profits ---
    @Query("SELECT * FROM sale_records ORDER BY timestamp DESC")
    fun getSaleRecords(): Flow<List<SaleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleRecords(sales: List<SaleRecordEntity>)

    @Query("DELETE FROM sale_records")
    suspend fun clearSaleRecords()

    // --- Wishlist ---
    @Query("SELECT * FROM wishlist_items ORDER BY addedAt DESC")
    fun getWishlistItems(): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist_items WHERE productId = :productId AND catalogType = :catalogType LIMIT 1")
    suspend fun getWishlistItemByProduct(productId: Int, catalogType: String): WishlistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItemEntity): Long

    @Query("DELETE FROM wishlist_items WHERE id = :id")
    suspend fun deleteWishlistItem(id: Int)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId AND catalogType = :catalogType")
    suspend fun deleteWishlistByProductId(productId: Int, catalogType: String)

    @Query("DELETE FROM wishlist_items")
    suspend fun clearWishlist()

    // --- Price History (Room Local Storage) ---
    @Query("SELECT * FROM price_history WHERE productId = :productId OR LOWER(TRIM(productName)) = LOWER(TRIM(:productName)) ORDER BY timestamp ASC")
    fun getPriceHistoryForProduct(productId: Int, productName: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId OR LOWER(TRIM(productName)) = LOWER(TRIM(:productName)) ORDER BY timestamp ASC")
    suspend fun getPriceHistoryList(productId: Int, productName: String): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(records: List<PriceHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistoryRecord(record: PriceHistoryEntity): Long

    @Query("DELETE FROM price_history WHERE productId = :productId")
    suspend fun deletePriceHistoryForProduct(productId: Int)

    @Query("DELETE FROM price_history")
    suspend fun clearPriceHistory()
}
