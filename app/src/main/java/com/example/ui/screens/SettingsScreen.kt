package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CatalogViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.LanguageSelector
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.util.AppLanguage
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CatalogViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()

    // Sync state flows
    val isAutoSyncEnabled by viewModel.isFirestoreAutoSyncEnabled.collectAsState()
    val isPeriodicWorkManagerEnabled by viewModel.isPeriodicWorkManagerEnabled.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()

    // Scanner preferences
    val autoAddToCartOnScan by viewModel.autoAddToCartOnScan.collectAsState()
    val continuousScanMode by viewModel.continuousScanMode.collectAsState()
    val vibrateOnScan by viewModel.vibrateOnScan.collectAsState()

    // Auth & Room entities counts
    val isSignedIn by viewModel.isSignedInWithGoogle.collectAsState()
    val userEmail by viewModel.googleAccountEmail.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val saleRecords by viewModel.saleRecords.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()

    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

    val statusCardBg by animateColorAsState(
        targetValue = if (isAutoSyncEnabled) EmeraldSuccess.copy(alpha = 0.08f) else Color(0xFFF59E0B).copy(alpha = 0.08f),
        animationSpec = tween(400),
        label = "statusBg"
    )

    val statusBorderColor by animateColorAsState(
        targetValue = if (isAutoSyncEnabled) EmeraldSuccess.copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.35f),
        animationSpec = tween(400),
        label = "statusBorder"
    )

    val activeAccentColor = if (isAutoSyncEnabled) EmeraldSuccess else Color(0xFFD97706)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BluePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Paramètres & Données",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gestion Room SQLite & Cloud Firestore",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 0: LANGUAGE & REGIONAL SETTINGS (LanguageSelector Component)
            item {
                Text(
                    text = when (currentLang) {
                        AppLanguage.FRENCH -> "LANGUE & RÉGION"
                        AppLanguage.ENGLISH -> "LANGUAGE & REGION"
                        AppLanguage.CREOLE -> "LANGAZ & REZION"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BluePrimary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_selector_settings_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = AppStrings.languageSectionTitle(currentLang),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (currentLang) {
                                        AppLanguage.FRENCH -> "Basculez instantanément entre Français, Anglais et Kreol Morisien"
                                        AppLanguage.ENGLISH -> "Instantly toggle between English, French, and Mauritian Creole"
                                        AppLanguage.CREOLE -> "Sanz vitman ant Franse, Angle ek Kreol Morisien"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LanguageSelector(
                            selectedLanguage = currentLang,
                            onLanguageSelected = { newLang ->
                                viewModel.setAppLanguage(newLang)
                                Toast.makeText(
                                    context,
                                    AppStrings.languageSelectedToast(newLang),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }

            // SECTION 1: ROOM-TO-FIRESTORE SYNCHRONIZATION SETTINGS
            item {
                Text(
                    text = "SYNCHRONISATION DES DONNÉES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BluePrimary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firestore_sync_settings_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, statusBorderColor)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Header Switch Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = activeAccentColor.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isAutoSyncEnabled) Icons.Default.CloudSync else Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = activeAccentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Synchro Room ↔ Firestore",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAutoSyncEnabled) "Mode En Ligne (Cloud + Local)" else "Mode Hors-Ligne (Room SQLite)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = activeAccentColor
                                    )
                                }
                            }

                            Switch(
                                checked = isAutoSyncEnabled,
                                onCheckedChange = { newState ->
                                    viewModel.setFirestoreAutoSyncEnabled(newState)
                                    val msg = if (newState) "Synchronisation Cloud Firestore activée" else "Mode Hors-Ligne activé (Room SQLite uniquement)"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("firestore_sync_toggle_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldSuccess,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Description of Current Mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusCardBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isAutoSyncEnabled) {
                                        "⚡ Synchronisation temps réel active : Toutes les modifications de votre panier, commandes et catalogues sont sauvegardées dans Room SQLite local et synchronisées avec Cloud Firestore."
                                    } else {
                                        "🔒 Mode Hors-Ligne sécurisé : L'application fonctionne exclusivement avec la base de données locale Room SQLite. Aucune donnée n'est transmise sur le réseau afin d'économiser votre forfait mobile."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Sync Status & Details
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SyncDetailRow(
                                icon = if (isFirestoreSyncing) Icons.Default.Sync else Icons.Default.CheckCircle,
                                label = "Statut Réseau",
                                value = if (isFirestoreSyncing) "Synchronisation en cours..." else if (isAutoSyncEnabled) "Connecté & Synchronisé" else "Hors-Ligne (Room Seul)",
                                valueColor = activeAccentColor,
                                isLoading = isFirestoreSyncing
                            )

                            SyncDetailRow(
                                icon = Icons.Default.Storage,
                                label = "Projet Cloud Firestore",
                                value = "shopping-cart-80d07 (Default)",
                                valueColor = MaterialTheme.colorScheme.onSurface
                            )

                            SyncDetailRow(
                                icon = Icons.Default.AccountCircle,
                                label = "Compte Utilisateur",
                                value = if (isSignedIn && !userEmail.isNullOrBlank()) userEmail!! else "Invité (Local)",
                                valueColor = BluePrimary
                            )

                            SyncDetailRow(
                                icon = Icons.Default.Info,
                                label = "Dernier message",
                                value = firestoreSyncStatus,
                                valueColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Periodic WorkManager Background Sync Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPeriodicWorkManagerEnabled) Color(0xFF6366F1).copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.12f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            tint = if (isPeriodicWorkManagerEnabled) Color(0xFF6366F1) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Periodic WorkManager",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isPeriodicWorkManagerEnabled) Color(0xFFEEF2FF) else Color(0xFFF3F4F6)
                                        ) {
                                            Text(
                                                text = "Toutes les 6h",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPeriodicWorkManagerEnabled) Color(0xFF4F46E5) else Color.Gray,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isPeriodicWorkManagerEnabled) "Synchro auto en arrière-plan activée" else "Synchro auto en arrière-plan désactivée",
                                        fontSize = 11.sp,
                                        color = if (isPeriodicWorkManagerEnabled) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Switch(
                                checked = isPeriodicWorkManagerEnabled,
                                onCheckedChange = { newState ->
                                    viewModel.setPeriodicWorkManagerEnabled(context, newState)
                                    val msg = if (newState) "Periodic WorkManager activé (toutes les 6h)" else "Periodic WorkManager désactivé"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("periodic_workmanager_toggle_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6366F1),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Manual Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.forceSyncCartToFirestore()
                                    Toast.makeText(context, "Synchronisation lancée...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("force_sync_now_button")
                            ) {
                                if (isFirestoreSyncing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Synchroniser",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.toggleFirestoreAutoSync()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isAutoSyncEnabled) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAutoSyncEnabled) "Désactiver" else "Activer Cloud",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2: BARCODE SCANNER PREFERENCES (ML KIT)
            item {
                Text(
                    text = "SCANNER DE CODE-BARRES (ML KIT)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BluePrimary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Quick launch scanner banner
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showBarcodeScannerModal = true }
                                .testTag("open_scanner_from_settings"),
                            color = BluePrimary.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BluePrimary,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Ouvrir le Scanner ML Kit",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                        Text(
                                            text = "Scanner des produits pour le panier ou l'inventaire",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Preference 1: Auto add to cart on scan match
                        SettingToggleRow(
                            icon = Icons.Default.AddShoppingCart,
                            title = "Ajout direct au panier lors du scan",
                            subtitle = "Ajoute automatiquement 1 unité dès qu'un produit correspondant est détecté",
                            checked = autoAddToCartOnScan,
                            onCheckedChange = {
                                viewModel.setAutoAddToCartOnScan(it)
                            },
                            testTag = "auto_add_to_cart_scan_switch"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Preference 2: Continuous scanning mode
                        SettingToggleRow(
                            icon = Icons.Default.QrCodeScanner,
                            title = "Mode de scan continu",
                            subtitle = "Garde la caméra ouverte pour enchaîner plusieurs articles en rayon",
                            checked = continuousScanMode,
                            onCheckedChange = {
                                viewModel.setContinuousScanMode(it)
                            },
                            testTag = "continuous_scan_mode_switch"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Preference 3: Vibration / haptic feedback
                        SettingToggleRow(
                            icon = Icons.Default.Vibration,
                            title = "Confirmation tactile / vibration",
                            subtitle = "Vibre brièvement lors de la détection réussie d'un code-barres",
                            checked = vibrateOnScan,
                            onCheckedChange = {
                                viewModel.setVibrateOnScan(it)
                            },
                            testTag = "vibrate_on_scan_switch"
                        )
                    }
                }
            }

            // SECTION 3: ROOM LOCAL DATABASE INVENTORY STATS
            item {
                Text(
                    text = "BASE DE DONNÉES LOCALE ROOM SQLITE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BluePrimary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Entités enregistrées dans Room",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSuccess.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "SQLite Actif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats 2x2 Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoomStatCard(
                                title = "Produits",
                                count = "${allProducts.size}",
                                icon = Icons.Default.Inventory2,
                                color = BluePrimary,
                                modifier = Modifier.weight(1f)
                            )
                            RoomStatCard(
                                title = "Panier",
                                count = "${cartItems.sumOf { it.quantity }}",
                                icon = Icons.Default.ShoppingCart,
                                color = Color(0xFFEA580C),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoomStatCard(
                                title = "Ventes",
                                count = "${saleRecords.size}",
                                icon = Icons.Default.TrendingUp,
                                color = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            RoomStatCard(
                                title = "Favoris",
                                count = "${wishlistItems.size}",
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFFE11D48),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reset / Seed actions
                        OutlinedButton(
                            onClick = {
                                viewModel.resetAllData()
                                Toast.makeText(context, "Base de données Room réinitialisée", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_room_database_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Réinitialiser toutes les données Room", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // SECTION 4: USER PROFILE & ACCESS
            item {
                Text(
                    text = "COMPTE & SÉCURITÉ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BluePrimary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isSignedIn && !userEmail.isNullOrBlank()) userEmail!! else "Mode Invité Local",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Rôle : $userRole • Authentification Google",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (userRole == "ADMIN") Color(0xFFDC2626).copy(alpha = 0.12f) else BluePrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = userRole,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (userRole == "ADMIN") Color(0xFFDC2626) else BluePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Barcode scanner modal if opened
    if (showBarcodeScannerModal) {
        BarcodeScannerDialog(
            viewModel = viewModel,
            onDismiss = { showBarcodeScannerModal = false }
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = BluePrimary.copy(alpha = 0.08f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BluePrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB)
            )
        )
    }
}

@Composable
private fun SyncDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = BluePrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun RoomStatCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = count,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
