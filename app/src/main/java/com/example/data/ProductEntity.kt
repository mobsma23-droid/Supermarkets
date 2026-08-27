package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val catalogType: String, // "DREAMPRICE" or "INTERMART"
    val name: String,
    val category: String,
    val brand: String,
    val unit: String,
    val price: Double,
    val cost: Double,
    val createdAt: Long = System.currentTimeMillis()
)
