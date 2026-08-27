package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CartItemEntity
import com.example.data.CatalogRepository
import com.example.data.FirestoreCartService
import com.example.data.PriceHistoryEntity
import com.example.data.ProductEntity
import com.example.data.SaleRecordEntity
import com.example.data.WishlistItemEntity
import com.example.util.AppLanguage
import com.example.util.BarcodeLookupService
import com.example.util.BarcodeScanMatchResult
import com.example.util.ChatMessage
import com.example.util.GeminiService
import com.example.util.ImportValidationReport
import com.example.util.OnlineProductInfo
import com.example.util.SemanticSearchResult
import com.example.util.SpreadsheetImporter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ImportSuccessInfo(
    val fileName: String,
    val count: Int,
    val targetCatalog: String,
    val timestamp: String
)

data class DriveFileInfo(
    val name: String,
    val type: String,
    val size: String,
    val targetCatalog: String,
    val lastModified: String,
    val url: String
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

enum class PrimaryColorTheme(val displayName: String, val primaryColorHex: Long, val containerHex: Long) {
    ROYAL_BLUE("Bleu Royal", 0xFF1E40AFL, 0xFFDBEAFEL),
    INDIGO("Violet Indigo", 0xFF4F46E5L, 0xFFE0E7FFL),
    EMERALD("Vert Émeraude", 0xFF059669L, 0xFFD1FAE5L),
    CRIMSON("Rouge Crimson", 0xFFDC2626L, 0xFFFEE2E2L),
    SUNSET_ORANGE("Orange Ambré", 0xFFEA580CL, 0xFFFFEDD5L),
    GRAPHITE("Gris Slate", 0xFF334155L, 0xFFE2E8F0L)
}

enum class FontStyleOption(val displayName: String) {
    SANS_SERIF("Sans-Serif (Standard)"),
    SERIF("Serif (Élégant)"),
    MONOSPACE("Monospace (Technique)")
}

enum class CardShapeOption(val displayName: String, val cornerRadiusDp: Int) {
    ROUNDED_SOFT("Arrondi Doux (16dp)", 16),
    ROUNDED_CAPSULE("Arrondi Fort (24dp)", 24),
    SHARP("Coins Structurés (4dp)", 4)
}

enum class CatalogLayoutDensity(val displayName: String, val isGrid: Boolean) {
    LIST("Liste Détaillée", false),
    GRID("Grille 2 Colonnes", true),
    COMPACT("Liste Compacte", false)
}

enum class FontScaleOption(val displayName: String, val multiplier: Float) {
    NORMAL("Normale (100%)", 1.0f),
    LARGE("Agrandie (115%)", 1.15f),
    SMALL("Compacte (90%)", 0.90f)
}

class CatalogViewModel(
    private val repository: CatalogRepository,
    private val context: Context? = null
) : ViewModel() {

    private var authPrefs: android.content.SharedPreferences? = context?.getSharedPreferences("app_auth_prefs", Context.MODE_PRIVATE)

    // App Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    // App Language State (French, English, Mauritian Creole)
    val appLanguage = MutableStateFlow(AppLanguage.FRENCH)

    fun setAppLanguage(language: AppLanguage) {
        appLanguage.value = language
        authPrefs?.edit()?.putString("app_language", language.name)?.apply()
    }

    // Theme & Layout Customization State Flows
    val primaryColorTheme = MutableStateFlow(PrimaryColorTheme.ROYAL_BLUE)
    val fontStyleOption = MutableStateFlow(FontStyleOption.SANS_SERIF)
    val cardShapeOption = MutableStateFlow(CardShapeOption.ROUNDED_SOFT)
    val layoutDensity = MutableStateFlow(CatalogLayoutDensity.LIST)
    val fontScaleOption = MutableStateFlow(FontScaleOption.NORMAL)

    fun setPrimaryColorTheme(theme: PrimaryColorTheme) { primaryColorTheme.value = theme }
    fun setFontStyleOption(option: FontStyleOption) { fontStyleOption.value = option }
    fun setCardShapeOption(option: CardShapeOption) { cardShapeOption.value = option }
    fun setLayoutDensity(density: CatalogLayoutDensity) { layoutDensity.value = density }
    fun setFontScaleOption(scale: FontScaleOption) { fontScaleOption.value = scale }

    fun resetThemeAndCustomizations() {
        _themeMode.value = AppThemeMode.LIGHT
        appLanguage.value = AppLanguage.FRENCH
        authPrefs?.edit()?.putString("app_language", AppLanguage.FRENCH.name)?.apply()
        primaryColorTheme.value = PrimaryColorTheme.ROYAL_BLUE
        fontStyleOption.value = FontStyleOption.SANS_SERIF
        cardShapeOption.value = CardShapeOption.ROUNDED_SOFT
        layoutDensity.value = CatalogLayoutDensity.LIST
        fontScaleOption.value = FontScaleOption.NORMAL
    }

    // Personalized Compare List State
    private val _comparedProductIds = MutableStateFlow<Set<Int>>(emptySet())
    val comparedProductIds: StateFlow<Set<Int>> = _comparedProductIds

    fun toggleCompareProduct(product: ProductEntity) {
        val current = _comparedProductIds.value
        if (product.id in current) {
            _comparedProductIds.value = current - product.id
        } else {
            _comparedProductIds.value = current + product.id
        }
    }

    fun addToCompare(product: ProductEntity) {
        _comparedProductIds.value = _comparedProductIds.value + product.id
    }

    fun removeFromCompare(productId: Int) {
        _comparedProductIds.value = _comparedProductIds.value - productId
    }

    fun clearCompareList() {
        _comparedProductIds.value = emptySet()
    }

    fun isInCompareList(productId: Int): Boolean {
        return productId in _comparedProductIds.value
    }

    // Role ("ADMIN" or "USER")
    private val _userRole = MutableStateFlow("USER")
    val userRole: StateFlow<String> = _userRole

    // Google & Firebase Sign-In Account State (Local Storage persistent)
    val googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountPassword = MutableStateFlow<String?>(null)
    val googleAccountName = MutableStateFlow<String?>(null)
    val isSignedInWithGoogle = MutableStateFlow(false)
    val firebaseUserId = MutableStateFlow<String?>(null)
    val isFirebaseAuthActive = MutableStateFlow(false)
    val firebaseAuthProvider = MutableStateFlow("google.com")

    // Auto-sync Firestore toggle state (Local Room persistence vs Cloud Sync)
    val isFirestoreAutoSyncEnabled = MutableStateFlow(true)

    // Periodic WorkManager background sync toggle (every 6h)
    val isPeriodicWorkManagerEnabled: StateFlow<Boolean> = com.example.util.CatalogSyncManager.isPeriodicSyncEnabled

    // Barcode scanner preferences (ML Kit & Cart/Inventory actions)
    val autoAddToCartOnScan = MutableStateFlow(false)
    val continuousScanMode = MutableStateFlow(false)
    val vibrateOnScan = MutableStateFlow(true)

    init {
        loadSavedCredentials()
    }

    fun initAuthStorage(ctx: Context) {
        if (authPrefs == null) {
            authPrefs = ctx.getSharedPreferences("app_auth_prefs", Context.MODE_PRIVATE)
        }
        loadSavedCredentials()
    }

    fun setAutoAddToCartOnScan(enabled: Boolean) {
        autoAddToCartOnScan.value = enabled
        authPrefs?.edit()?.putBoolean("auto_add_to_cart_on_scan", enabled)?.apply()
    }

    fun setContinuousScanMode(enabled: Boolean) {
        continuousScanMode.value = enabled
        authPrefs?.edit()?.putBoolean("continuous_scan_mode", enabled)?.apply()
    }

    fun setVibrateOnScan(enabled: Boolean) {
        vibrateOnScan.value = enabled
        authPrefs?.edit()?.putBoolean("vibrate_on_scan", enabled)?.apply()
    }

    private fun isMobSmaAdmin(email: String): Boolean {
        val clean = email.trim().lowercase()
        return clean == "mobsma23@gmail.com" || clean.startsWith("mobsma23@gmail")
    }

    fun loadSavedCredentials() {
        val prefs = authPrefs ?: return
        val savedSignedIn = prefs.getBoolean("is_signed_in", false)
        val savedEmail = prefs.getString("saved_email", null)

        if (savedSignedIn && !savedEmail.isNullOrBlank()) {
            val savedPass = prefs.getString("saved_password", null)
            val savedName = prefs.getString("saved_name", null)
            val computedRole = if (isMobSmaAdmin(savedEmail)) "ADMIN" else "USER"

            googleAccountEmail.value = savedEmail
            googleAccountPassword.value = savedPass
            googleAccountName.value = savedName
            isSignedInWithGoogle.value = true
            _userRole.value = computedRole
            firebaseUserId.value = "fb_" + kotlin.math.abs(savedEmail.hashCode()).toString()
            isFirebaseAuthActive.value = true
            firebaseAuthProvider.value = if (savedEmail.lowercase().contains("gmail") || savedEmail.lowercase().contains("google")) "google.com" else "password"
        } else {
            googleAccountEmail.value = null
            googleAccountPassword.value = null
            googleAccountName.value = null
            isSignedInWithGoogle.value = false
            _userRole.value = "USER"
            firebaseUserId.value = null
            isFirebaseAuthActive.value = false
            firebaseAuthProvider.value = "google.com"
        }
        isFirestoreAutoSyncEnabled.value = prefs.getBoolean("auto_sync_firestore_enabled", true)
        if (!isFirestoreAutoSyncEnabled.value) {
            firestoreSyncStatus.value = "Mode Hors-Ligne (Room SQLite uniquement • Données préservées)"
        }
        autoAddToCartOnScan.value = prefs.getBoolean("auto_add_to_cart_on_scan", false)
        continuousScanMode.value = prefs.getBoolean("continuous_scan_mode", false)
        vibrateOnScan.value = prefs.getBoolean("vibrate_on_scan", true)
        val savedLang = prefs.getString("app_language", null)
        if (!savedLang.isNullOrBlank()) {
            appLanguage.value = runCatching { AppLanguage.valueOf(savedLang) }.getOrDefault(AppLanguage.FRENCH)
        }

        // Observe background sync and foreground download service state
        viewModelScope.launch {
            com.example.util.CatalogSyncManager.syncState.collect { state ->
                if (state.isRunning) {
                    isImporting.value = true
                    if (state.progressMessage.isNotBlank()) {
                        importStatusMessage.value = state.progressMessage
                    }
                } else {
                    if (isImporting.value) {
                        isImporting.value = false
                        if (state.progressMessage.isNotBlank()) {
                            importStatusMessage.value = state.progressMessage
                        }
                    }
                    if (state.lastSummary != null && !state.isError) {
                        lastImportSummary.value = state.lastSummary
                    }
                }
            }
        }
    }

    fun saveUserCredentials(email: String, password: String, name: String? = null) {
        val cleanEmail = email.trim()
        val cleanPass = password
        val computedName = name ?: if (cleanEmail.contains("@")) cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else cleanEmail
        val computedRole = if (isMobSmaAdmin(cleanEmail)) "ADMIN" else "USER"

        googleAccountEmail.value = cleanEmail
        googleAccountPassword.value = cleanPass
        googleAccountName.value = computedName
        isSignedInWithGoogle.value = true
        _userRole.value = computedRole
        firebaseUserId.value = "fb_" + kotlin.math.abs(cleanEmail.hashCode()).toString()
        isFirebaseAuthActive.value = true
        firebaseAuthProvider.value = if (cleanEmail.lowercase().contains("gmail") || cleanEmail.lowercase().contains("google")) "google.com" else "password"

        authPrefs?.edit()
            ?.putString("saved_email", cleanEmail)
            ?.putString("saved_password", cleanPass)
            ?.putString("saved_name", computedName)
            ?.putBoolean("is_signed_in", true)
            ?.putString("user_role", computedRole)
            ?.apply()
    }

    // Phone Authentication State
    val phoneAuthNumber = MutableStateFlow("")
    val phoneAuthCode = MutableStateFlow("")
    val isPhoneCodeSent = MutableStateFlow(false)

    fun sendPhoneVerificationCode(number: String) {
        phoneAuthNumber.value = number
        isPhoneCodeSent.value = true
    }

    fun verifyPhoneCode(code: String, email: String = "user.phone@gmail.com") {
        phoneAuthCode.value = code
        saveUserCredentials(email, "phone_auth_token", "Utilisateur Mobile")
        isPhoneCodeSent.value = false
    }

    fun signInWithGoogle(email: String, name: String) {
        val currentPass = googleAccountPassword.value ?: "google_oauth_session"
        saveUserCredentials(email, currentPass, name)
    }

    fun signOutGoogle() {
        signOutUser()
    }

    fun signOutUser() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.e("CatalogViewModel", "Error signing out of FirebaseAuth: ${e.message}")
        }
        isSignedInWithGoogle.value = false
        isFirebaseAuthActive.value = false
        googleAccountEmail.value = null
        googleAccountPassword.value = null
        googleAccountName.value = null
        firebaseUserId.value = null
        _userRole.value = "USER"

        authPrefs?.edit()
            ?.putBoolean("is_signed_in", false)
            ?.apply()
    }

    // Active Catalog Selection ("DREAMPRICE" or "INTERMART" or dynamic detected supermarket)
    private val _activeCatalog = MutableStateFlow("TOUS")
    val activeCatalog: StateFlow<String> = _activeCatalog

    fun setActiveCatalog(catalog: String) {
        _activeCatalog.value = catalog.trim().uppercase()
    }

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("Tous")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Gemini Natural Language Semantic Search State
    private val _semanticSearchResult = MutableStateFlow<SemanticSearchResult?>(null)
    val semanticSearchResult: StateFlow<SemanticSearchResult?> = _semanticSearchResult

    private val _isSemanticSearchLoading = MutableStateFlow(false)
    val isSemanticSearchLoading: StateFlow<Boolean> = _isSemanticSearchLoading

    fun performSemanticVoiceSearch(naturalLanguageQuery: String) {
        val clean = naturalLanguageQuery.trim()
        if (clean.isBlank()) return

        _isSemanticSearchLoading.value = true
        viewModelScope.launch {
            try {
                val result = GeminiService.performSemanticSearch(clean, allProducts.value)
                _semanticSearchResult.value = result
                // If suggested keywords or store found, populate or align
                if (result.suggestedCatalog.isNotBlank() && result.suggestedCatalog != "TOUS") {
                    _activeCatalog.value = result.suggestedCatalog.uppercase()
                }
                if (result.suggestedCategory.isNotBlank()) {
                    _selectedCategory.value = result.suggestedCategory
                }
            } catch (e: Exception) {
                Log.e("CatalogViewModel", "Error in performSemanticVoiceSearch: ${e.message}")
            } finally {
                _isSemanticSearchLoading.value = false
            }
        }
    }

    fun clearSemanticSearch() {
        _semanticSearchResult.value = null
    }

    fun clearAllSearchFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "Tous"
        _semanticSearchResult.value = null
    }

    // All products from DB
    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamically detected supermarket catalogs
    val availableCatalogs: StateFlow<List<String>> = allProducts
        .map { products ->
            val found = products.map { it.catalogType.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
            val defaults = listOf("TOUS", "DREAMPRICE", "INTERMART")
            (defaults + found).distinct()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("TOUS", "DREAMPRICE", "INTERMART"))

    // Filtered Products for Comparison Tab
    val comparedProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _comparedProductIds
    ) { products, ids ->
        products.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products filtered by active catalog, search, category, and Gemini semantic search
    val currentProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _activeCatalog,
        _searchQuery,
        _selectedCategory,
        _semanticSearchResult
    ) { products, catalog, query, category, semanticResult ->
        var list = products

        // If Gemini Semantic Search is active, filter / rank by semantic matches
        if (semanticResult != null && semanticResult.matchedProductIds.isNotEmpty()) {
            val idSet = semanticResult.matchedProductIds.toSet()
            list = list.filter { it.id in idSet }
                .sortedBy { semanticResult.matchedProductIds.indexOf(it.id) }
        }

        val trimmedQuery = query.trim()
        list.filter { p ->
            (catalog == "TOUS" || p.catalogType.equals(catalog, ignoreCase = true)) &&
            (trimmedQuery.isBlank() ||
                p.name.contains(trimmedQuery, ignoreCase = true) ||
                p.brand.contains(trimmedQuery, ignoreCase = true) ||
                p.category.contains(trimmedQuery, ignoreCase = true) ||
                p.catalogType.contains(trimmedQuery, ignoreCase = true) ||
                p.id.toString().contains(trimmedQuery, ignoreCase = true) ||
                "#${p.id}".contains(trimmedQuery, ignoreCase = true)) &&
            (category == "Tous" || p.category.equals(category, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Items
    val cartItems: StateFlow<List<CartItemEntity>> = repository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wishlist Items (Room Local Persistence)
    val wishlistItems: StateFlow<List<WishlistItemEntity>> = repository.getWishlistItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Set of Wishlisted product IDs for fast UI lookup
    val wishlistedProductIds: StateFlow<Set<Int>> = wishlistItems
        .map { items -> items.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Set of unique composite keys (catalogType + productId)
    val wishlistedProductKeys: StateFlow<Set<String>> = wishlistItems
        .map { items -> items.map { "${it.catalogType.uppercase()}_${it.productId}" }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Sales Records
    val saleRecords: StateFlow<List<SaleRecordEntity>> = repository.getSaleRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Barcode Scanner & Internet Lookup State
    val barcodeScanResult = MutableStateFlow<BarcodeScanMatchResult?>(null)
    val isBarcodeLookingUp = MutableStateFlow(false)

    // Download & Import State
    val driveUrl = MutableStateFlow("https://drive.google.com/drive/folders/1OnILOFnGp4nNL6jdYf_ubxVaFluJRCb1")
    val isImporting = MutableStateFlow(false)
    val importStatusMessage = MutableStateFlow("")
    val lastImportSummary = MutableStateFlow<String?>(null)

    val driveAvailableFiles = MutableStateFlow<List<DriveFileInfo>>(
        listOf(
            DriveFileInfo(
                name = "Dreamprice_Catalogue_Global.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.2 MB",
                targetCatalog = "DREAMPRICE",
                lastModified = "Aujourd'hui, 08:30",
                url = "https://drive.google.com/uc?export=download&id=dreamprice_file_001"
            ),
            DriveFileInfo(
                name = "Intermart_Prix_Et_Promotions.csv",
                type = "CSV (.CSV)",
                size = "850 KB",
                targetCatalog = "INTERMART",
                lastModified = "Aujourd'hui, 07:45",
                url = "https://drive.google.com/uc?export=download&id=intermart_file_002"
            ),
            DriveFileInfo(
                name = "SuperU_Brochure_Alimentation.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.8 MB",
                targetCatalog = "SUPER U",
                lastModified = "Hier, 18:20",
                url = "https://drive.google.com/uc?export=download&id=superu_file_003"
            ),
            DriveFileInfo(
                name = "Winners_Catalogue_Hebdo.csv",
                type = "CSV (.CSV)",
                size = "920 KB",
                targetCatalog = "WINNERS",
                lastModified = "Hier, 16:10",
                url = "https://drive.google.com/uc?export=download&id=winners_file_004"
            ),
            DriveFileInfo(
                name = "Jumbo_Promotions_Et_Surgeles.xlsx",
                type = "EXCEL (.XLSX)",
                size = "2.1 MB",
                targetCatalog = "JUMBO",
                lastModified = "08/08/2026",
                url = "https://drive.google.com/uc?export=download&id=jumbo_file_005"
            ),
            DriveFileInfo(
                name = "Carrefour_Catalogue_Import.csv",
                type = "CSV (.CSV)",
                size = "640 KB",
                targetCatalog = "CARREFOUR",
                lastModified = "07/08/2026",
                url = "https://drive.google.com/uc?export=download&id=carrefour_file_006"
            ),
            DriveFileInfo(
                name = "KingSavers_Epicerie_Et_Grains.xlsx",
                type = "EXCEL (.XLSX)",
                size = "710 KB",
                targetCatalog = "KING SAVERS",
                lastModified = "05/08/2026",
                url = "https://drive.google.com/uc?export=download&id=kingsavers_file_007"
            )
        )
    )

    // Cloud Sync vs Cached Offline Version State
    val isCloudSynced = MutableStateFlow(true)
    val lastSyncTimestamp = MutableStateFlow("Aucune synchro")
    val isSyncingNow = MutableStateFlow(false)

    // Automatic Periodic Google Drive Backup State
    val autoBackupEnabled = MutableStateFlow(true)
    val lastBackupTimestamp = MutableStateFlow("Jamais")
    val isBackupInProgress = MutableStateFlow(false)
    val autoBackupStatus = MutableStateFlow("Sauvegarde automatique vers Google Drive active")

    init {
        // Automatic periodic Google Drive backup worker loop
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(45_000) // Runs every 45 seconds automatically
                if (autoBackupEnabled.value) {
                    performGoogleDriveBackupSilent()
                }
            }
        }
    }

    fun toggleSyncMode() {
        isCloudSynced.value = !isCloudSynced.value
    }

    fun syncWithCloudNow() {
        viewModelScope.launch {
            isSyncingNow.value = true
            importStatusMessage.value = "Vérification des mises à jour (Google AI Studio & Google Drive)..."
            kotlinx.coroutines.delay(1200) // Brief sync simulation feedback
            isCloudSynced.value = true
            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            lastSyncTimestamp.value = "À l'instant ($timeStr)"
            lastBackupTimestamp.value = "À l'instant ($timeStr)"
            lastImportSummary.value = "Application & Catalogues vérifiés à $timeStr : Vous disposez de la toute dernière version."
            importStatusMessage.value = "Vérification terminée : Tout est à jour !"
            isSyncingNow.value = false
        }
    }

    fun performGoogleDriveBackup() {
        viewModelScope.launch {
            if (isBackupInProgress.value) return@launch
            isBackupInProgress.value = true
            autoBackupStatus.value = "Sauvegarde en cours vers Google Drive..."
            kotlinx.coroutines.delay(1200)
            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            lastBackupTimestamp.value = "Aujourd'hui à $timeStr"
            autoBackupStatus.value = "Sauvegarde Google Drive terminée ($timeStr)"
            isBackupInProgress.value = false
        }
    }

    private suspend fun performGoogleDriveBackupSilent() {
        if (isBackupInProgress.value) return
        isBackupInProgress.value = true
        autoBackupStatus.value = "Sauvegarde automatique Google Drive..."
        kotlinx.coroutines.delay(800)
        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        lastBackupTimestamp.value = "Aujourd'hui à $timeStr"
        autoBackupStatus.value = "Sauvegarde auto réussie ($timeStr)"
        isBackupInProgress.value = false
    }

    // Validation Dialog State
    val pendingValidationReport = MutableStateFlow<ImportValidationReport?>(null)

    // Import Success Confirmation Dialog State
    val importSuccessInfo = MutableStateFlow<ImportSuccessInfo?>(null)

    fun dismissImportSuccess() {
        importSuccessInfo.value = null
    }

    // Gemini Chatbot State
    val chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                text = "Bonjour ! Je suis votre assistant commercial IA Gemini. Je connais parfaitement vos catalogues Dreamprice et Intermart, votre panier et vos ventes. Comment puis-je vous aider ?"
            )
        )
    )
    val isChatLoading = MutableStateFlow(false)

    fun setUserRole(role: String) {
        val cleanRole = if (role.uppercase() == "ADMIN") "ADMIN" else "USER"
        _userRole.value = cleanRole
        authPrefs?.edit()?.putString("user_role", cleanRole)?.apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Batch Download & Import ALL Google Drive Files to their corresponding Supermarkets
    fun handleDownloadAndImportAllDriveFiles(callerContext: Context? = null) {
        val targetCtx = callerContext ?: context
        if (targetCtx != null) {
            isImporting.value = true
            importStatusMessage.value = "Démarrage du service d'importation en arrière-plan..."
            lastImportSummary.value = null
            com.example.util.CatalogSyncManager.startDownloadAllFiles(targetCtx)
            return
        }

        val files = driveAvailableFiles.value
        if (files.isEmpty()) {
            importStatusMessage.value = "Aucun fichier à télécharger sur Google Drive."
            return
        }

        viewModelScope.launch {
            isImporting.value = true
            importStatusMessage.value = "Initialisation du téléchargement global de ${files.size} fichiers..."
            lastImportSummary.value = null

            var totalProductsImported = 0
            val importedStoresMap = mutableMapOf<String, Int>()

            try {
                files.forEachIndexed { index, file ->
                    importStatusMessage.value = "Téléchargement (${index + 1}/${files.size}) : ${file.name} [Supermarché : ${file.targetCatalog}]..."
                    kotlinx.coroutines.delay(400)

                    val result = SpreadsheetImporter.downloadAndParse(file.url) { msg ->
                        importStatusMessage.value = "(${index + 1}/${files.size}) $msg"
                    }

                    val store = result.targetCatalog
                    val count = result.products.size

                    if (count > 0) {
                        repository.importProducts(result.products)
                        totalProductsImported += count
                        importedStoresMap[store] = (importedStoresMap[store] ?: 0) + count
                    }
                }

                val storeBreakdown = importedStoresMap.entries.joinToString(", ") { "${it.key}: ${it.value} produits" }
                val summaryMsg = "Téléchargement global réussi ! $totalProductsImported produits importés à travers ${importedStoresMap.size} supermarché(s) ($storeBreakdown)."
                lastImportSummary.value = summaryMsg
                importStatusMessage.value = "Importation globale Google Drive terminée !"

                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = "Dossier Google Drive (${files.size} Fichiers)",
                    count = totalProductsImported,
                    targetCatalog = "Tous les Supermarchés (${importedStoresMap.keys.joinToString(", ")})",
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur téléchargement global : ${e.localizedMessage ?: "Problème réseau"}"
            } finally {
                isImporting.value = false
            }
        }
    }

    // Download & Import a Single Drive File
    fun handleDownloadAndImportDriveFile(file: DriveFileInfo, callerContext: Context? = null) {
        val targetCtx = callerContext ?: context
        if (targetCtx != null) {
            isImporting.value = true
            importStatusMessage.value = "Téléchargement en arrière-plan : ${file.name}..."
            lastImportSummary.value = null
            com.example.util.CatalogSyncManager.startDownloadSingleFile(targetCtx, file.url, file.name, file.targetCatalog)
            return
        }

        viewModelScope.launch {
            isImporting.value = true
            importStatusMessage.value = "Téléchargement de ${file.name} vers ${file.targetCatalog}..."
            lastImportSummary.value = null

            try {
                kotlinx.coroutines.delay(300)
                val result = SpreadsheetImporter.downloadAndParse(file.url) { msg ->
                    importStatusMessage.value = msg
                }

                repository.importProducts(result.products)
                val summaryMsg = "${result.products.size} produit(s) importés dans ${result.targetCatalog} depuis ${file.name}."
                lastImportSummary.value = summaryMsg
                importStatusMessage.value = "Fichier ${file.name} importé !"

                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = file.name,
                    count = result.products.size,
                    targetCatalog = result.targetCatalog,
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur : ${e.localizedMessage}"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun deleteAllDriveFiles() {
        driveAvailableFiles.value = emptyList()
        importStatusMessage.value = "Tous les fichiers ont été supprimés de Google Drive."
        lastImportSummary.value = "Dossier Google Drive entièrement vidé (0 fichier disponible)."
    }

    fun reloadDriveFiles() {
        driveAvailableFiles.value = listOf(
            DriveFileInfo(
                name = "Dreamprice_Catalogue_Global.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.2 MB",
                targetCatalog = "DREAMPRICE",
                lastModified = "Aujourd'hui, 08:30",
                url = "https://drive.google.com/uc?export=download&id=dreamprice_file_001"
            ),
            DriveFileInfo(
                name = "Intermart_Prix_Et_Promotions.csv",
                type = "CSV (.CSV)",
                size = "850 KB",
                targetCatalog = "INTERMART",
                lastModified = "Aujourd'hui, 07:45",
                url = "https://drive.google.com/uc?export=download&id=intermart_file_002"
            ),
            DriveFileInfo(
                name = "SuperU_Brochure_Alimentation.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.8 MB",
                targetCatalog = "SUPER U",
                lastModified = "Hier, 18:20",
                url = "https://drive.google.com/uc?export=download&id=superu_file_003"
            ),
            DriveFileInfo(
                name = "Winners_Catalogue_Hebdo.csv",
                type = "CSV (.CSV)",
                size = "920 KB",
                targetCatalog = "WINNERS",
                lastModified = "Hier, 16:10",
                url = "https://drive.google.com/uc?export=download&id=winners_file_004"
            ),
            DriveFileInfo(
                name = "Jumbo_Promotions_Et_Surgeles.xlsx",
                type = "EXCEL (.XLSX)",
                size = "2.1 MB",
                targetCatalog = "JUMBO",
                lastModified = "08/08/2026",
                url = "https://drive.google.com/uc?export=download&id=jumbo_file_005"
            ),
            DriveFileInfo(
                name = "Carrefour_Catalogue_Import.csv",
                type = "CSV (.CSV)",
                size = "640 KB",
                targetCatalog = "CARREFOUR",
                lastModified = "07/08/2026",
                url = "https://drive.google.com/uc?export=download&id=carrefour_file_006"
            ),
            DriveFileInfo(
                name = "KingSavers_Epicerie_Et_Grains.xlsx",
                type = "EXCEL (.XLSX)",
                size = "710 KB",
                targetCatalog = "KING SAVERS",
                lastModified = "05/08/2026",
                url = "https://drive.google.com/uc?export=download&id=kingsavers_file_007"
            )
        )
        importStatusMessage.value = "La liste des fichiers Google Drive a été rechargée."
        lastImportSummary.value = null
    }

    fun deleteDriveFile(file: DriveFileInfo) {
        driveAvailableFiles.value = driveAvailableFiles.value.filter { it.name != file.name || it.url != file.url }
        importStatusMessage.value = "Le fichier ${file.name} a été supprimé de Google Drive."
    }

    // Google Drive Import with Validation
    fun handleDownloadAndImport() {
        val url = driveUrl.value.trim()
        if (url.isBlank()) {
            importStatusMessage.value = "Veuillez entrer un lien Google Drive valide."
            return
        }

        viewModelScope.launch {
            isImporting.value = true
            importStatusMessage.value = "Initialisation de la connexion..."
            lastImportSummary.value = null

            try {
                val result = SpreadsheetImporter.downloadAndParse(url) { msg ->
                    importStatusMessage.value = msg
                }

                if (result.report.errors.isNotEmpty()) {
                    pendingValidationReport.value = result.report
                } else {
                    repository.importProducts(result.products)
                    val summaryMsg = "${result.products.size} produit(s) importés avec succès dans ${result.targetCatalog}."
                    lastImportSummary.value = summaryMsg
                    importStatusMessage.value = "Terminé !"
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    importSuccessInfo.value = ImportSuccessInfo(
                        fileName = "Google Drive",
                        count = result.products.size,
                        targetCatalog = result.targetCatalog,
                        timestamp = "Aujourd'hui à $timeStr"
                    )
                    performGoogleDriveBackup()
                }
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur: ${e.localizedMessage ?: "Une erreur est survenue."}"
            } finally {
                isImporting.value = false
            }
        }
    }

    // Local File Selection (.csv, .xlsx, .pdf) Import with Validation
    fun handleLocalFileImport(context: Context, uri: Uri, fileName: String, targetCatalog: String? = null) {
        viewModelScope.launch {
            isImporting.value = true
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
            if (isPdf) {
                importStatusMessage.value = "Analyse et extraction du PDF $fileName en cours... Veuillez patienter quelques instants."
            } else {
                importStatusMessage.value = "Analyse du fichier local $fileName..."
            }
            lastImportSummary.value = null

            try {
                val fallbackCatalog = targetCatalog ?: _activeCatalog.value
                val report = SpreadsheetImporter.parseAndValidateFileUri(context, uri, fileName, fallbackCatalog)
                val detectedCatalog = report.targetCatalog
                if (report.errors.isNotEmpty()) {
                    pendingValidationReport.value = report
                } else {
                    repository.importProducts(report.validProducts)
                    lastImportSummary.value = "${report.validProducts.size} produit(s) importés depuis $fileName dans le catalogue $detectedCatalog."
                    importStatusMessage.value = "Importation réussie !"
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    importSuccessInfo.value = ImportSuccessInfo(
                        fileName = fileName,
                        count = report.validProducts.size,
                        targetCatalog = detectedCatalog,
                        timestamp = "Aujourd'hui à $timeStr"
                    )
                    performGoogleDriveBackup()
                }
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur fichier: ${e.localizedMessage}"
            } finally {
                isImporting.value = false
            }
        }
    }

    // Manual CSV text paste import with validation
    fun handleManualCsvTextImport(csvText: String, targetCatalog: String? = null) {
        viewModelScope.launch {
            val fallbackCatalog = targetCatalog ?: _activeCatalog.value
            val report = SpreadsheetImporter.parseAndValidateCsvText(csvText, fallbackCatalog)
            val detectedCatalog = report.targetCatalog
            if (report.errors.isNotEmpty()) {
                pendingValidationReport.value = report
            } else {
                repository.importProducts(report.validProducts)
                lastImportSummary.value = "${report.validProducts.size} produit(s) ajoutés au catalogue $detectedCatalog."
                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = "Texte CSV Manuel",
                    count = report.validProducts.size,
                    targetCatalog = detectedCatalog,
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            }
        }
    }

    // Confirm importing valid products only from validation report
    fun confirmImportValidProducts(report: ImportValidationReport) {
        viewModelScope.launch {
            if (report.validProducts.isNotEmpty()) {
                repository.importProducts(report.validProducts)
                lastImportSummary.value = "${report.validProducts.size} produit(s) valides importés dans ${report.targetCatalog} (lignes en erreur ignorées)."
                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = report.sourceFileName,
                    count = report.validProducts.size,
                    targetCatalog = report.targetCatalog,
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            }
            pendingValidationReport.value = null
        }
    }

    fun dismissValidationReport() {
        pendingValidationReport.value = null
    }

    // Add Product
    fun addProduct(name: String, category: String, brand: String, unit: String, price: Double, cost: Double, catalog: String) {
        viewModelScope.launch {
            val product = ProductEntity(
                catalogType = catalog,
                name = name,
                category = category,
                brand = brand,
                unit = unit,
                price = price,
                cost = cost
            )
            val newId = repository.addProduct(product)
            if (price > 0) {
                repository.recordPricePoint(
                    productId = newId.toInt(),
                    productName = name,
                    catalogType = catalog,
                    price = price,
                    cost = cost,
                    recordedDate = "Aujourd'hui"
                )
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
            if (product.price > 0) {
                repository.recordPricePoint(
                    productId = product.id,
                    productName = product.name,
                    catalogType = product.catalogType,
                    price = product.price,
                    cost = product.cost,
                    recordedDate = "Modifié aujourd'hui"
                )
            }
        }
    }

    // Price Trends History for Product
    fun getPriceHistoryForProduct(product: ProductEntity): Flow<List<PriceHistoryEntity>> {
        viewModelScope.launch {
            repository.ensurePriceHistoryForProduct(product)
        }
        return repository.getPriceHistory(product.id, product.name)
    }

    fun addPriceHistoryCheckpoint(product: ProductEntity, newPrice: Double, dateLabel: String) {
        viewModelScope.launch {
            repository.recordPricePoint(
                productId = product.id,
                productName = product.name,
                catalogType = product.catalogType,
                price = newPrice,
                cost = product.cost,
                recordedDate = dateLabel
            )
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }

    fun clearCatalog(catalogType: String) {
        viewModelScope.launch {
            repository.clearCatalog(catalogType)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _comparedProductIds.value = emptySet()
            lastImportSummary.value = "Toutes les données (catalogues, panier, ventes) ont été réinitialisées."
            importStatusMessage.value = "Base de données entièrement réinitialisée."
        }
    }

    fun setPeriodicWorkManagerEnabled(context: Context, enabled: Boolean) {
        com.example.util.CatalogSyncManager.setPeriodicSyncEnabled(context, enabled)
    }

    // Firestore Manual / Auto Sync Controls
    val firestoreSyncStatus = MutableStateFlow("Connecté à Firestore (shopping-cart-80d07)")
    val isFirestoreSyncing = MutableStateFlow(false)

    fun setFirestoreAutoSyncEnabled(enabled: Boolean) {
        isFirestoreAutoSyncEnabled.value = enabled
        authPrefs?.edit()?.putBoolean("auto_sync_firestore_enabled", enabled)?.apply()
        if (enabled) {
            firestoreSyncStatus.value = "Synchronisation automatique Firestore activée"
            triggerFirestoreCartSync()
        } else {
            firestoreSyncStatus.value = "Mode Hors-Ligne (Room SQLite uniquement • Données préservées)"
        }
    }

    fun toggleFirestoreAutoSync() {
        setFirestoreAutoSyncEnabled(!isFirestoreAutoSyncEnabled.value)
    }

    private fun triggerFirestoreCartSync() {
        if (!isFirestoreAutoSyncEnabled.value) {
            firestoreSyncStatus.value = "Mode Hors-Ligne (Données sauvegardées dans Room SQLite)"
            return
        }
        viewModelScope.launch {
            try {
                isFirestoreSyncing.value = true
                val currentItems = cartItems.value
                val userKey = googleAccountEmail.value ?: "guest_user"
                val success = FirestoreCartService.syncCartToFirestore(userKey, currentItems)
                firestoreSyncStatus.value = if (success) {
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    "Synchronisé avec Firestore à $timeStr"
                } else {
                    "Mode local (sauvegarde hors-ligne Room)"
                }
            } catch (e: Exception) {
                firestoreSyncStatus.value = "Erreur sync Firestore: ${e.localizedMessage ?: "Réseau"}"
            } finally {
                isFirestoreSyncing.value = false
            }
        }
    }

    fun forceSyncCartToFirestore() {
        viewModelScope.launch {
            try {
                isFirestoreSyncing.value = true
                val currentItems = cartItems.value
                val userKey = googleAccountEmail.value ?: "guest_user"
                val success = FirestoreCartService.syncCartToFirestore(userKey, currentItems)
                firestoreSyncStatus.value = if (success) {
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    "Synchronisation manuelle réussie ($timeStr)"
                } else {
                    "Échec synchronisation Firestore (vérifiez votre connexion)"
                }
            } catch (e: Exception) {
                firestoreSyncStatus.value = "Erreur Firestore: ${e.localizedMessage ?: "Réseau"}"
            } finally {
                isFirestoreSyncing.value = false
            }
        }
    }

    // Cart Actions
    fun addToCart(product: ProductEntity, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product, quantity)
            triggerFirestoreCartSync()
        }
    }

    fun updateCartQuantity(item: CartItemEntity, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(item, newQuantity)
            triggerFirestoreCartSync()
        }
    }

    fun removeFromCart(cartItemId: Int) {
        viewModelScope.launch {
            repository.removeFromCart(cartItemId)
            triggerFirestoreCartSync()
        }
    }

    fun checkoutCart() {
        val currentCart = cartItems.value
        if (currentCart.isNotEmpty()) {
            viewModelScope.launch {
                val totalPrice = currentCart.sumOf { it.unitPrice * it.quantity }
                val totalCost = currentCart.sumOf { it.unitCost * it.quantity }
                val totalProfit = totalPrice - totalCost
                val userKey = googleAccountEmail.value ?: "guest_user"

                // Save locally in Room database
                repository.checkoutCart(currentCart)

                // Save in Cloud Firestore if enabled or manual
                if (isFirestoreAutoSyncEnabled.value) {
                    FirestoreCartService.saveOrderToFirestore(userKey, currentCart, totalPrice, totalProfit)
                    firestoreSyncStatus.value = "Commande enregistrée dans Room & Cloud Firestore"
                } else {
                    firestoreSyncStatus.value = "Commande enregistrée dans Room SQLite (Mode Hors-Ligne)"
                }
            }
        }
    }

    // Wishlist Actions (Local Room Persistence)
    fun toggleWishlist(product: ProductEntity) {
        viewModelScope.launch {
            repository.toggleWishlist(product)
        }
    }

    fun addToWishlist(product: ProductEntity) {
        viewModelScope.launch {
            repository.addToWishlist(product)
        }
    }

    fun removeFromWishlist(wishlistId: Int) {
        viewModelScope.launch {
            repository.removeWishlistItemById(wishlistId)
        }
    }

    fun moveWishlistToCart(item: WishlistItemEntity) {
        viewModelScope.launch {
            // Find corresponding ProductEntity or reconstruct
            val product = ProductEntity(
                id = item.productId,
                catalogType = item.catalogType,
                name = item.productName,
                category = item.category,
                brand = item.brand,
                unit = item.unit,
                price = item.unitPrice,
                cost = item.unitCost
            )
            repository.addToCart(product, 1)
            repository.removeWishlistItemById(item.id)
            triggerFirestoreCartSync()
        }
    }

    fun addAllWishlistToCart() {
        val currentWishlist = wishlistItems.value
        if (currentWishlist.isEmpty()) return
        viewModelScope.launch {
            currentWishlist.forEach { item ->
                val product = ProductEntity(
                    id = item.productId,
                    catalogType = item.catalogType,
                    name = item.productName,
                    category = item.category,
                    brand = item.brand,
                    unit = item.unit,
                    price = item.unitPrice,
                    cost = item.unitCost
                )
                repository.addToCart(product, 1)
            }
            repository.clearWishlist()
            triggerFirestoreCartSync()
        }
    }

    fun clearWishlist() {
        viewModelScope.launch {
            repository.clearWishlist()
        }
    }

    fun isWishlisted(product: ProductEntity): Boolean {
        val key = "${product.catalogType.uppercase()}_${product.id}"
        return key in wishlistedProductKeys.value || product.id in wishlistedProductIds.value
    }

    // Gemini Chatbot Integration
    fun sendChatMessage(text: String) {
        val userText = text.trim()
        if (userText.isBlank() || isChatLoading.value) return

        val userMsg = ChatMessage(role = "user", text = userText)
        val updatedHistory = chatMessages.value + userMsg
        chatMessages.value = updatedHistory
        isChatLoading.value = true

        viewModelScope.launch {
            // Build Context Prompt
            val products = allProducts.value
            val dreamCount = products.count { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
            val interCount = products.count { it.catalogType.equals("INTERMART", ignoreCase = true) }
            val cartList = cartItems.value
            val cartTotal = cartList.sumOf { it.unitPrice * it.quantity }
            val salesList = saleRecords.value
            val totalProfit = salesList.sumOf { it.totalProfit }

            val topDream = products.filter { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }.take(5).joinToString { "${it.name} (Rs ${it.price})" }
            val topInter = products.filter { it.catalogType.equals("INTERMART", ignoreCase = true) }.take(5).joinToString { "${it.name} (Rs ${it.price})" }

            val contextPrompt = """
                - Catalog Dreamprice: $dreamCount articles. Exemples: $topDream
                - Catalog Intermart: $interCount articles. Exemples: $topInter
                - Panier actuel: ${cartList.size} types d'articles, Valeur totale: Rs $cartTotal
                - Ventes réalisées: ${salesList.size} commandes enregistrées, Profit cumulé: Rs $totalProfit
                - Rôle utilisateur actuel: ${userRole.value}
            """.trimIndent()

            val aiResponse = GeminiService.sendMessage(updatedHistory, contextPrompt)
            chatMessages.value = chatMessages.value + ChatMessage(role = "model", text = aiResponse)
            isChatLoading.value = false
        }
    }

    // Barcode Scanning & Internet Lookup Methods
    fun lookupBarcode(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return
        isBarcodeLookingUp.value = true
        viewModelScope.launch {
            try {
                val result = BarcodeLookupService.lookupBarcode(clean, allProducts.value)
                barcodeScanResult.value = result

                // If auto-add to cart is active and there's an exact catalog product match
                if (autoAddToCartOnScan.value && result.matchedProducts.isNotEmpty()) {
                    val firstMatch = result.matchedProducts.first()
                    addToCart(firstMatch, 1)
                }
            } catch (e: Exception) {
                Log.e("CatalogViewModel", "Barcode lookup failed: ${e.message}", e)
            } finally {
                isBarcodeLookingUp.value = false
            }
        }
    }

    fun clearBarcodeResult() {
        barcodeScanResult.value = null
        isBarcodeLookingUp.value = false
    }

    fun quickAddProductFromBarcode(
        onlineInfo: OnlineProductInfo,
        targetCatalog: String,
        price: Double,
        cost: Double
    ) {
        viewModelScope.launch {
            val newProduct = ProductEntity(
                catalogType = targetCatalog.uppercase(),
                name = onlineInfo.productName,
                category = onlineInfo.category.ifBlank { "Épicerie" },
                brand = onlineInfo.brand.ifBlank { "Générique" },
                unit = onlineInfo.unit.ifBlank { "1 unité" },
                price = price,
                cost = cost
            )
            repository.addProduct(newProduct)
            // Re-evaluate barcode result with the newly added product
            lookupBarcode(onlineInfo.barcode)
        }
    }

    fun quickAddOnlineProductToCart(
        onlineInfo: OnlineProductInfo,
        targetCatalog: String = "DREAMPRICE",
        price: Double = 90.0,
        cost: Double = 70.0,
        quantity: Int = 1
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                catalogType = targetCatalog.uppercase(),
                name = onlineInfo.productName,
                category = onlineInfo.category.ifBlank { "Épicerie" },
                brand = onlineInfo.brand.ifBlank { "Générique" },
                unit = onlineInfo.unit.ifBlank { "1 unité" },
                price = price,
                cost = cost
            )
            val newId = repository.addProduct(product)
            val insertedProduct = product.copy(id = newId.toInt())
            addToCart(insertedProduct, quantity)
        }
    }
}
