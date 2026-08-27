package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.CatalogLayoutDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CloudSyncStatusCard
import com.example.ui.components.VoiceSemanticSearchModal
import com.example.ui.components.WishlistDialog
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor

@Composable
fun ProductsScreen(viewModel: CatalogViewModel) {
    val context = LocalContext.current
    val activeCatalog by viewModel.activeCatalog.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    val currentProducts by viewModel.currentProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val availableCatalogs by viewModel.availableCatalogs.collectAsState()
    val comparedIds by viewModel.comparedProductIds.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val wishlistedIds by viewModel.wishlistedProductIds.collectAsState()
    val layoutDensity by viewModel.layoutDensity.collectAsState()
    val semanticResult by viewModel.semanticSearchResult.collectAsState()

    val categories = remember(allProducts) {
        val cats = allProducts
            .map { it.category }
            .distinct()
            .sorted()
        listOf("Tous") + cats
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceSearchModal by remember { mutableStateOf(false) }
    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var showWishlistModal by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Title
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
                Text(
                    text = "Produits",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Comparez les prix entre supermarchés",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Search and Filter Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_bar"),
                    placeholder = { Text("Rechercher un produit ou une catégorie...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearAllSearchFilters() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Effacer",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showWishlistModal = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("wishlist_header_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (wishlistItems.isNotEmpty()) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Liste d'envies",
                                        tint = if (wishlistItems.isNotEmpty()) Color(0xFFE11D48) else BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (wishlistItems.isNotEmpty()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFE11D48),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${wishlistItems.size}",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(
                                onClick = { showBarcodeScannerModal = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("barcode_scanner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scanner code-barres",
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { showVoiceSearchModal = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("mic_search_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (semanticResult != null) BluePrimary.copy(alpha = 0.15f) else Color.Transparent,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Recherche vocale & IA Gemini",
                                            tint = if (semanticResult != null) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Active AI Semantic Search Banner
                AnimatedVisibility(visible = semanticResult != null) {
                    if (semanticResult != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BluePrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Recherche IA : \"${semanticResult!!.query}\"",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                        Text(
                                            text = semanticResult!!.explanation,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.clearSemanticSearch() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Annuler la recherche sémantique",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            onClick = { viewModel.setSelectedCategory(category) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Products List
            if (currentProducts.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucun produit pour le moment",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Importez un catalogue CSV ou ajoutez des produits manuellement pour commencer.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (layoutDensity.isGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentProducts, key = { it.id }) { product ->
                        val isInCompare = product.id in comparedIds
                        val isWishlisted = product.id in wishlistedIds
                        ProductItemCard(
                            product = product,
                            allProducts = allProducts,
                            userRole = userRole,
                            isInCompare = isInCompare,
                            isWishlisted = isWishlisted,
                            onToggleWishlist = {
                                viewModel.toggleWishlist(product)
                                val msg = if (isWishlisted)
                                    "${product.name} retiré de la liste d'envies"
                                else
                                    "${product.name} sauvegardé dans la liste d'envies"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onToggleCompare = {
                                viewModel.toggleCompareProduct(product)
                                val msg = if (isInCompare)
                                    "${product.name} retiré du comparateur"
                                else
                                    "${product.name} ajouté au comparateur"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onAddToCart = {
                                viewModel.addToCart(product)
                                Toast.makeText(context, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = { editingProduct = product },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (layoutDensity == CatalogLayoutDensity.COMPACT) 6.dp else 12.dp)
                ) {
                    items(currentProducts, key = { it.id }) { product ->
                        val isInCompare = product.id in comparedIds
                        val isWishlisted = product.id in wishlistedIds
                        ProductItemCard(
                            product = product,
                            allProducts = allProducts,
                            userRole = userRole,
                            isInCompare = isInCompare,
                            isWishlisted = isWishlisted,
                            onToggleWishlist = {
                                viewModel.toggleWishlist(product)
                                val msg = if (isWishlisted)
                                    "${product.name} retiré de la liste d'envies"
                                else
                                    "${product.name} sauvegardé dans la liste d'envies"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onToggleCompare = {
                                viewModel.toggleCompareProduct(product)
                                val msg = if (isInCompare)
                                    "${product.name} retiré du comparateur"
                                else
                                    "${product.name} ajouté au comparateur"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onAddToCart = {
                                viewModel.addToCart(product)
                                Toast.makeText(context, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = { editingProduct = product },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button for Admin
        if (userRole == "ADMIN") {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter Produit")
            }
        }

        // Add Product Dialog
        if (showAddDialog) {
            AddProductDialog(
                activeCatalog = activeCatalog,
                onDismiss = { showAddDialog = false },
                onAdd = { name, cat, brand, unit, price, cost ->
                    viewModel.addProduct(name, cat, brand, unit, price, cost, activeCatalog)
                    showAddDialog = false
                    Toast.makeText(context, "Produit ajouté à $activeCatalog", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Product Dialog
        editingProduct?.let { prod ->
            EditProductDialog(
                product = prod,
                onDismiss = { editingProduct = null },
                onSave = { updated ->
                    viewModel.updateProduct(updated)
                    editingProduct = null
                    Toast.makeText(context, "Produit modifié avec succès", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Voice & Semantic AI Search Modal
        if (showVoiceSearchModal) {
            VoiceSemanticSearchModal(
                viewModel = viewModel,
                onDismiss = { showVoiceSearchModal = false }
            )
        }

        // Barcode Camera Scanner Dialog
        if (showBarcodeScannerModal) {
            BarcodeScannerDialog(
                viewModel = viewModel,
                onDismiss = { showBarcodeScannerModal = false }
            )
        }

        // Wishlist Saved Products Dialog
        if (showWishlistModal) {
            WishlistDialog(
                viewModel = viewModel,
                onDismiss = { showWishlistModal = false }
            )
        }
    }
}

@Composable
private fun ProductItemCard(
    product: ProductEntity,
    allProducts: List<ProductEntity>,
    userRole: String,
    isInCompare: Boolean,
    isWishlisted: Boolean,
    onToggleWishlist: () -> Unit,
    onToggleCompare: () -> Unit,
    onAddToCart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val margin = product.price - product.cost
    val marginPct = if (product.price > 0) (margin / product.price) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "#${product.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Supermarkets badges & respective prices under each item
                    val normName = product.name.trim().lowercase()
                    val productsWithSameName = allProducts.filter { it.name.trim().lowercase() == normName }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        productsWithSameName.forEach { p ->
                            if (p.price > 0) {
                                val color = when (p.catalogType.uppercase()) {
                                    "DREAMPRICE" -> DreampriceColor
                                    "INTERMART" -> IntermartColor
                                    "SUPER U" -> Color(0xFFE53935)
                                    "JUMBO" -> Color(0xFFFF9800)
                                    "WINNERS" -> Color(0xFF4CAF50)
                                    else -> BluePrimary
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = color.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = color
                                        ) {
                                            Text(
                                                text = p.catalogType.take(1).uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "${p.catalogType}: Rs ${String.format("%.2f", p.price)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color,
                                            softWrap = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (userRole == "ADMIN") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Rs ${String.format("%.2f", product.price)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )
                        Text(
                            text = " / ${product.unit}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleWishlist,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("wishlist_toggle_${product.id}")
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isWishlisted) "Retirer de la liste d'envies" else "Sauvegarder",
                            tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleCompare,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isInCompare) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = if (isInCompare) BluePrimary.copy(alpha = 0.12f) else Color.Transparent
                        ),
                        border = BorderStroke(1.dp, if (isInCompare) BluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isInCompare) Icons.Default.Check else Icons.Default.CompareArrows,
                            contentDescription = "Compare",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isInCompare) "Comparé" else "Comparer",
                            fontSize = 12.sp,
                            fontWeight = if (isInCompare) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onAddToCart,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ajouter", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    activeCatalog: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, cat: String, brand: String, unit: String, price: Double, cost: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentaire") }
    var brand by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("PCS") }
    var priceText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Produit ($activeCatalog)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du Produit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marque") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix Vente (Rs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Prix Achat / Coût (Rs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                    val cost = costText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onAdd(name, category, brand, unit, price, cost)
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun EditProductDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var brand by remember { mutableStateOf(product.brand) }
    var unit by remember { mutableStateOf(product.unit) }
    var priceText by remember { mutableStateOf(if (product.price > 0) product.price.toString() else "") }
    var costText by remember { mutableStateOf(if (product.cost > 0) product.cost.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier Produit #${product.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du Produit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marque") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix Vente (Rs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Prix Achat / Coût (Rs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceText.replace(',', '.').trim().toDoubleOrNull() ?: product.price
                    val parsedCost = costText.replace(',', '.').trim().toDoubleOrNull() ?: product.cost
                    if (name.isNotBlank()) {
                        onSave(
                            product.copy(
                                name = name.trim(),
                                category = category.trim(),
                                brand = brand.trim(),
                                unit = unit.trim(),
                                price = parsedPrice,
                                cost = parsedCost
                            )
                        )
                    }
                }
            ) {
                Text("Enregistrer les modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
