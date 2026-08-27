package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.SlateTextSecondary

@Composable
fun CompareScreen(
    viewModel: CatalogViewModel,
    onNavigateToProducts: () -> Unit
) {
    val context = LocalContext.current
    val comparedProducts by viewModel.comparedProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Comparateur de Prix",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Liste personnalisée de comparaison",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (comparedProducts.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.clearCompareList()
                            Toast.makeText(context, "Liste de comparaison vidée", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Vider",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Vider",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (comparedProducts.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BluePrimary.copy(alpha = 0.1f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = BluePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Liste de comparaison vide",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sélectionnez des articles dans l'onglet 'Produits' en cliquant sur le bouton 'Comparer' pour mesurer les prix entre supermarchés.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onNavigateToProducts,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(text = "Parcourir les produits", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        } else {
            // Compare Content List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Total Basket Comparison Summary Card
                item {
                    GlobalBasketComparisonCard(
                        comparedProducts = comparedProducts,
                        allProducts = allProducts
                    )
                }

                // Section Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Articles Sélectionnés (${comparedProducts.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(onClick = onNavigateToProducts) {
                            Text("+ Ajouter d'autres", fontSize = 12.sp, color = BluePrimary)
                        }
                    }
                }

                // Individual Comparison Items
                items(comparedProducts, key = { it.id }) { product ->
                    ComparedProductCard(
                        product = product,
                        allProducts = allProducts,
                        onAddToCart = {
                            viewModel.addToCart(product)
                            Toast.makeText(context, "${product.name} copié au panier (conservé dans le comparateur)", Toast.LENGTH_SHORT).show()
                        },
                        onRemove = {
                            viewModel.removeFromCompare(product.id)
                            Toast.makeText(context, "${product.name} retiré", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalBasketComparisonCard(
    comparedProducts: List<ProductEntity>,
    allProducts: List<ProductEntity>
) {
    var totalDream = 0.0
    var totalInter = 0.0
    var dreamMatchCount = 0
    var interMatchCount = 0

    comparedProducts.forEach { p ->
        val normName = p.name.trim().lowercase()
        val dream = allProducts.find { it.name.trim().lowercase() == normName && it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
        val inter = allProducts.find { it.name.trim().lowercase() == normName && it.catalogType.equals("INTERMART", ignoreCase = true) }

        val pDream = dream?.price ?: if (p.catalogType.equals("DREAMPRICE", ignoreCase = true)) p.price else null
        val pInter = inter?.price ?: if (p.catalogType.equals("INTERMART", ignoreCase = true)) p.price else null

        if (pDream != null) {
            totalDream += pDream
            dreamMatchCount++
        }
        if (pInter != null) {
            totalInter += pInter
            interMatchCount++
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bilan Comparatif de votre Sélection",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dreamprice Basket Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = DreampriceColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, DreampriceColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Dreamprice",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DreampriceColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rs ${String.format("%.2f", totalDream)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DreampriceColor
                        )
                        Text(
                            text = "$dreamMatchCount / ${comparedProducts.size} articles",
                            fontSize = 10.sp,
                            color = SlateTextSecondary
                        )
                    }
                }

                // Intermart Basket Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = IntermartColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, IntermartColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Intermart",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IntermartColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rs ${String.format("%.2f", totalInter)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IntermartColor
                        )
                        Text(
                            text = "$interMatchCount / ${comparedProducts.size} articles",
                            fontSize = 10.sp,
                            color = SlateTextSecondary
                        )
                    }
                }
            }

            if (totalDream > 0 && totalInter > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                val diff = kotlin.math.abs(totalDream - totalInter)
                val cheaperStore = if (totalDream < totalInter) "Dreamprice" else if (totalInter < totalDream) "Intermart" else "Équivalent"
                val cheaperColor = if (totalDream < totalInter) DreampriceColor else if (totalInter < totalDream) IntermartColor else EmeraldSuccess

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = cheaperColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = cheaperColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (cheaperStore == "Équivalent") {
                            Text(
                                text = "Les deux enseignes proposent le même total global.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cheaperColor
                            )
                        } else {
                            Text(
                                text = "$cheaperStore est moins cher de Rs ${String.format("%.2f", diff)} sur votre liste !",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = cheaperColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparedProductCard(
    product: ProductEntity,
    allProducts: List<ProductEntity>,
    onAddToCart: () -> Unit,
    onRemove: () -> Unit
) {
    val normName = product.name.trim().lowercase()
    val dreamItem = allProducts.find { it.name.trim().lowercase() == normName && it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
    val interItem = allProducts.find { it.name.trim().lowercase() == normName && it.catalogType.equals("INTERMART", ignoreCase = true) }

    val dreamPrice = dreamItem?.price ?: if (product.catalogType.equals("DREAMPRICE", ignoreCase = true)) product.price else null
    val interPrice = interItem?.price ?: if (product.catalogType.equals("INTERMART", ignoreCase = true)) product.price else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Category & Remove Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BluePrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = product.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (product.brand.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${product.brand}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Retirer",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Product Name
            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Comparative Stores Table
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dreamprice Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = DreampriceColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DreampriceColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = DreampriceColor) {
                                Text(
                                    text = "D",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dreamprice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DreampriceColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (dreamPrice != null) {
                            Text(
                                text = "Rs ${String.format("%.2f", dreamPrice)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DreampriceColor
                            )
                        } else {
                            Text("Non répertorié", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                    }
                }

                // Intermart Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = IntermartColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, IntermartColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = IntermartColor) {
                                Text(
                                    text = "I",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Intermart", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IntermartColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (interPrice != null) {
                            Text(
                                text = "Rs ${String.format("%.2f", interPrice)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntermartColor
                            )
                        } else {
                            Text("Non répertorié", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                    }
                }
            }

            // Price Verdict & Add to Cart Action
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dreamPrice != null && interPrice != null) {
                    val diff = kotlin.math.abs(dreamPrice - interPrice)
                    if (dreamPrice < interPrice) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DreampriceColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Moins cher à Dreamprice (-Rs ${String.format("%.2f", diff)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DreampriceColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (interPrice < dreamPrice) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IntermartColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Moins cher à Intermart (-Rs ${String.format("%.2f", diff)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntermartColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Prix identique",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = onAddToCart,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Ajouter au panier",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Au panier", fontSize = 12.sp)
                }
            }
        }
    }
}
