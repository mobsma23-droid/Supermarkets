package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.util.AppStrings
// import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
// import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
// import com.patrykandpatrick.vico.compose.chart.Chart
// import com.patrykandpatrick.vico.compose.chart.line.lineChart
// import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun ProductCatalogScreen(viewModel: CatalogViewModel) {
    val products by viewModel.currentProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text(AppStrings.searchPlaceholder(currentLang)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products) { product ->
                Card(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    onClick = { selectedProduct = product }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Prix: ${product.price} | Coût: ${product.cost}")
                    }
                }
            }
        }

        selectedProduct?.let { product ->
            Text(text = "Historique des prix: ${product.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "(Graphique indisponible)")
            
            // Vico Chart Implementation (Commented out until proper imports verified)
            // val chartEntryModel = entryModelOf(10f, 15f, 12f, 18f, 14f)
            // Chart(
            //     chart = lineChart(),
            //     model = chartEntryModel,
            //     startAxis = rememberStartAxis(),
            //     bottomAxis = rememberBottomAxis()
            // )
        }
    }
}
