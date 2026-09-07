package com.apoorvdarshan.calorietracker.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.roundToLong

@Serializable
enum class NutritionDataSource(val badgeText: String) {
    @SerialName("personalVerified") PERSONAL_VERIFIED("Saved"),
    @SerialName("openFoodFacts") OPEN_FOOD_FACTS("Open Food Facts"),
    @SerialName("usdaFoodDataCentral") USDA_FOOD_DATA_CENTRAL("USDA"),
    @SerialName("aiEstimate") AI_ESTIMATE("AI Estimate"),
    @SerialName("manual") MANUAL("Manual"),
    @SerialName("mixed") MIXED("Mixed");

    companion object {
        fun defaultFor(source: FoodSource): NutritionDataSource = when (source) {
            FoodSource.BARCODE -> OPEN_FOOD_FACTS
            FoodSource.NUTRITION_LABEL, FoodSource.MANUAL -> MANUAL
            FoodSource.SNAP_FOOD, FoodSource.TEXT_INPUT -> AI_ESTIMATE
        }
    }
}

@Serializable
data class NutritionSourceMetadata(
    val providerItemID: String? = null,
    val barcode: String? = null,
    val matchedFoodName: String? = null,
    val matchedBrand: String? = null,
    val servingQuantity: Double? = null,
    val servingUnit: String? = null,
    val servingWeightGrams: Double? = null,
    val householdServingDescription: String? = null,
    val portionQuantity: Double? = null,
    val portionUnit: String? = null,
    val portionIsEstimated: Boolean = false,
    val matchConfidence: Double? = null,
    val databaseDescription: String? = null
) {
    val portionSummary: String?
        get() = when {
            portionQuantity != null && !portionUnit.isNullOrBlank() ->
                "${NutritionNumberFormatter.quantity(portionQuantity)} $portionUnit"
            servingQuantity != null && !servingUnit.isNullOrBlank() ->
                "${NutritionNumberFormatter.quantity(servingQuantity)} $servingUnit"
            servingWeightGrams != null && servingWeightGrams > 0 ->
                "${NutritionNumberFormatter.quantity(servingWeightGrams)} g"
            else -> null
        }
}

@Serializable
data class StructuredFoodItem(
    val name: String,
    val brand: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val branded: Boolean? = null,
    val variant: String? = null,
    val preparation: String? = null,
    val modifiers: List<String> = emptyList(),
    val portionIsEstimated: Boolean = false
) {
    val lookupQuery: String
        get() = listOfNotNull(brand, variant, preparation, name, modifiers.joinToString(" "))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}

object NutritionNumberFormatter {
    fun quantity(value: Double): String {
        if (kotlin.math.abs(value.roundToLong() - value) < 0.0001) {
            return value.roundToLong().toString()
        }
        return if (kotlin.math.abs(value) < 10) {
            String.format(Locale.US, "%.2f", value)
        } else {
            String.format(Locale.US, "%.1f", value)
        }.trimEnd('0').trimEnd('.')
    }
}
