package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

object FirestoreCartService {
    private const val TAG = "FirestoreCartService"

    private fun getDb(): FirebaseFirestore? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
            } catch (ignored: Exception) {
                // Settings can only be set once before any other operations
            }
            firestore
        } catch (e: Exception) {
            Log.w(TAG, "Firestore is unavailable: ${e.message}")
            null
        }
    }

    /**
     * Syncs entire cart to Firestore under collection `carts/{userKey}`
     */
    suspend fun syncCartToFirestore(userKey: String, items: List<CartItemEntity>): Boolean {
        return try {
            val db = getDb() ?: return false
            val sanitizedUser = if (userKey.isBlank()) "guest_user" else userKey.replace(".", "_").replace("@", "_")
            val cartDocRef = db.collection("carts").document(sanitizedUser)

            val cartData = hashMapOf(
                "userEmail" to userKey,
                "updatedAt" to Date(),
                "totalItems" to items.sumOf { it.quantity },
                "totalAmount" to items.sumOf { it.unitPrice * it.quantity },
                "items" to items.map { item ->
                    hashMapOf(
                        "id" to item.id,
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "catalogType" to item.catalogType,
                        "category" to item.category,
                        "unit" to item.unit,
                        "unitPrice" to item.unitPrice,
                        "unitCost" to item.unitCost,
                        "quantity" to item.quantity,
                        "totalItemPrice" to (item.unitPrice * item.quantity)
                    )
                }
            )

            cartDocRef.set(cartData, SetOptions.merge()).await()
            Log.d(TAG, "Cart successfully synced to Firestore for user: $userKey (${items.size} items)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing cart to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun syncProductsToFirestore(products: List<com.example.data.ProductEntity>): Boolean {
        return try {
            val db = getDb() ?: return false
            val batch = db.batch()
            products.forEach { product ->
                val docRef = db.collection("products").document(product.id.toString())
                val productData = hashMapOf(
                    "id" to product.id,
                    "catalogType" to product.catalogType,
                    "name" to product.name,
                    "category" to product.category,
                    "brand" to product.brand,
                    "unit" to product.unit,
                    "price" to product.price,
                    "cost" to product.cost
                )
                batch.set(docRef, productData, SetOptions.merge())
            }
            batch.commit().await()
            Log.d(TAG, "Products successfully synced to Firestore (${products.size} items)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing products to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Records a checkout order in Firestore under `orders`
     */
    suspend fun saveOrderToFirestore(
        userKey: String,
        items: List<CartItemEntity>,
        totalPrice: Double,
        totalProfit: Double
    ): String? {
        return try {
            val db = getDb() ?: return null
            val sanitizedUser = if (userKey.isBlank()) "guest_user" else userKey
            val orderRef = db.collection("orders").document()

            val orderData = hashMapOf(
                "orderId" to orderRef.id,
                "userEmail" to sanitizedUser,
                "createdAt" to Date(),
                "status" to "CONFIRMED",
                "totalAmount" to totalPrice,
                "totalProfit" to totalProfit,
                "itemCount" to items.sumOf { it.quantity },
                "items" to items.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "catalogType" to item.catalogType,
                        "category" to item.category,
                        "unit" to item.unit,
                        "unitPrice" to item.unitPrice,
                        "unitCost" to item.unitCost,
                        "quantity" to item.quantity
                    )
                }
            )

            orderRef.set(orderData).await()

            // Also clear cart in Firestore
            val sanitizedUserKey = if (userKey.isBlank()) "guest_user" else userKey.replace(".", "_").replace("@", "_")
            db.collection("carts").document(sanitizedUserKey).delete().await()

            Log.d(TAG, "Order ${orderRef.id} saved to Firestore")
            orderRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error saving order to Firestore: ${e.message}", e)
            null
        }
    }
}

