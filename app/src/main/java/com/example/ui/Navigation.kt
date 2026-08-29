package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.LiquidNavigationBar
import com.example.ui.components.SmartShoppingListDialog
import com.example.ui.components.WishlistDialog
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.ConnexionScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.PersonalizeDialog
import com.example.ui.screens.PersonalizeScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ProductCatalogScreen
import com.example.ui.screens.ProfitsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SyncDashboardScreen
import com.example.ui.screens.GoogleBlue
import com.example.util.CatalogSyncManager
import com.example.util.AppLanguage
import com.example.util.AppStrings
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val icon: ImageVector) {
    abstract fun getTitle(lang: AppLanguage): String
    val title: String get() = getTitle(AppLanguage.FRENCH)

    object Products : Screen("products", Icons.Default.Search) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navProducts(lang)
    }
    object Compare : Screen("compare", Icons.Default.CompareArrows) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navCompare(lang)
    }
    object Cart : Screen("cart", Icons.Default.ShoppingCart) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navCart(lang)
    }
    object Profits : Screen("profits", Icons.Default.TrendingUp) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navProfits(lang)
    }
    object Personalize : Screen("personalize", Icons.Default.Palette) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navStyle(lang)
    }
    object Import : Screen("import", Icons.Default.CloudUpload) {
        override fun getTitle(lang: AppLanguage) = AppStrings.navImport(lang)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatalogManagerNavHost(viewModel: CatalogViewModel) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val savedEmail by viewModel.googleAccountEmail.collectAsState()
    val savedName by viewModel.googleAccountName.collectAsState()
    val isSignedIn by viewModel.isSignedInWithGoogle.collectAsState()

    val cartItems by viewModel.cartItems.collectAsState()
    val totalCartCount = cartItems.sumOf { it.quantity }

    val comparedProducts by viewModel.comparedProducts.collectAsState()
    val compareCount = comparedProducts.size

    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val wishlistCount = wishlistItems.size
    val syncState by CatalogSyncManager.syncState.collectAsState()

    var showAccountLoginModal by remember { mutableStateOf(false) }
    var showStyleModal by remember { mutableStateOf(false) }
    var showWishlistModal by remember { mutableStateOf(false) }
    var showSmartShoppingListModal by remember { mutableStateOf(false) }
    var showSyncDashboardModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }

    // Bottom Navigation Screens
    val bottomNavScreens = remember(userRole) {
        val list = mutableListOf(
            Screen.Products,
            Screen.Compare,
            Screen.Cart,
            Screen.Profits
        )
        if (userRole == "ADMIN") {
            list.add(Screen.Import)
        }
        list
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = bottomNavScreens.indexOf(Screen.Products).coerceAtLeast(0)
    ) {
        bottomNavScreens.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "QuicKart",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Smart Shopping List & Optimizer Button (Lowest Price Engine)
                    IconButton(
                        onClick = { showSmartShoppingListModal = true },
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(38.dp)
                            .testTag("smart_shopping_list_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Liste de Courses Intelligente (Meilleur Prix)",
                                tint = if (wishlistCount > 0) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            if (wishlistCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF059669),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$wishlistCount",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Background Sync Dashboard Button (WorkManager Live Status)
                    IconButton(
                        onClick = { showSyncDashboardModal = true },
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(38.dp)
                            .testTag("sync_dashboard_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Tableau de Synchronisation",
                                tint = if (syncState.isRunning) Color(0xFF2563EB) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            if (syncState.isRunning) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                ) {}
                            }
                        }
                    }

                    // Wishlist / Favoris Action Button with Badge
                    IconButton(
                        onClick = { showWishlistModal = true },
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (wishlistCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Ma Liste d'Envies",
                                tint = if (wishlistCount > 0) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            if (wishlistCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE11D48),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$wishlistCount",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Style & Theme Customization Modal Button
                    IconButton(
                        onClick = { showStyleModal = true },
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(38.dp)
                            .testTag("theme_customizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Style & Thème",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Dedicated Settings Screen Button (Room-to-Firestore sync toggle & DB settings)
                    IconButton(
                        onClick = { showSettingsModal = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(38.dp)
                            .testTag("settings_topbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres & Synchronisation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Account & Connexion Profile Button in Top Bar
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { showAccountLoginModal = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSignedIn && !savedEmail.isNullOrBlank()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = savedEmail!!.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (userRole == "ADMIN") "Admin" else "Compte",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Se connecter",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Connexion",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            LiquidNavigationBar(
                screens = bottomNavScreens,
                selectedIndex = pagerState.currentPage,
                badgeCounts = remember(totalCartCount, compareCount) {
                    mapOf(
                        Screen.Cart to totalCartCount,
                        Screen.Compare to compareCount
                    )
                },
                titleProvider = { it.getTitle(currentLang) },
                onItemSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (bottomNavScreens.getOrNull(page)) {
                Screen.Products -> ProductsScreen(viewModel = viewModel)
                Screen.Compare -> CompareScreen(
                    viewModel = viewModel,
                    onNavigateToProducts = {
                        val productsIndex = bottomNavScreens.indexOf(Screen.Products)
                        if (productsIndex >= 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(productsIndex)
                            }
                        }
                    }
                )
                Screen.Cart -> CartScreen(viewModel = viewModel)
                Screen.Profits -> ProfitsScreen(viewModel = viewModel)
                Screen.Import -> ImportScreen(viewModel = viewModel)
                else -> ProductsScreen(viewModel = viewModel)
            }
        }

        // Style Customization Popup Modal Dialog
        if (showStyleModal) {
            PersonalizeDialog(
                viewModel = viewModel,
                onDismiss = { showStyleModal = false }
            )
        }

        // Wishlist Modal Dialog
        if (showWishlistModal) {
            WishlistDialog(
                viewModel = viewModel,
                onDismiss = { showWishlistModal = false }
            )
        }

        // Smart Shopping List Modal Dialog (Lowest Available Store Price Engine)
        if (showSmartShoppingListModal) {
            SmartShoppingListDialog(
                viewModel = viewModel,
                onDismiss = { showSmartShoppingListModal = false }
            )
        }

        // Sync Dashboard Modal Dialog (WorkManager Status & Manual Sync)
        if (showSyncDashboardModal) {
            Dialog(
                onDismissRequest = { showSyncDashboardModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SyncDashboardScreen(
                        viewModel = viewModel,
                        onBack = { showSyncDashboardModal = false }
                    )
                }
            }
        }

        // Dedicated Settings Screen Modal Dialog
        if (showSettingsModal) {
            Dialog(
                onDismissRequest = { showSettingsModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { showSettingsModal = false }
                    )
                }
            }
        }

        // Full Screen Google Login Modal Dialog
        if (showAccountLoginModal) {
            Dialog(
                onDismissRequest = { showAccountLoginModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnexionScreen(
                        viewModel = viewModel,
                        onCloseModal = { showAccountLoginModal = false }
                    )
                }
            }
        }
    }
}

