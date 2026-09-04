package com.apoorvdarshan.calorietracker.services

import android.content.Context
import com.apoorvdarshan.calorietracker.data.KeyStore
import com.apoorvdarshan.calorietracker.data.PreferencesStore
import com.apoorvdarshan.calorietracker.models.FoodEntry
import com.apoorvdarshan.calorietracker.models.MealIngredient
import com.apoorvdarshan.calorietracker.models.NutritionDataSource
import com.apoorvdarshan.calorietracker.models.NutritionSourceMetadata
import com.apoorvdarshan.calorietracker.models.ServingUnitOption
import com.apoorvdarshan.calorietracker.models.StructuredFoodItem
import com.apoorvdarshan.calorietracker.models.SupplementalNutrient
import com.apoorvdarshan.calorietracker.services.ai.FoodAnalysis
import com.apoorvdarshan.calorietracker.services.ai.FoodAnalysisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

@Serializable
data class NutritionFacts(
    val calories: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val sugar: Double? = null,
    val addedSugar: Double? = null,
    val fiber: Double? = null,
    val saturatedFat: Double? = null,
    val monounsaturatedFat: Double? = null,
    val polyunsaturatedFat: Double? = null,
    val cholesterol: Double? = null,
    val caffeine: Double? = null,
    val supplementalNutrients: Map<String, Double> = emptyMap(),
    val sodium: Double? = null,
    val potassium: Double? = null,
    val transFat: Double? = null,
    val calcium: Double? = null,
    val iron: Double? = null,
    val magnesium: Double? = null,
    val zinc: Double? = null,
    val vitaminA: Double? = null,
    val vitaminC: Double? = null,
    val vitaminD: Double? = null,
    val vitaminB12: Double? = null,
    val vitaminE: Double? = null,
    val vitaminK: Double? = null,
    val folate: Double? = null,
    val omega3: Double? = null
) {
    val hasUsableNutrition: Boolean
        get() = calories != null || protein != null || carbs != null || fat != null

    fun scaled(factor: Double): NutritionFacts {
        fun s(value: Double?): Double? = value?.let { round(it * factor * 10.0) / 10.0 }
        return copy(
            calories = calories?.let { round(it * factor) },
            protein = s(protein),
            carbs = s(carbs),
            fat = s(fat),
            sugar = s(sugar),
            addedSugar = s(addedSugar),
            fiber = s(fiber),
            saturatedFat = s(saturatedFat),
            monounsaturatedFat = s(monounsaturatedFat),
            polyunsaturatedFat = s(polyunsaturatedFat),
            cholesterol = s(cholesterol),
            caffeine = s(caffeine),
            supplementalNutrients = supplementalNutrients.mapValues { (_, value) -> round(value * factor * 10.0) / 10.0 },
            sodium = s(sodium),
            potassium = s(potassium),
            transFat = s(transFat),
            calcium = s(calcium),
            iron = s(iron),
            magnesium = s(magnesium),
            zinc = s(zinc),
            vitaminA = s(vitaminA),
            vitaminC = s(vitaminC),
            vitaminD = s(vitaminD),
            vitaminB12 = s(vitaminB12),
            vitaminE = s(vitaminE),
            vitaminK = s(vitaminK),
            folate = s(folate),
            omega3 = s(omega3)
        )
    }

    companion object {
        fun from(entry: FoodEntry): NutritionFacts = NutritionFacts(
            calories = entry.calories.toDouble(),
            protein = entry.protein,
            carbs = entry.carbs,
            fat = entry.fat,
            sugar = entry.sugar,
            addedSugar = entry.addedSugar,
            fiber = entry.fiber,
            saturatedFat = entry.saturatedFat,
            monounsaturatedFat = entry.monounsaturatedFat,
            polyunsaturatedFat = entry.polyunsaturatedFat,
            cholesterol = entry.cholesterol,
            caffeine = entry.caffeine,
            supplementalNutrients = entry.supplementalNutrients,
            sodium = entry.sodium,
            potassium = entry.potassium,
            transFat = entry.transFat,
            calcium = entry.calcium,
            iron = entry.iron,
            magnesium = entry.magnesium,
            zinc = entry.zinc,
            vitaminA = entry.vitaminA,
            vitaminC = entry.vitaminC,
            vitaminD = entry.vitaminD,
            vitaminB12 = entry.vitaminB12,
            vitaminE = entry.vitaminE,
            vitaminK = entry.vitaminK,
            folate = entry.folate,
            omega3 = entry.omega3
        )

        fun from(analysis: FoodAnalysis): NutritionFacts = NutritionFacts(
            calories = analysis.calories.toDouble(),
            protein = analysis.protein,
            carbs = analysis.carbs,
            fat = analysis.fat,
            sugar = analysis.sugar,
            addedSugar = analysis.addedSugar,
            fiber = analysis.fiber,
            saturatedFat = analysis.saturatedFat,
            monounsaturatedFat = analysis.monounsaturatedFat,
            polyunsaturatedFat = analysis.polyunsaturatedFat,
            cholesterol = analysis.cholesterol,
            caffeine = analysis.caffeine,
            supplementalNutrients = analysis.supplementalNutrients,
            sodium = analysis.sodium,
            potassium = analysis.potassium,
            transFat = analysis.transFat,
            calcium = analysis.calcium,
            iron = analysis.iron,
            magnesium = analysis.magnesium,
            zinc = analysis.zinc,
            vitaminA = analysis.vitaminA,
            vitaminC = analysis.vitaminC,
            vitaminD = analysis.vitaminD,
            vitaminB12 = analysis.vitaminB12,
            vitaminE = analysis.vitaminE,
            vitaminK = analysis.vitaminK,
            folate = analysis.folate,
            omega3 = analysis.omega3
        )
    }
}

@Serializable
data class NutritionCandidate(
    val nutritionDataSource: NutritionDataSource,
    val providerID: String,
    val barcode: String? = null,
    val name: String,
    val brand: String? = null,
    val servingQuantity: Double? = null,
    val servingUnit: String? = null,
    val servingWeightGrams: Double? = null,
    val householdServingDescription: String? = null,
    val facts: NutritionFacts,
    val dataType: String? = null,
    val matchScore: Double = 0.0
) {
    val id: String get() = "${nutritionDataSource.name}:$providerID"
    val hasUsableNutrition: Boolean get() = facts.hasUsableNutrition

    fun withScore(score: Double): NutritionCandidate = copy(matchScore = score)

    fun scaledAnalysis(item: StructuredFoodItem): FoodAnalysis {
        val resolvedScale = NutritionPortionScaler.scaleFactor(item, this)
        val scale = resolvedScale ?: 1.0
        val scaled = facts.scaled(scale)
        val servingAmount = NutritionPortionScaler.displayServingAmount(item, this, scale)
        val option = NutritionPortionScaler.servingOption(item, this, servingAmount.amount)
        val metadata = NutritionSourceMetadata(
            providerItemID = providerID,
            barcode = barcode,
            matchedFoodName = name,
            matchedBrand = brand,
            servingQuantity = servingQuantity,
            servingUnit = servingUnit,
            servingWeightGrams = servingWeightGrams,
            householdServingDescription = householdServingDescription,
            portionQuantity = item.quantity,
            portionUnit = item.unit,
            portionIsEstimated = item.portionIsEstimated || resolvedScale == null,
            matchConfidence = matchScore,
            databaseDescription = dataType
        )
        return FoodAnalysis(
            name = listOfNotNull(brand, name).joinToString(" ").trim().ifEmpty { name },
            calories = (scaled.calories ?: 0.0).roundToInt(),
            protein = scaled.protein ?: 0.0,
            carbs = scaled.carbs ?: 0.0,
            fat = scaled.fat ?: 0.0,
            servingSizeGrams = servingAmount.amount,
            emoji = if (nutritionDataSource == NutritionDataSource.OPEN_FOOD_FACTS) "🏷️" else "🍽️",
            sugar = scaled.sugar,
            addedSugar = scaled.addedSugar,
            fiber = scaled.fiber,
            saturatedFat = scaled.saturatedFat,
            monounsaturatedFat = scaled.monounsaturatedFat,
            polyunsaturatedFat = scaled.polyunsaturatedFat,
            cholesterol = scaled.cholesterol,
            caffeine = scaled.caffeine,
            supplementalNutrients = scaled.supplementalNutrients,
            sodium = scaled.sodium,
            potassium = scaled.potassium,
            transFat = scaled.transFat,
            calcium = scaled.calcium,
            iron = scaled.iron,
            magnesium = scaled.magnesium,
            zinc = scaled.zinc,
            vitaminA = scaled.vitaminA,
            vitaminC = scaled.vitaminC,
            vitaminD = scaled.vitaminD,
            vitaminB12 = scaled.vitaminB12,
            vitaminE = scaled.vitaminE,
            vitaminK = scaled.vitaminK,
            folate = scaled.folate,
            omega3 = scaled.omega3,
            servingUnitOptions = option?.let(::listOf).orEmpty(),
            selectedServingUnit = option?.unit,
            selectedServingQuantity = item.quantity ?: 1.0,
            servingSizeIsKnown = servingAmount.isKnownMass,
            nutritionDataSource = nutritionDataSource,
            nutritionSourceMetadata = metadata
        )
    }
}

sealed interface NutritionLookupResolution {
    data class Resolved(val analysis: FoodAnalysis) : NutritionLookupResolution
    data class Ambiguous(
        val item: StructuredFoodItem,
        val candidates: List<NutritionCandidate>,
        val fallback: FoodAnalysis
    ) : NutritionLookupResolution
    data class NoMatch(val analysis: FoodAnalysis) : NutritionLookupResolution
}

interface NutritionLookupProvider {
    val source: NutritionDataSource
    suspend fun search(item: StructuredFoodItem): List<NutritionCandidate>
}

class NutritionLookupService(
    private val context: Context,
    private val prefs: PreferencesStore,
    private val keyStore: KeyStore,
    private val client: OkHttpClient = FoodAnalysisService.defaultClient
) {
    private val confirmedStore = NutritionConfirmedMappingStore(context)

    suspend fun resolve(
        fallback: FoodAnalysis,
        originalText: String? = null,
        previousEntries: List<FoodEntry> = emptyList()
    ): NutritionLookupResolution {
        if (!prefs.exactNutritionLookupEnabled.first()) {
            return NutritionLookupResolution.NoMatch(fallback.withNutritionSource(NutritionDataSource.AI_ESTIMATE))
        }
        val items = fallback.structuredItems.ifEmpty {
            listOf(StructuredFoodTextInterpreter.infer(fallback, originalText))
        }
        val analyses = mutableListOf<FoodAnalysis>()
        var anyDatabaseMatch = false
        var hasUnmappedUnmatchedComponent = false
        for (item in items) {
            confirmedStore.candidateFor(item)?.let { confirmed ->
                anyDatabaseMatch = true
                analyses += confirmed.scaledAnalysis(item).withNutritionSource(NutritionDataSource.PERSONAL_VERIFIED)
                return@let
            } ?: run {
                val ranked = rankedCandidates(item, previousEntries)
                val top = ranked.firstOrNull()
                val second = ranked.drop(1).firstOrNull()
                if (top != null &&
                    top.matchScore >= AUTO_MATCH_THRESHOLD &&
                    (second == null || top.matchScore - second.matchScore >= SEPARATION_THRESHOLD)
                ) {
                    anyDatabaseMatch = true
                    analyses += top.scaledAnalysis(item)
                } else if (ranked.size > 1 && (ranked.firstOrNull()?.matchScore ?: 0.0) >= AMBIGUOUS_THRESHOLD && items.size == 1) {
                    return NutritionLookupResolution.Ambiguous(
                        item = item,
                        candidates = ranked.take(5),
                        fallback = fallback.withNutritionSource(NutritionDataSource.AI_ESTIMATE)
                    )
                } else {
                    fallback.ingredients.firstOrNull { ingredient ->
                        NutritionLookupScorer.tokens(ingredient.name)
                            .intersect(NutritionLookupScorer.tokens(item.name))
                            .isNotEmpty()
                    }?.let {
                        analyses += foodAnalysisFromIngredient(it)
                    } ?: run {
                        hasUnmappedUnmatchedComponent = true
                    }
                }
            }
        }
        if (!anyDatabaseMatch || hasUnmappedUnmatchedComponent || analyses.isEmpty()) {
            return NutritionLookupResolution.NoMatch(fallback.withNutritionSource(NutritionDataSource.AI_ESTIMATE))
        }
        return NutritionLookupResolution.Resolved(
            if (analyses.size == 1) analyses.first() else combinedFoodAnalysis(
                name = fallback.name,
                emoji = fallback.emoji,
                analyses = analyses,
                fallbackNote = fallback.customNote
            )
        )
    }

    suspend fun searchDatabase(query: String, previousEntries: List<FoodEntry> = emptyList()): List<NutritionCandidate> =
        rankedCandidates(StructuredFoodTextInterpreter.parse(query), previousEntries)

    fun remember(candidate: NutritionCandidate, item: StructuredFoodItem) {
        confirmedStore.remember(candidate, item)
    }

    private suspend fun rankedCandidates(
        item: StructuredFoodItem,
        previousEntries: List<FoodEntry>
    ): List<NutritionCandidate> = coroutineScope {
        val allProviders = providers(previousEntries)
        val personalProviders = allProviders.filter { it.source == NutritionDataSource.PERSONAL_VERIFIED }
        val externalProviders = allProviders.filter { it.source != NutritionDataSource.PERSONAL_VERIFIED }
        val personalCandidates = personalProviders
            .map { provider ->
                async(Dispatchers.IO) { runCatching { provider.search(item) }.getOrDefault(emptyList()) }
            }
            .awaitAll()
            .flatten()
        val cacheKey = externalCacheKey(item, externalProviders)
        val externalCandidates = if (cacheKey != null) {
            val cached = NutritionLookupQueryCache.candidates(cacheKey)
            if (cached != null) {
                cached
            } else {
                val fetched = externalProviders
                    .map { provider ->
                        async(Dispatchers.IO) { runCatching { provider.search(item) }.getOrDefault(emptyList()) }
                    }
                    .awaitAll()
                    .flatten()
                NutritionLookupQueryCache.remember(fetched, cacheKey)
                fetched
            }
        } else {
            emptyList()
        }
        NutritionLookupScorer.ranked(item, personalCandidates + externalCandidates)
    }

    private fun externalCacheKey(item: StructuredFoodItem, providers: List<NutritionLookupProvider>): String? {
        if (providers.isEmpty()) return null
        val sources = providers.map { it.source.name }.sorted().joinToString(",")
        return "${NutritionLookupScorer.normalizedText(item.lookupQuery)}|$sources"
    }

    private suspend fun providers(previousEntries: List<FoodEntry>): List<NutritionLookupProvider> {
        val providers = mutableListOf<NutritionLookupProvider>(PersonalNutritionLookupProvider(previousEntries))
        keyStore.usdaFoodDataCentralApiKey()?.takeIf { it.isNotBlank() }?.let { apiKey ->
            providers += USDAFoodDataCentralProvider(apiKey, client)
        }
        if (prefs.openFoodFactsTextLookupEnabled.first()) {
            providers += OpenFoodFactsTextLookupProvider(client)
        }
        return providers
    }

    private companion object {
        const val AUTO_MATCH_THRESHOLD = 0.92
        const val SEPARATION_THRESHOLD = 0.06
        const val AMBIGUOUS_THRESHOLD = 0.72
    }
}

private object NutritionLookupQueryCache {
    private const val TTL_MILLIS = 6 * 60 * 60 * 1_000L
    private data class Entry(val candidates: List<NutritionCandidate>, val savedAtMillis: Long)

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    suspend fun candidates(key: String): List<NutritionCandidate>? = mutex.withLock {
        val entry = entries[key] ?: return@withLock null
        if (System.currentTimeMillis() - entry.savedAtMillis > TTL_MILLIS) {
            entries.remove(key)
            null
        } else {
            entry.candidates
        }
    }

    suspend fun remember(candidates: List<NutritionCandidate>, key: String) {
        mutex.withLock {
            entries[key] = Entry(candidates = candidates, savedAtMillis = System.currentTimeMillis())
        }
    }
}

private object NutritionProviderRateLimiter {
    private val mutex = Mutex()
    private val nextAllowedAtMillis = mutableMapOf<NutritionDataSource, Long>()

    suspend fun waitIfNeeded(source: NutritionDataSource, minimumIntervalMillis: Long) {
        val waitMillis = mutex.withLock {
            val now = System.currentTimeMillis()
            val next = nextAllowedAtMillis[source] ?: now
            val wait = maxOf(0L, next - now)
            nextAllowedAtMillis[source] = now + wait + minimumIntervalMillis
            wait
        }
        if (waitMillis > 0) delay(waitMillis)
    }
}

@Serializable
private data class NutritionConfirmedMapping(
    val normalizedQuery: String,
    val candidate: NutritionCandidate,
    val updatedAtEpochMillis: Long
)

private class NutritionConfirmedMappingStore(context: Context) {
    private val prefs = context.getSharedPreferences("fudai_confirmed_nutrition_mappings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), NutritionConfirmedMapping.serializer())

    fun candidateFor(item: StructuredFoodItem): NutritionCandidate? =
        mappings()[NutritionLookupScorer.normalizedText(item.lookupQuery)]?.candidate

    fun remember(candidate: NutritionCandidate, item: StructuredFoodItem) {
        val key = NutritionLookupScorer.normalizedText(item.lookupQuery)
        val values = mappings().toMutableMap()
        values[key] = NutritionConfirmedMapping(key, candidate, System.currentTimeMillis())
        prefs.edit().putString(STORAGE_KEY, json.encodeToString(serializer, values)).apply()
    }

    private fun mappings(): Map<String, NutritionConfirmedMapping> =
        prefs.getString(STORAGE_KEY, null)?.let { raw ->
            runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
        }.orEmpty()

    private companion object {
        const val STORAGE_KEY = "mappings"
    }
}

private class PersonalNutritionLookupProvider(private val entries: List<FoodEntry>) : NutritionLookupProvider {
    override val source: NutritionDataSource = NutritionDataSource.PERSONAL_VERIFIED

    override suspend fun search(item: StructuredFoodItem): List<NutritionCandidate> = entries.mapNotNull { entry ->
        if (entry.nutritionDataSource == NutritionDataSource.AI_ESTIMATE) return@mapNotNull null
        val score = NutritionLookupScorer.score(
            item = item,
            candidateName = entry.name,
            brand = entry.nutritionSourceMetadata?.matchedBrand
        )
        if (score < 0.72) return@mapNotNull null
        NutritionCandidate(
            nutritionDataSource = NutritionDataSource.PERSONAL_VERIFIED,
            providerID = entry.nutritionSourceMetadata?.providerItemID ?: entry.id.toString(),
            barcode = entry.nutritionSourceMetadata?.barcode,
            name = entry.nutritionSourceMetadata?.matchedFoodName ?: entry.name,
            brand = entry.nutritionSourceMetadata?.matchedBrand,
            servingQuantity = entry.selectedServingQuantity,
            servingUnit = entry.selectedServingUnit,
            servingWeightGrams = entry.servingSizeGrams,
            householdServingDescription = entry.nutritionSourceMetadata?.householdServingDescription,
            facts = NutritionFacts.from(entry),
            dataType = "Previously logged",
            matchScore = score
        )
    }
}

private object NutritionPortionScaler {
    data class ServingAmount(val amount: Double, val isKnownMass: Boolean)

    fun scaleFactor(item: StructuredFoodItem, candidate: NutritionCandidate): Double? {
        val quantity = item.quantity?.takeIf { it > 0 } ?: return 1.0
        val itemUnit = item.unit ?: candidate.servingUnit ?: "serving"
        val servingQuantity = candidate.servingQuantity?.takeIf { it > 0 } ?: 1.0
        val servingUnit = candidate.servingUnit ?: "serving"
        if (UnitNormalizer.sameUnit(itemUnit, servingUnit)) return quantity / servingQuantity
        val consumedGrams = UnitNormalizer.massInGrams(quantity, itemUnit)
        val servingGrams = candidate.servingWeightGrams
        if (consumedGrams != null && servingGrams != null && servingGrams > 0) {
            return consumedGrams / servingGrams
        }
        val consumedMl = UnitNormalizer.volumeInMilliliters(quantity, itemUnit)
        val servingMl = UnitNormalizer.volumeInMilliliters(servingQuantity, servingUnit)
        if (consumedMl != null && servingMl != null && servingMl > 0) {
            return consumedMl / servingMl
        }
        return if (UnitNormalizer.sameCountUnit(itemUnit, servingUnit)) quantity / servingQuantity else null
    }

    fun displayServingAmount(item: StructuredFoodItem, candidate: NutritionCandidate, scale: Double): ServingAmount {
        val quantity = item.quantity
        val unit = item.unit
        if (quantity != null && unit != null) {
            UnitNormalizer.massInGrams(quantity, unit)?.let { return ServingAmount(it, true) }
        }
        candidate.servingWeightGrams?.takeIf { it > 0 }?.let { return ServingAmount(it * scale, true) }
        return ServingAmount(maxOf(quantity ?: scale, 1.0), false)
    }

    fun servingOption(
        item: StructuredFoodItem,
        candidate: NutritionCandidate,
        scaledServingAmount: Double
    ): ServingUnitOption? {
        val itemQuantity = item.quantity
        val itemUnit = item.unit
        if (itemQuantity != null && itemQuantity > 0 && itemUnit != null) {
            return ServingUnitOption(unit = itemUnit, gramsPerUnit = scaledServingAmount / itemQuantity, quantity = itemQuantity)
        }
        val quantity = candidate.servingQuantity
        val unit = candidate.servingUnit
        val grams = candidate.servingWeightGrams
        if (quantity != null && quantity > 0 && unit != null && grams != null && grams > 0) {
            return ServingUnitOption(unit = unit, gramsPerUnit = grams / quantity, quantity = quantity)
        }
        return null
    }
}

internal object UnitNormalizer {
    fun normalized(unit: String): String {
        val lower = unit.trim().lowercase(Locale.US).replace(".", "")
        return when (lower) {
            "gram", "grams" -> "g"
            "kilogram", "kilograms", "kgs" -> "kg"
            "milligram", "milligrams" -> "mg"
            "ounce", "ounces", "ozs" -> "oz"
            "pound", "pounds", "lbs" -> "lb"
            "milliliter", "milliliters", "millilitre", "millilitres" -> "ml"
            "liter", "liters", "litre", "litres" -> "l"
            "fluid ounce", "fluid ounces", "fl ounce", "fl ounces" -> "fl oz"
            "tortillas" -> "tortilla"
            "pieces" -> "piece"
            "slices" -> "slice"
            "servings" -> "serving"
            else -> if (lower.endsWith("s") && lower.length > 3) lower.dropLast(1) else lower
        }
    }

    fun sameUnit(lhs: String, rhs: String): Boolean = normalized(lhs) == normalized(rhs)

    fun sameCountUnit(lhs: String, rhs: String): Boolean {
        val countUnits = setOf("serving", "piece", "slice", "tortilla", "egg", "strip", "bar", "packet", "can", "cup")
        val left = normalized(lhs)
        val right = normalized(rhs)
        return left == right && left in countUnits
    }

    fun massInGrams(quantity: Double, unit: String): Double? = when (normalized(unit)) {
        "g" -> quantity
        "kg" -> quantity * 1_000.0
        "mg" -> quantity / 1_000.0
        "oz" -> quantity * 28.349523125
        "lb" -> quantity * 453.59237
        else -> null
    }

    fun volumeInMilliliters(quantity: Double, unit: String): Double? = when (normalized(unit)) {
        "ml" -> quantity
        "l" -> quantity * 1_000.0
        "fl oz" -> quantity * 29.5735295625
        "cup" -> quantity * 236.5882365
        "tbsp" -> quantity * 14.78676478125
        "tsp" -> quantity * 4.92892159375
        else -> null
    }
}

object NutritionLookupScorer {
    private val stopwords = setOf("a", "an", "and", "the", "of", "with", "food", "foods")
    private val importantModifiers = setOf(
        "fat", "free", "skim", "whole", "wheat", "flour", "vanilla", "original",
        "cooked", "raw", "lean", "carb", "balance", "skinless", "boneless", "low",
        "reduced", "sugar", "unsweetened", "sweetened", "organic"
    )

    fun ranked(item: StructuredFoodItem, candidates: List<NutritionCandidate>): List<NutritionCandidate> =
        candidates
            .filter { it.hasUsableNutrition }
            .map { it.withScore(score(item, it.name, it.brand)) }
            .sortedWith(
                compareByDescending<NutritionCandidate> { it.matchScore }
                    .thenBy { sourcePriority(it.nutritionDataSource) }
            )

    fun score(item: StructuredFoodItem, candidateName: String, brand: String?): Double {
        val query = item.lookupQuery
        val queryTokens = tokens(query)
        val candidateText = listOfNotNull(brand, candidateName).joinToString(" ")
        val candidateTokens = tokens(candidateText)
        if (queryTokens.isEmpty() || candidateTokens.isEmpty()) return 0.0
        val overlap = queryTokens.intersect(candidateTokens).size.toDouble() / queryTokens.size.toDouble()
        val brandScore = item.brand?.takeIf { it.isNotBlank() }?.let { queryBrand ->
            if (normalizedText(queryBrand) == normalizedText(brand.orEmpty())) 0.24 else -0.18
        } ?: 0.04
        val exactBonus = if (normalizedText(candidateText).contains(normalizedText(query))) 0.12 else 0.0
        val modifierPenalty = queryTokens.intersect(importantModifiers).subtract(candidateTokens).size * 0.12
        val conflictPenalty = conflictPenalty(normalizedText(query), normalizedText(candidateText))
        return (0.55 * overlap + brandScore + exactBonus + 0.12 - modifierPenalty - conflictPenalty)
            .coerceIn(0.0, 0.99)
    }

    fun tokens(text: String): Set<String> =
        normalizedText(text).split(" ").filter { it.isNotBlank() && it !in stopwords }.toSet()

    fun normalizedText(text: String): String =
        text.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9%]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun conflictPenalty(query: String, candidate: String): Double {
        var penalty = 0.0
        if ((query.contains("fat free") || query.contains("skim")) && candidate.contains("whole")) penalty += 0.35
        if (query.contains("whole") && candidate.contains("fat free")) penalty += 0.30
        if (query.contains("whole wheat") && candidate.contains("flour") && !candidate.contains("whole wheat")) penalty += 0.22
        if (query.contains("cooked") && candidate.contains("raw")) penalty += 0.28
        if (query.contains("raw") && candidate.contains("cooked")) penalty += 0.28
        return penalty
    }

    private fun sourcePriority(source: NutritionDataSource): Int = when (source) {
        NutritionDataSource.PERSONAL_VERIFIED -> 0
        NutritionDataSource.USDA_FOOD_DATA_CENTRAL -> 1
        NutritionDataSource.OPEN_FOOD_FACTS -> 2
        NutritionDataSource.MANUAL -> 3
        NutritionDataSource.MIXED -> 4
        NutritionDataSource.AI_ESTIMATE -> 5
    }
}

object StructuredFoodTextInterpreter {
    fun infer(analysis: FoodAnalysis, originalText: String?): StructuredFoodItem {
        val parsed = parse(originalText?.takeIf { it.isNotBlank() } ?: analysis.name)
        return parsed.copy(
            name = parsed.name.ifBlank { analysis.name },
            quantity = parsed.quantity ?: analysis.selectedServingQuantity,
            unit = parsed.unit ?: analysis.selectedServingUnit,
            portionIsEstimated = !analysis.servingSizeIsKnown
        )
    }

    fun parse(text: String): StructuredFoodItem {
        val cleaned = text.trim()
        val match = Regex("""^\s*([0-9]+(?:[.,][0-9]+)?)\s*([a-zA-Z][a-zA-Z ]{0,14})?\s+(.+)$""")
            .find(cleaned)
        val quantity = match?.groupValues?.getOrNull(1)?.replace(",", ".")?.toDoubleOrNull()
        val unit = match?.groupValues?.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
        val remaining = match?.groupValues?.getOrNull(3) ?: cleaned
        val tokens = remaining.split(Regex("\\s+")).filter { it.isNotBlank() }
        val knownBrands = setOf("lactaid", "mission", "cheerios", "kirkland", "chobani", "fairlife")
        val brand = tokens.firstOrNull()?.takeIf { it.lowercase(Locale.US) in knownBrands }
        val nameTokens = if (brand == null) tokens else tokens.drop(1)
        val normalized = NutritionLookupScorer.normalizedText(remaining)
        val modifiers = listOf("fat free", "whole wheat", "whole", "skim", "vanilla", "original", "cooked", "raw", "boneless", "skinless", "carb balance")
            .filter { normalized.contains(it) }
        return StructuredFoodItem(
            name = nameTokens.joinToString(" ").ifBlank { remaining },
            brand = brand,
            quantity = quantity,
            unit = unit,
            branded = brand != null,
            preparation = when {
                normalized.contains("cooked") -> "cooked"
                normalized.contains("raw") -> "raw"
                else -> null
            },
            modifiers = modifiers
        )
    }
}

private class USDAFoodDataCentralProvider(
    private val apiKey: String,
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl = "https://api.nal.usda.gov/fdc/v1/".toHttpUrl()
) : NutritionLookupProvider {
    override val source: NutritionDataSource = NutritionDataSource.USDA_FOOD_DATA_CENTRAL

    override suspend fun search(item: StructuredFoodItem): List<NutritionCandidate> = withContext(Dispatchers.IO) {
        NutritionProviderRateLimiter.waitIfNeeded(source, minimumIntervalMillis = 250L)
        val url = baseUrl.newBuilder()
            .addPathSegments("foods/search")
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("query", item.lookupQuery)
            .addQueryParameter("pageSize", "25")
            .build()
        val request = Request.Builder().url(url).addHeader("Accept", "application/json").build()
        val raw = execute(request)
        val root = Json.parseToJsonElement(raw) as? JsonObject ?: return@withContext emptyList()
        val foods = root["foods"] as? JsonArray ?: return@withContext emptyList()
        foods.take(8).mapNotNull { element ->
            val food = element as? JsonObject ?: return@mapNotNull null
            val fdcId = food.int("fdcId") ?: return@mapNotNull null
            detailsCandidate(fdcId, food) ?: searchCandidate(fdcId, food)
        }
    }

    private fun searchCandidate(fdcId: Int, food: JsonObject): NutritionCandidate? {
        val description = food.string("description") ?: return null
        val facts = USDANutrientMapper.fromSearchNutrients(food["foodNutrients"] as? JsonArray)
            ?: return null
        return NutritionCandidate(
            nutritionDataSource = NutritionDataSource.USDA_FOOD_DATA_CENTRAL,
            providerID = fdcId.toString(),
            name = description,
            brand = food.string("brandName") ?: food.string("brandOwner"),
            servingQuantity = 100.0,
            servingUnit = "g",
            servingWeightGrams = 100.0,
            facts = facts,
            dataType = food.string("dataType")
        )
    }

    private fun detailsCandidate(fdcId: Int, fallback: JsonObject): NutritionCandidate? {
        val url = baseUrl.newBuilder()
            .addPathSegments("food/$fdcId")
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("format", "full")
            .build()
        val request = Request.Builder().url(url).addHeader("Accept", "application/json").build()
        val raw = runCatching { execute(request) }.getOrNull() ?: return null
        val food = Json.parseToJsonElement(raw) as? JsonObject ?: return null
        val labelFacts = USDANutrientMapper.fromLabelNutrients(food["labelNutrients"] as? JsonObject)
        val facts = labelFacts ?: USDANutrientMapper.fromFoodNutrients(food["foodNutrients"] as? JsonArray)
            ?: return null
        val servingUnit = food.string("servingSizeUnit") ?: fallback.string("servingSizeUnit") ?: "g"
        val servingQuantity = food.double("servingSize") ?: fallback.double("servingSize") ?: 100.0
        val servingWeight = UnitNormalizer.massInGrams(servingQuantity, servingUnit)
            ?: (food["foodPortions"] as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.double("gramWeight") }
                ?.firstOrNull()
            ?: if (servingUnit.equals("g", ignoreCase = true)) servingQuantity else null
        val servingFacts = if (labelFacts == null && abs((servingWeight ?: 100.0) - 100.0) > 0.001) {
            facts.scaled((servingWeight ?: 100.0) / 100.0)
        } else {
            facts
        }
        return NutritionCandidate(
            nutritionDataSource = NutritionDataSource.USDA_FOOD_DATA_CENTRAL,
            providerID = fdcId.toString(),
            name = food.string("description") ?: fallback.string("description") ?: return null,
            brand = food.string("brandName") ?: food.string("brandOwner") ?: fallback.string("brandName") ?: fallback.string("brandOwner"),
            servingQuantity = if (labelFacts == null) (servingWeight ?: 100.0) else servingQuantity,
            servingUnit = if (labelFacts == null) "g" else servingUnit,
            servingWeightGrams = servingWeight ?: 100.0,
            householdServingDescription = food.string("householdServingFullText"),
            facts = servingFacts,
            dataType = food.string("dataType") ?: fallback.string("dataType")
        )
    }

    private fun execute(request: Request): String = try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    } catch (error: IOException) {
        throw error
    }
}

internal object USDANutrientMapper {
    fun fromLabelNutrients(label: JsonObject?): NutritionFacts? {
        if (label == null) return null
        val facts = NutritionFacts(
            calories = label.labelValue("calories"),
            protein = label.labelValue("protein"),
            carbs = label.labelValue("carbohydrates"),
            fat = label.labelValue("fat"),
            sugar = label.labelValue("sugars"),
            addedSugar = label.labelValue("addedSugars"),
            fiber = label.labelValue("fiber"),
            saturatedFat = label.labelValue("saturatedFat"),
            cholesterol = label.labelValue("cholesterol"),
            sodium = label.labelValue("sodium"),
            potassium = label.labelValue("potassium"),
            transFat = label.labelValue("transFat"),
            calcium = label.labelValue("calcium"),
            iron = label.labelValue("iron")
        )
        return facts.takeIf { it.hasUsableNutrition }
    }

    fun fromSearchNutrients(nutrients: JsonArray?): NutritionFacts? {
        var builder = Builder()
        nutrients.orEmpty().forEach { element ->
            val item = element as? JsonObject ?: return@forEach
            builder = builder.apply(
                id = item.int("nutrientId"),
                name = item.string("nutrientName"),
                amount = item.double("value"),
                unit = item.string("unitName")
            )
        }
        return builder.facts.takeIf { it.hasUsableNutrition }
    }

    fun fromFoodNutrients(nutrients: JsonArray?): NutritionFacts? {
        var builder = Builder()
        nutrients.orEmpty().forEach { element ->
            val item = element as? JsonObject ?: return@forEach
            val nutrient = item["nutrient"] as? JsonObject
            builder = builder.apply(
                id = nutrient?.int("id") ?: item.int("nutrientId"),
                name = nutrient?.string("name") ?: item.string("nutrientName"),
                amount = item.double("amount") ?: item.double("value"),
                unit = nutrient?.string("unitName") ?: item.string("unitName")
            )
        }
        return builder.facts.takeIf { it.hasUsableNutrition }
    }

    private data class Builder(val facts: NutritionFacts = NutritionFacts()) {
        fun apply(id: Int?, name: String?, amount: Double?, unit: String?): Builder {
            if (amount == null) return this
            val next = when (id) {
                1008 -> facts.copy(calories = amount)
                1062 -> if (unit.equals("kj", ignoreCase = true)) facts.copy(calories = amount * 0.23900573614) else facts
                1003 -> facts.copy(protein = grams(amount, unit))
                1005 -> facts.copy(carbs = grams(amount, unit))
                1004 -> facts.copy(fat = grams(amount, unit))
                2000 -> facts.copy(sugar = grams(amount, unit))
                1235 -> facts.copy(addedSugar = grams(amount, unit))
                1079 -> facts.copy(fiber = grams(amount, unit))
                1258 -> facts.copy(saturatedFat = grams(amount, unit))
                1292 -> facts.copy(monounsaturatedFat = grams(amount, unit))
                1293 -> facts.copy(polyunsaturatedFat = grams(amount, unit))
                1253 -> facts.copy(cholesterol = milligrams(amount, unit))
                1057 -> facts.copy(caffeine = milligrams(amount, unit))
                1093 -> facts.copy(sodium = milligrams(amount, unit))
                1092 -> facts.copy(potassium = milligrams(amount, unit))
                1257, 1259 -> facts.copy(transFat = grams(amount, unit))
                1087 -> facts.copy(calcium = milligrams(amount, unit))
                1089 -> facts.copy(iron = milligrams(amount, unit))
                1090 -> facts.copy(magnesium = milligrams(amount, unit))
                1095 -> facts.copy(zinc = milligrams(amount, unit))
                1106 -> facts.copy(vitaminA = micrograms(amount, unit))
                1162 -> facts.copy(vitaminC = milligrams(amount, unit))
                1114 -> facts.copy(vitaminD = micrograms(amount, unit))
                1178 -> facts.copy(vitaminB12 = micrograms(amount, unit))
                1109 -> facts.copy(vitaminE = milligrams(amount, unit))
                1185 -> facts.copy(vitaminK = micrograms(amount, unit))
                1177 -> facts.copy(folate = micrograms(amount, unit))
                1272, 1404 -> facts.copy(omega3 = grams(amount, unit))
                else -> applyByName(name, amount, unit)
            }
            return copy(facts = next)
        }

        private fun applyByName(name: String?, amount: Double, unit: String?): NutritionFacts {
            val normalized = NutritionLookupScorer.normalizedText(name.orEmpty())
            return when {
                normalized.contains("energy") && normalized.contains("kcal") -> facts.copy(calories = amount)
                normalized == "protein" -> facts.copy(protein = grams(amount, unit))
                normalized.contains("carbohydrate") -> facts.copy(carbs = grams(amount, unit))
                normalized.contains("total lipid") || normalized == "fat" -> facts.copy(fat = grams(amount, unit))
                else -> facts
            }
        }

        private fun grams(amount: Double, unit: String?): Double = when (unit?.lowercase(Locale.US)) {
            "mg" -> amount / 1_000.0
            "ug", "mcg" -> amount / 1_000_000.0
            else -> amount
        }

        private fun milligrams(amount: Double, unit: String?): Double = when (unit?.lowercase(Locale.US)) {
            "g" -> amount * 1_000.0
            "ug", "mcg" -> amount / 1_000.0
            else -> amount
        }

        private fun micrograms(amount: Double, unit: String?): Double = when (unit?.lowercase(Locale.US)) {
            "g" -> amount * 1_000_000.0
            "mg" -> amount * 1_000.0
            else -> amount
        }
    }
}

private class OpenFoodFactsTextLookupProvider(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl = "https://world.openfoodfacts.org/".toHttpUrl()
) : NutritionLookupProvider {
    override val source: NutritionDataSource = NutritionDataSource.OPEN_FOOD_FACTS

    override suspend fun search(item: StructuredFoodItem): List<NutritionCandidate> = withContext(Dispatchers.IO) {
        NutritionProviderRateLimiter.waitIfNeeded(source, minimumIntervalMillis = 1_000L)
        val url = baseUrl.newBuilder()
            .addPathSegments("cgi/search.pl")
            .addQueryParameter("search_terms", item.lookupQuery)
            .addQueryParameter("search_simple", "1")
            .addQueryParameter("action", "process")
            .addQueryParameter("json", "1")
            .addQueryParameter("page_size", "10")
            .addQueryParameter("fields", "code,product_name,generic_name,brands,serving_size,serving_quantity,nutriments")
            .build()
        val request = Request.Builder().url(url).addHeader("Accept", "application/json").build()
        val raw = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        val root = Json.parseToJsonElement(raw) as? JsonObject ?: return@withContext emptyList()
        val products = root["products"] as? JsonArray ?: return@withContext emptyList()
        products.mapNotNull { element ->
            (element as? JsonObject)?.toOpenFoodFactsCandidate()
        }
    }

    private fun JsonObject.toOpenFoodFactsCandidate(): NutritionCandidate? {
        val nutriments = this["nutriments"] as? JsonObject ?: return null
        val servingGrams = maxOf(
            double("serving_quantity") ?: gramsFrom(string("serving_size")) ?: 100.0,
            1.0
        )
        val scale = servingGrams / 100.0
        fun value(key: String): Double? =
            nutriments.double("${key}_serving") ?: nutriments.double("${key}_100g")?.let { it * scale }
        val facts = NutritionFacts(
            calories = value("energy-kcal") ?: value("energy")?.let { it * 0.23900573614 },
            protein = value("proteins"),
            carbs = value("carbohydrates"),
            fat = value("fat"),
            sugar = value("sugars"),
            addedSugar = value("added-sugars"),
            fiber = value("fiber"),
            saturatedFat = value("saturated-fat"),
            monounsaturatedFat = value("monounsaturated-fat"),
            polyunsaturatedFat = value("polyunsaturated-fat"),
            cholesterol = value("cholesterol")?.let { it * 1_000.0 },
            caffeine = value("caffeine")?.let { it * 1_000.0 },
            supplementalNutrients = SupplementalNutrient.values().mapNotNull { nutrient ->
                value(nutrient.apiKey.replace('_', '-'))?.let { nutrient.storageKey to it }
            }.toMap(),
            sodium = value("sodium")?.let { it * 1_000.0 },
            potassium = value("potassium")?.let { it * 1_000.0 },
            transFat = value("trans-fat"),
            calcium = value("calcium")?.let { it * 1_000.0 },
            iron = value("iron")?.let { it * 1_000.0 },
            magnesium = value("magnesium")?.let { it * 1_000.0 },
            zinc = value("zinc")?.let { it * 1_000.0 },
            vitaminA = value("vitamin-a")?.let { it * 1_000_000.0 },
            vitaminC = value("vitamin-c")?.let { it * 1_000.0 },
            vitaminD = value("vitamin-d")?.let { it * 1_000_000.0 },
            vitaminB12 = value("vitamin-b12")?.let { it * 1_000_000.0 },
            vitaminE = value("vitamin-e")?.let { it * 1_000.0 },
            vitaminK = value("vitamin-k")?.let { it * 1_000_000.0 },
            folate = value("folates")?.let { it * 1_000_000.0 },
            omega3 = value("omega-3-fat")
        )
        if (!facts.hasUsableNutrition) return null
        val brand = string("brands")?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        return NutritionCandidate(
            nutritionDataSource = NutritionDataSource.OPEN_FOOD_FACTS,
            providerID = string("code") ?: java.util.UUID.randomUUID().toString(),
            barcode = string("code"),
            name = string("product_name") ?: string("generic_name") ?: "Open Food Facts product",
            brand = brand,
            servingQuantity = 1.0,
            servingUnit = "serving",
            servingWeightGrams = servingGrams,
            householdServingDescription = string("serving_size"),
            facts = facts,
            dataType = "Open Food Facts"
        )
    }

    private fun gramsFrom(servingSize: String?): Double? {
        val text = servingSize?.lowercase(Locale.US)?.replace(",", ".")?.replace("fl. oz", "fl oz") ?: return null
        val match = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(fl oz|kg|mg|g|oz|ml|l)""").find(text) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        return when (match.groupValues[2]) {
            "kg" -> value * 1_000.0
            "mg" -> value / 1_000.0
            "oz" -> value * 28.3495
            "fl oz" -> value * 29.5735
            "ml" -> value
            "l" -> value * 1_000.0
            else -> value
        }
    }
}

private fun foodAnalysisFromIngredient(ingredient: MealIngredient): FoodAnalysis = FoodAnalysis(
    name = ingredient.name,
    calories = ingredient.calories,
    protein = ingredient.protein,
    carbs = ingredient.carbs,
    fat = ingredient.fat,
    servingSizeGrams = ingredient.grams,
    nutritionDataSource = ingredient.nutritionDataSource ?: NutritionDataSource.AI_ESTIMATE,
    nutritionSourceMetadata = ingredient.nutritionSourceMetadata
)

private fun combinedFoodAnalysis(
    name: String,
    emoji: String?,
    analyses: List<FoodAnalysis>,
    fallbackNote: String?
): FoodAnalysis {
    fun sum(values: List<Double?>): Double? = values.filterNotNull().takeIf { it.isNotEmpty() }
        ?.sumOf { it }
        ?.let { round(it * 10.0) / 10.0 }
    val sources = analyses.map { it.nutritionDataSource }.toSet()
    return FoodAnalysis(
        name = name,
        calories = analyses.sumOf { it.calories },
        protein = analyses.sumOf { it.protein },
        carbs = analyses.sumOf { it.carbs },
        fat = analyses.sumOf { it.fat },
        servingSizeGrams = analyses.sumOf { it.servingSizeGrams },
        emoji = emoji,
        sugar = sum(analyses.map { it.sugar }),
        addedSugar = sum(analyses.map { it.addedSugar }),
        fiber = sum(analyses.map { it.fiber }),
        saturatedFat = sum(analyses.map { it.saturatedFat }),
        monounsaturatedFat = sum(analyses.map { it.monounsaturatedFat }),
        polyunsaturatedFat = sum(analyses.map { it.polyunsaturatedFat }),
        cholesterol = sum(analyses.map { it.cholesterol }),
        caffeine = sum(analyses.map { it.caffeine }),
        supplementalNutrients = SupplementalNutrient.values().mapNotNull { nutrient ->
            analyses.sumOf { it.supplementalNutrients[nutrient.storageKey] ?: 0.0 }
                .takeIf { it > 0 }
                ?.let { nutrient.storageKey to it }
        }.toMap(),
        sodium = sum(analyses.map { it.sodium }),
        potassium = sum(analyses.map { it.potassium }),
        transFat = sum(analyses.map { it.transFat }),
        calcium = sum(analyses.map { it.calcium }),
        iron = sum(analyses.map { it.iron }),
        magnesium = sum(analyses.map { it.magnesium }),
        zinc = sum(analyses.map { it.zinc }),
        vitaminA = sum(analyses.map { it.vitaminA }),
        vitaminC = sum(analyses.map { it.vitaminC }),
        vitaminD = sum(analyses.map { it.vitaminD }),
        vitaminB12 = sum(analyses.map { it.vitaminB12 }),
        vitaminE = sum(analyses.map { it.vitaminE }),
        vitaminK = sum(analyses.map { it.vitaminK }),
        folate = sum(analyses.map { it.folate }),
        omega3 = sum(analyses.map { it.omega3 }),
        customNote = fallbackNote,
        ingredients = analyses.map { analysis ->
            MealIngredient(
                name = analysis.name,
                grams = analysis.servingSizeGrams,
                calories = analysis.calories,
                protein = analysis.protein,
                carbs = analysis.carbs,
                fat = analysis.fat,
                nutritionDataSource = analysis.nutritionDataSource,
                nutritionSourceMetadata = analysis.nutritionSourceMetadata
            )
        },
        nutritionDataSource = if (sources.size == 1) sources.first() else NutritionDataSource.MIXED
    )
}

private fun FoodAnalysis.withNutritionSource(
    source: NutritionDataSource,
    metadata: NutritionSourceMetadata? = null
): FoodAnalysis = copy(
    nutritionDataSource = source,
    nutritionSourceMetadata = metadata ?: nutritionSourceMetadata
)

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

private fun JsonObject.int(key: String): Int? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.intOrNull ?: primitive.contentOrNull?.trim()?.toIntOrNull()
}

private fun JsonObject.double(key: String): Double? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return (primitive.doubleOrNull ?: primitive.contentOrNull?.trim()?.replace(",", ".")?.toDoubleOrNull())
        ?.takeUnless { it.isNaN() || it.isInfinite() }
}

private fun JsonObject.labelValue(key: String): Double? {
    val element = this[key] ?: return null
    return when (element) {
        is JsonObject -> element.double("value")
        is JsonPrimitive -> element.doubleOrNull ?: element.contentOrNull?.trim()?.replace(",", ".")?.toDoubleOrNull()
        else -> null
    }?.takeUnless { it.isNaN() || it.isInfinite() }
}
