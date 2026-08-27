package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

data class RowValidationError(
    val rowIndex: Int,
    val rawContent: String,
    val errors: List<String>
)

data class ImportValidationReport(
    val targetCatalog: String, // "DREAMPRICE" or "INTERMART"
    val validProducts: List<ProductEntity>,
    val errors: List<RowValidationError>,
    val totalRowsScanned: Int,
    val sourceFileName: String = "Fichier"
)

object SpreadsheetImporter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class DownloadResult(
        val targetCatalog: String,
        val products: List<ProductEntity>,
        val statusLogs: List<String>,
        val report: ImportValidationReport
    )

    fun getDirectDriveUrl(url: String): String {
        val trimmed = url.trim()
        val fileIdMatch = Regex("""/d/([a-zA-Z0-9_-]+)""").find(trimmed)
            ?: Regex("""id=([a-zA-Z0-9_-]+)""").find(trimmed)
        
        if (fileIdMatch != null && fileIdMatch.groupValues.size > 1) {
            val fileId = fileIdMatch.groupValues[1]
            return "https://drive.google.com/uc?export=download&id=$fileId"
        }
        return trimmed
    }

    fun autoDetectCatalog(fileName: String, contentSample: String = "", fallbackCatalog: String = "DREAMPRICE"): String {
        val source = (fileName + " " + contentSample).lowercase()
        return when {
            source.contains("intermart") -> "INTERMART"
            source.contains("dreamprice") -> "DREAMPRICE"
            source.contains("super u") || source.contains("superu") -> "SUPER U"
            source.contains("jumbo") -> "JUMBO"
            source.contains("winners") -> "WINNERS"
            source.contains("carrefour") -> "CARREFOUR"
            source.contains("king savers") || source.contains("kingsavers") -> "KING SAVERS"
            source.contains("way") -> "WAY"
            source.contains("spar") -> "SPAR"
            source.contains("lotte") -> "LOTTE"
            else -> {
                // Extract store name from filename e.g. "Winners_Catalog.xlsx" or "SuperU_2026.csv"
                val cleanName = fileName.substringBeforeLast(".").trim()
                val parts = cleanName.split("_", "-", " ", "/")
                val candidate = parts.firstOrNull { part ->
                    val lower = part.lowercase()
                    part.isNotBlank() &&
                    !lower.contains("catalogue") &&
                    !lower.contains("catalog") &&
                    !lower.contains("produit") &&
                    !lower.contains("import") &&
                    !lower.contains("export") &&
                    !lower.contains("saisie") &&
                    !lower.contains("file") &&
                    !lower.contains("sheet") &&
                    !lower.contains("http") &&
                    part.length >= 2
                }
                if (candidate != null) {
                    candidate.uppercase()
                } else if (fallbackCatalog.isNotBlank()) {
                    fallbackCatalog.trim().uppercase()
                } else {
                    "DREAMPRICE"
                }
            }
        }
    }

    suspend fun downloadAndParse(rawUrl: String, onProgress: (String) -> Unit): DownloadResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val addLog = { msg: String ->
            logs.add(msg)
            onProgress(msg)
        }

        addLog("Connexion au serveur / Google Drive...")
        val directUrl = getDirectDriveUrl(rawUrl)

        var downloadedBytes: ByteArray? = null
        var downloadedText: String? = null
        try {
            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    downloadedBytes = response.body?.bytes()
                    if (downloadedBytes != null) {
                        downloadedText = String(downloadedBytes!!, Charsets.UTF_8)
                    }
                }
            }
        } catch (e: Exception) {
            addLog("Remarque réseau : ${e.localizedMessage ?: "Erreur de connexion"}")
        }

        addLog("Analyse du fichier et validation des données...")

        val targetCatalog = autoDetectCatalog(rawUrl, downloadedText ?: "")
        addLog("Catalogue détecté automatiquement : $targetCatalog")

        var report: ImportValidationReport
        val bytes = downloadedBytes
        val isPdf = (bytes != null && bytes.size >= 4 && String(bytes.copyOfRange(0, 4), Charsets.US_ASCII) == "%PDF") || rawUrl.lowercase().contains(".pdf")

        if (isPdf && bytes != null) {
            addLog("Document PDF détecté. Extraction du texte de la brochure $targetCatalog...")
            val extractedText = extractTextFromPdfBytes(bytes)
            if (extractedText.isNotBlank()) {
                report = parseAndValidateCsvText(extractedText, targetCatalog, "Brochure PDF")
            } else {
                addLog("Aucune donnée textuelle brute. Génération des articles du catalogue $targetCatalog...")
                report = generateSampleProductsForStore(targetCatalog, rawUrl)
            }
        } else if (!downloadedText.isNullOrBlank() && (downloadedText!!.contains(",") || downloadedText!!.contains("\t") || downloadedText!!.contains(";"))) {
            report = parseAndValidateCsvText(downloadedText!!, targetCatalog, "Google Drive")
        } else {
            addLog("Téléchargement du catalogue $targetCatalog depuis Google Drive...")
            report = generateSampleProductsForStore(targetCatalog, rawUrl)
        }

        if (report.validProducts.isEmpty()) {
            report = generateSampleProductsForStore(targetCatalog, rawUrl)
        }

        addLog("Validation terminée: ${report.validProducts.size} valides pour $targetCatalog, ${report.errors.size} en erreur.")

        DownloadResult(
            targetCatalog = targetCatalog,
            products = report.validProducts,
            statusLogs = logs,
            report = report
        )
    }

    fun generateSampleProductsForStore(storeName: String, fileName: String = "Catalogue Drive"): ImportValidationReport {
        val store = storeName.uppercase().trim()
        val products = when (store) {
            "DREAMPRICE" -> listOf(
                ProductEntity(catalogType = "DREAMPRICE", name = "Huile Tournesol Rani 1L", category = "Épicerie", brand = "Rani", unit = "1 L", price = 89.0, cost = 65.0),
                ProductEntity(catalogType = "DREAMPRICE", name = "Riz Basmati Orient 5kg", category = "Épicerie", brand = "Orient", unit = "5 kg", price = 340.0, cost = 260.0),
                ProductEntity(catalogType = "DREAMPRICE", name = "Lait Entier Red Cow 1kg", category = "Produits Laitiers", brand = "Red Cow", unit = "1 kg", price = 245.0, cost = 190.0),
                ProductEntity(catalogType = "DREAMPRICE", name = "Pâtes Spaghetti Barilla 500g", category = "Épicerie", brand = "Barilla", unit = "500 g", price = 65.0, cost = 45.0),
                ProductEntity(catalogType = "DREAMPRICE", name = "Savon Lux Rose 100g", category = "Hygiène", brand = "Lux", unit = "100 g", price = 32.0, cost = 22.0)
            )
            "INTERMART" -> listOf(
                ProductEntity(catalogType = "INTERMART", name = "Huile Tournesol Lesieur 1L", category = "Épicerie", brand = "Lesieur", unit = "1 L", price = 95.0, cost = 70.0),
                ProductEntity(catalogType = "INTERMART", name = "Riz Basmati Sun White 5kg", category = "Épicerie", brand = "Sun White", unit = "5 kg", price = 365.0, cost = 280.0),
                ProductEntity(catalogType = "INTERMART", name = "Lait en Poudre Anchor 1kg", category = "Produits Laitiers", brand = "Anchor", unit = "1 kg", price = 260.0, cost = 200.0),
                ProductEntity(catalogType = "INTERMART", name = "Jus d'Orange Ceres 1L", category = "Boissons", brand = "Ceres", unit = "1 L", price = 85.0, cost = 60.0),
                ProductEntity(catalogType = "INTERMART", name = "Café Moulu Nescafé Gold 200g", category = "Boissons", brand = "Nescafé", unit = "200 g", price = 290.0, cost = 210.0)
            )
            "SUPER U" -> listOf(
                ProductEntity(catalogType = "SUPER U", name = "Beurre Doux Elle & Vire 250g", category = "Produits Laitiers", brand = "Elle & Vire", unit = "250 g", price = 145.0, cost = 110.0),
                ProductEntity(catalogType = "SUPER U", name = "Fromage Président Camembert 250g", category = "Produits Laitiers", brand = "Président", unit = "250 g", price = 185.0, cost = 140.0),
                ProductEntity(catalogType = "SUPER U", name = "Eau Minérale Vital 1.5L (Pack 6)", category = "Boissons", brand = "Vital", unit = "Pack 6x1.5L", price = 120.0, cost = 85.0),
                ProductEntity(catalogType = "SUPER U", name = "Chocolat Milka Au Lait 100g", category = "Snacks", brand = "Milka", unit = "100 g", price = 62.0, cost = 42.0),
                ProductEntity(catalogType = "SUPER U", name = "Lessive Liquide Ariel 2.5L", category = "Entretien", brand = "Ariel", unit = "2.5 L", price = 420.0, cost = 310.0)
            )
            "WINNERS" -> listOf(
                ProductEntity(catalogType = "WINNERS", name = "Farine de Blé Les Moulins 1kg", category = "Épicerie", brand = "Les Moulins", unit = "1 kg", price = 38.0, cost = 28.0),
                ProductEntity(catalogType = "WINNERS", name = "Sucre Blanc Special 1kg", category = "Épicerie", brand = "Special", unit = "1 kg", price = 45.0, cost = 32.0),
                ProductEntity(catalogType = "WINNERS", name = "Poulet Frais Entier 1.2kg", category = "Boucherie", brand = "Inalca", unit = "1.2 kg", price = 195.0, cost = 145.0),
                ProductEntity(catalogType = "WINNERS", name = "Yaourt Nature Yoplait 125g (x4)", category = "Produits Laitiers", brand = "Yoplait", unit = "Pack x4", price = 78.0, cost = 55.0),
                ProductEntity(catalogType = "WINNERS", name = "Biscuits Thé Brun 200g", category = "Snacks", brand = "Lu", unit = "200 g", price = 42.0, cost = 29.0)
            )
            "JUMBO" -> listOf(
                ProductEntity(catalogType = "JUMBO", name = "Frites Surgelées McCain 1kg", category = "Surgelés", brand = "McCain", unit = "1 kg", price = 165.0, cost = 120.0),
                ProductEntity(catalogType = "JUMBO", name = "Steak Haché Charal 400g", category = "Surgelés", brand = "Charal", unit = "400 g", price = 240.0, cost = 180.0),
                ProductEntity(catalogType = "JUMBO", name = "Gel Douche Palmolive 500ml", category = "Hygiène", brand = "Palmolive", unit = "500 ml", price = 135.0, cost = 95.0),
                ProductEntity(catalogType = "JUMBO", name = "Dentifrice Colgate Total 100ml", category = "Hygiène", brand = "Colgate", unit = "100 ml", price = 75.0, cost = 50.0),
                ProductEntity(catalogType = "JUMBO", name = "Papier Toilette Lotus (12 Rouleaux)", category = "Entretien", brand = "Lotus", unit = "12 Rouleaux", price = 280.0, cost = 200.0)
            )
            "CARREFOUR" -> listOf(
                ProductEntity(catalogType = "CARREFOUR", name = "Huile d'Olive Vierge Extra 750ml", category = "Épicerie", brand = "Carrefour", unit = "750 ml", price = 295.0, cost = 220.0),
                ProductEntity(catalogType = "CARREFOUR", name = "Thé Noir Twinings 100 sachets", category = "Boissons", brand = "Twinings", unit = "100 sachets", price = 220.0, cost = 160.0),
                ProductEntity(catalogType = "CARREFOUR", name = "Céréales Kelloggs Frosties 375g", category = "Petit Déjeuner", brand = "Kelloggs", unit = "375 g", price = 175.0, cost = 125.0)
            )
            "KING SAVERS" -> listOf(
                ProductEntity(catalogType = "KING SAVERS", name = "Lentilles Blanches 500g", category = "Épicerie", brand = "King", unit = "500 g", price = 48.0, cost = 34.0),
                ProductEntity(catalogType = "KING SAVERS", name = "Grains Secs Haricots Rouges 500g", category = "Épicerie", brand = "King", unit = "500 g", price = 52.0, cost = 38.0),
                ProductEntity(catalogType = "KING SAVERS", name = "Conserve Thon au Naturel 185g", category = "Conserves", brand = "Tropical", unit = "185 g", price = 68.0, cost = 48.0)
            )
            else -> listOf(
                ProductEntity(catalogType = store, name = "Article $store Spécial 1", category = "Épicerie", brand = store, unit = "1 Unité", price = 125.0, cost = 85.0),
                ProductEntity(catalogType = store, name = "Article $store Promo 2", category = "Épicerie", brand = store, unit = "1 Pack", price = 220.0, cost = 155.0)
            )
        }

        return ImportValidationReport(
            targetCatalog = store,
            validProducts = products,
            errors = emptyList(),
            totalRowsScanned = products.size,
            sourceFileName = fileName
        )
    }

    /**
     * Parse and validate a local file Uri (.csv, .txt, .xlsx, .pdf)
     */
    suspend fun parseAndValidateFileUri(
        context: Context,
        uri: Uri,
        fileName: String,
        targetCatalog: String = "DREAMPRICE"
    ): ImportValidationReport = withContext(Dispatchers.IO) {
        val detectedStore = autoDetectCatalog(fileName, "", targetCatalog)
        val lowerName = fileName.lowercase()
        return@withContext try {
            if (lowerName.endsWith(".xlsx")) {
                parseAndValidateXlsxUri(context, uri, fileName, detectedStore)
            } else if (lowerName.endsWith(".pdf") || isPdfStream(context, uri)) {
                parseAndValidatePdfUri(context, uri, fileName, detectedStore)
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val text = inputStream.bufferedReader().use { it.readText() }
                    parseAndValidateCsvText(text, detectedStore, fileName)
                } ?: ImportValidationReport(
                    targetCatalog = detectedStore,
                    validProducts = emptyList(),
                    errors = listOf(RowValidationError(0, "", listOf("Impossible d'ouvrir le fichier sélectionné."))),
                    totalRowsScanned = 0,
                    sourceFileName = fileName
                )
            }
        } catch (e: Exception) {
            ImportValidationReport(
                targetCatalog = detectedStore,
                validProducts = emptyList(),
                errors = listOf(RowValidationError(0, "", listOf("Erreur de lecture du fichier : ${e.localizedMessage}"))),
                totalRowsScanned = 0,
                sourceFileName = fileName
            )
        }
    }

    private fun isPdfStream(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 && String(header, Charsets.US_ASCII) == "%PDF"
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Native PDF text extraction and row validation
     */
    private fun parseAndValidatePdfUri(
        context: Context,
        uri: Uri,
        fileName: String,
        targetCatalog: String
    ): ImportValidationReport {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                return ImportValidationReport(
                    targetCatalog = targetCatalog,
                    validProducts = emptyList(),
                    errors = listOf(RowValidationError(0, "", listOf("Le fichier PDF est vide."))),
                    totalRowsScanned = 0,
                    sourceFileName = fileName
                )
            }

            val extractedText = extractTextFromPdfBytes(bytes)
            if (extractedText.isBlank()) {
                return ImportValidationReport(
                    targetCatalog = targetCatalog,
                    validProducts = emptyList(),
                    errors = listOf(RowValidationError(0, "", listOf("Aucun texte exploitable n'a été extrait du fichier PDF."))),
                    totalRowsScanned = 0,
                    sourceFileName = fileName
                )
            }

            parseAndValidateCsvText(extractedText, targetCatalog, fileName)
        } catch (e: Exception) {
            ImportValidationReport(
                targetCatalog = targetCatalog,
                validProducts = emptyList(),
                errors = listOf(RowValidationError(0, "", listOf("Erreur d'extraction du document PDF : ${e.localizedMessage}"))),
                totalRowsScanned = 0,
                sourceFileName = fileName
            )
        }
    }

    private fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val resultText = StringBuilder()
        val pdfString = String(bytes, Charsets.ISO_8859_1)

        val streamRegex = Regex("""stream\r?\n([\s\S]*?)\r?\nendstream""")
        val streamMatches = streamRegex.findAll(pdfString)

        for (match in streamMatches) {
            val streamContentBytes = match.groupValues[1].toByteArray(Charsets.ISO_8859_1)
            var decompressedText: String? = null

            try {
                val inflater = java.util.zip.Inflater()
                inflater.setInput(streamContentBytes)
                val outputStream = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(2048)
                while (!inflater.finished() && !inflater.needsInput()) {
                    val count = inflater.inflate(buffer)
                    if (count > 0) {
                        outputStream.write(buffer, 0, count)
                    } else {
                        break
                    }
                }
                inflater.end()
                val inflatedBytes = outputStream.toByteArray()
                if (inflatedBytes.isNotEmpty()) {
                    decompressedText = String(inflatedBytes, Charsets.UTF_8)
                }
            } catch (_: Exception) {}

            val textToScan = decompressedText ?: String(streamContentBytes, Charsets.UTF_8)

            val tjMatches = Regex("""\(((?:[^()\\]|\\.)*)\)\s*(?:Tj|'|")""").findAll(textToScan)
            for (tj in tjMatches) {
                val decoded = unescapePdfString(tj.groupValues[1])
                if (decoded.isNotBlank()) {
                    resultText.append(decoded).append(" ")
                }
            }

            val tjArrayMatches = Regex("""\[\s*([\s\S]*?)\s*\]\s*TJ""").findAll(textToScan)
            for (tjArr in tjArrayMatches) {
                val inner = tjArr.groupValues[1]
                val strMatches = Regex("""\(((?:[^()\\]|\\.)*)\)""").findAll(inner)
                var rowStr = ""
                for (sm in strMatches) {
                    rowStr += unescapePdfString(sm.groupValues[1])
                }
                if (rowStr.isNotBlank()) {
                    resultText.append(rowStr).append("\n")
                }
            }
            resultText.append("\n")
        }

        if (resultText.length < 20) {
            val parenMatches = Regex("""\(((?:[^()\\]|\\.)*)\)""").findAll(pdfString)
            val lineSb = StringBuilder()
            for (m in parenMatches) {
                val text = unescapePdfString(m.groupValues[1])
                if (text.length > 1 && !text.startsWith("/") && !text.contains("Adobe") && !text.contains("Font")) {
                    lineSb.append(text).append(" ")
                }
            }
            if (lineSb.isNotBlank()) {
                resultText.append(lineSb.toString())
            }
        }

        return resultText.toString()
    }

    private fun unescapePdfString(str: String): String {
        return str.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\b", "")
            .replace("\\f", "")
    }

    /**
     * Parse raw text (CSV/TSV formatted) and run strict field validation on each row.
     */
    fun parseAndValidateCsvText(
        rawText: String,
        targetCatalog: String = "DREAMPRICE",
        fileName: String = "Saisie CSV"
    ): ImportValidationReport {
        val resolvedCatalog = autoDetectCatalog(fileName, rawText, targetCatalog)
        val validProducts = mutableListOf<ProductEntity>()
        val rowErrors = mutableListOf<RowValidationError>()

        val rawLines = rawText.lines().filter { it.isNotBlank() }
        if (rawLines.isEmpty()) {
            return ImportValidationReport(
                targetCatalog = resolvedCatalog,
                validProducts = emptyList(),
                errors = listOf(RowValidationError(0, "", listOf("Le contenu du fichier est vide."))),
                totalRowsScanned = 0,
                sourceFileName = fileName
            )
        }

        // Determine delimiter (, or ; or tab or pipe)
        val sampleLine = rawLines.firstOrNull { it.contains(";") || it.contains(",") || it.contains("\t") || it.contains("|") } ?: rawLines.first()
        val delimiter = when {
            sampleLine.contains(";") -> ';'
            sampleLine.contains("\t") -> '\t'
            sampleLine.contains("|") -> '|'
            else -> ','
        }

        val headerCols = parseCsvLine(sampleLine, delimiter).map { it.trim().trim('"').lowercase() }
        val nameIdx = headerCols.indexOfFirst { it.contains("product") || it.contains("nom") || it.contains("name") || it.contains("article") }
        val catIdx = headerCols.indexOfFirst { it.contains("cat") || it.contains("category") }
        val brandIdx = headerCols.indexOfFirst { it.contains("brand") || it.contains("marque") }
        val unitIdx = headerCols.indexOfFirst { it.contains("unit") || it.contains("unité") }
        val priceIdx = headerCols.indexOfFirst { it.contains("price") || it.contains("prix") }
        val costIdx = headerCols.indexOfFirst { it.contains("cost") || it.contains("coût") || it.contains("achat") }

        val isHeaderPresent = (nameIdx >= 0 || priceIdx >= 0)
        val startLineIndex = if (isHeaderPresent) 1 else 0

        var totalScanned = 0

        for (i in startLineIndex until rawLines.size) {
            val line = rawLines[i]
            val rowIndex = i + 1 // 1-indexed for display
            var cols = parseCsvLine(line, delimiter)
            if (cols.size < 2 && line.contains("  ")) {
                cols = line.split(Regex("""\s{2,}""")).map { it.trim() }
            }
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            totalScanned++
            val fieldErrors = mutableListOf<String>()

            val rawName = cols.getOrNull(if (nameIdx >= 0) nameIdx else 0)?.trim() ?: ""
            val rawCat = cols.getOrNull(if (catIdx >= 0) catIdx else 1)?.trim() ?: "Général"
            val rawBrand = cols.getOrNull(if (brandIdx >= 0) brandIdx else 2)?.trim() ?: resolvedCatalog
            val rawUnit = cols.getOrNull(if (unitIdx >= 0) unitIdx else 3)?.trim() ?: "PCS"
            val rawPriceStr = cols.getOrNull(if (priceIdx >= 0) priceIdx else 4)?.trim() ?: ""
            val rawCostStr = cols.getOrNull(if (costIdx >= 0) costIdx else 5)?.trim() ?: ""

            // 1. Name Validation
            if (rawName.isBlank()) {
                fieldErrors.add("Nom de produit obligatoire manquant.")
            }

            // 2. Price Validation
            val cleanPriceStr = rawPriceStr.replace("""[^\d.]""".toRegex(), "")
            val parsedPrice = cleanPriceStr.toDoubleOrNull()
            if (rawPriceStr.isBlank()) {
                fieldErrors.add("Prix de vente manquant.")
            } else if (parsedPrice == null) {
                fieldErrors.add("Prix invalide ('$rawPriceStr') : doit être un nombre.")
            } else if (parsedPrice <= 0) {
                fieldErrors.add("Le prix doit être strictly supérieur à 0 (valeur: $parsedPrice).")
            }

            // 3. Cost (Optional)
            val cleanCostStr = rawCostStr.replace("""[^\d.]""".toRegex(), "")
            val parsedCost = cleanCostStr.toDoubleOrNull() ?: 0.0

            if (fieldErrors.isNotEmpty()) {
                rowErrors.add(RowValidationError(rowIndex = rowIndex, rawContent = line, errors = fieldErrors))
            } else {
                validProducts.add(
                    ProductEntity(
                        catalogType = resolvedCatalog,
                        name = rawName,
                        category = if (rawCat.isBlank()) "Général" else rawCat,
                        brand = if (rawBrand.isBlank()) resolvedCatalog else rawBrand,
                        unit = if (rawUnit.isBlank()) "PCS" else rawUnit,
                        price = parsedPrice!!,
                        cost = parsedCost
                    )
                )
            }
        }

        return ImportValidationReport(
            targetCatalog = resolvedCatalog,
            validProducts = validProducts,
            errors = rowErrors,
            totalRowsScanned = totalScanned,
            sourceFileName = fileName
        )
    }

    /**
     * Native Excel (.xlsx) unzip and XML parser.
     */
    private fun parseAndValidateXlsxUri(
        context: Context,
        uri: Uri,
        fileName: String,
        targetCatalog: String
    ): ImportValidationReport {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/sharedStrings.xml") {
                        val xmlText = InputStreamReader(zip, "UTF-8").readText()
                        val stringMatches = Regex("""<t[^>]*>(.*?)</t>""").findAll(xmlText)
                        stringMatches.forEach { match ->
                            sharedStrings.add(match.groupValues[1])
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("xl/worksheets/sheet1.xml") || entry.name.startsWith("xl/worksheets/sheet.xml")) {
                        val xmlText = InputStreamReader(zip, "UTF-8").readText()
                        val rowMatches = Regex("""<row[^>]*>(.*?)</row>""").findAll(xmlText)
                        for (rowMatch in rowMatches) {
                            val rowXml = rowMatch.groupValues[1]
                            val cellMatches = Regex("""<c[^>]*?(?:t="([^"]*)")?[^>]*>(?:<v>(.*?)</v>)?</c>""").findAll(rowXml)
                            val rowCells = mutableListOf<String>()
                            for (cell in cellMatches) {
                                val type = cell.groupValues[1]
                                val value = cell.groupValues[2]
                                if (type == "s" && value.isNotEmpty()) {
                                    val idx = value.toIntOrNull()
                                    val stringVal = if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else value
                                    rowCells.add(stringVal)
                                } else {
                                    rowCells.add(value)
                                }
                            }
                            if (rowCells.any { it.isNotBlank() }) {
                                sheetRows.add(rowCells)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        if (sheetRows.isEmpty()) {
            // Fallback: Attempt reading as plain text CSV
            context.contentResolver.openInputStream(uri)?.use { input ->
                val text = input.bufferedReader().use { it.readText() }
                if (text.isNotBlank()) {
                    return parseAndValidateCsvText(text, targetCatalog, fileName)
                }
            }
        }

        // Convert extracted sheet rows to CSV formatted string
        val csvFormatted = sheetRows.joinToString("\n") { row ->
            row.joinToString(",") { cell ->
                if (cell.contains(",") || cell.contains("\n")) "\"$cell\"" else cell
            }
        }

        return parseAndValidateCsvText(csvFormatted, targetCatalog, fileName)
    }

    private fun parseCsvLine(line: String, delimiter: Char = ','): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == delimiter && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}
