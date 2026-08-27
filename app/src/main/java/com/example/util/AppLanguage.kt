package com.example.util

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val subtitle: String
) {
    FRENCH(
        code = "fr",
        displayName = "Français",
        nativeName = "Français",
        flagEmoji = "🇫🇷",
        subtitle = "Langue française standard"
    ),
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeName = "English",
        flagEmoji = "🇬🇧",
        subtitle = "English (UK / US)"
    ),
    CREOLE(
        code = "mfe",
        displayName = "Kreol Morisien",
        nativeName = "Kreol Morisien",
        flagEmoji = "🇲🇺",
        subtitle = "Lang kreol Repiblik Moris"
    );

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: FRENCH
        }
    }
}

object AppStrings {
    // Personalize Screen Strings
    fun personalizeTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Personnalisation & Style"
        AppLanguage.ENGLISH -> "Personalization & Style"
        AppLanguage.CREOLE -> "Personnalizasion & Stil"
    }

    fun personalizeSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Customisez la langue, les couleurs, les polices, les arrondis et la disposition."
        AppLanguage.ENGLISH -> "Customize language, colors, fonts, shapes, and layout density."
        AppLanguage.CREOLE -> "Sanz langaz, bann kouler, stil lekritir, kwin bann kart ek lorganizasion."
    }

    // Language Section
    fun languageSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Langue de l'Application"
        AppLanguage.ENGLISH -> "Application Language"
        AppLanguage.CREOLE -> "Langaz Laplikasion"
    }

    fun languageSelectedToast(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Langue changée en Français 🇫🇷"
        AppLanguage.ENGLISH -> "Language changed to English 🇬🇧"
        AppLanguage.CREOLE -> "Langaz inn sanze pou Kreol Morisien 🇲🇺"
    }

    // Live Preview
    fun livePreview(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Aperçu en Direct"
        AppLanguage.ENGLISH -> "Live Preview"
        AppLanguage.CREOLE -> "Laperersu an Direk"
    }

    fun sampleProductName(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Riz Basmati Superiore 5kg"
        AppLanguage.ENGLISH -> "Basmati Superiore Rice 5kg"
        AppLanguage.CREOLE -> "Diri Basmati Superiore 5kg"
    }

    fun sampleCategory(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Épicerie Fine • Marque Laila"
        AppLanguage.ENGLISH -> "Fine Grocery • Laila Brand"
        AppLanguage.CREOLE -> "Bann Grosi Fin • Mark Laila"
    }

    fun marginLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Marge: +Rs 75.00"
        AppLanguage.ENGLISH -> "Profit Margin: +Rs 75.00"
        AppLanguage.CREOLE -> "Bénéfis: +Rs 75.00"
    }

    fun addButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Ajouter"
        AppLanguage.ENGLISH -> "Add"
        AppLanguage.CREOLE -> "Azoute"
    }

    // Theme Mode Section
    fun themeModeTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Mode de Thème"
        AppLanguage.ENGLISH -> "Theme Mode"
        AppLanguage.CREOLE -> "Mod Tem"
    }

    fun themeLight(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Clair"
        AppLanguage.ENGLISH -> "Light"
        AppLanguage.CREOLE -> "Kler"
    }

    fun themeDark(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Sombre"
        AppLanguage.ENGLISH -> "Dark"
        AppLanguage.CREOLE -> "Som"
    }

    fun themeSystem(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Système"
        AppLanguage.ENGLISH -> "System"
        AppLanguage.CREOLE -> "Sistem"
    }

    // Primary Color Section
    fun primaryColorTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Palette & Couleur Principale"
        AppLanguage.ENGLISH -> "Color Palette & Primary Hue"
        AppLanguage.CREOLE -> "Palet & Kouler Prinsipal"
    }

    // Font Style Section
    fun fontStyleTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Style de Police"
        AppLanguage.ENGLISH -> "Font Style"
        AppLanguage.CREOLE -> "Stil Lekritir"
    }

    fun fontSampleText(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Aa Bb Cc 123 - Exemple de texte"
        AppLanguage.ENGLISH -> "Aa Bb Cc 123 - Sample typography"
        AppLanguage.CREOLE -> "Aa Bb Cc 123 - Legzanp lekritir"
    }

    // Text Scale Section
    fun textScaleTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Échelle du Texte"
        AppLanguage.ENGLISH -> "Text Scale"
        AppLanguage.CREOLE -> "Grandeur Lekritir"
    }

    fun searchPlaceholder(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Rechercher un produit"
        AppLanguage.ENGLISH -> "Search for a product"
        AppLanguage.CREOLE -> "Resers enn produi"
    }

    // Shapes Section
    fun shapesTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Arrondi des Formes & Cartes"
        AppLanguage.ENGLISH -> "Card & Shape Corner Radius"
        AppLanguage.CREOLE -> "Kwin bann Kart & Form"
    }

    fun shapesSubtitle(lang: AppLanguage, radius: Int): String = when (lang) {
        AppLanguage.FRENCH -> "Applique des coins arrondis de ${radius}dp à tous les composants"
        AppLanguage.ENGLISH -> "Applies ${radius}dp rounded corners to all UI cards"
        AppLanguage.CREOLE -> "Aplik kwin ${radius}dp lor tou bann kart ek bwat"
    }

    // Layout Section
    fun layoutTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Disposition du Catalogue"
        AppLanguage.ENGLISH -> "Catalog Layout"
        AppLanguage.CREOLE -> "Lorganizasion Katalog"
    }

    fun layoutGridSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Affichage côte à côte en 2 colonnes"
        AppLanguage.ENGLISH -> "Side-by-side 2-column grid layout"
        AppLanguage.CREOLE -> "Afisaz kot-a-kot lor 2 kolonn"
    }

    fun layoutListSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Affichage en liste verticale détaillée"
        AppLanguage.ENGLISH -> "Detailed vertical list layout"
        AppLanguage.CREOLE -> "Afisaz an lalis vertikal detaye"
    }

    // Reset button
    fun resetButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Réinitialiser les paramètres par défaut"
        AppLanguage.ENGLISH -> "Reset to default settings"
        AppLanguage.CREOLE -> "Remet tou reglaz par defo"
    }

    fun resetToast(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Paramètres de style et langue réinitialisés"
        AppLanguage.ENGLISH -> "Style and language settings reset"
        AppLanguage.CREOLE -> "Bann reglaz stil ek langaz inn remet par defo"
    }

    fun closeButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Fermer"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.CREOLE -> "Ferme"
    }

    // Navigation Tab Titles
    fun navProducts(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Produits"
        AppLanguage.ENGLISH -> "Products"
        AppLanguage.CREOLE -> "Produi"
    }

    fun navCompare(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Comparer"
        AppLanguage.ENGLISH -> "Compare"
        AppLanguage.CREOLE -> "Konpare"
    }

    fun navCart(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Panier"
        AppLanguage.ENGLISH -> "Cart"
        AppLanguage.CREOLE -> "Pannie"
    }

    fun navProfits(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Profits"
        AppLanguage.ENGLISH -> "Profits"
        AppLanguage.CREOLE -> "Bénéfis"
    }

    fun navStyle(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Style"
        AppLanguage.ENGLISH -> "Style"
        AppLanguage.CREOLE -> "Laparans"
    }

    fun navImport(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Importer"
        AppLanguage.ENGLISH -> "Import"
        AppLanguage.CREOLE -> "Inporte"
    }

    fun navSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Paramètres"
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.CREOLE -> "Bann Reglaz"
    }

    fun navWishlist(lang: AppLanguage): String = when (lang) {
        AppLanguage.FRENCH -> "Favoris"
        AppLanguage.ENGLISH -> "Wishlist"
        AppLanguage.CREOLE -> "Prefere"
    }
}
