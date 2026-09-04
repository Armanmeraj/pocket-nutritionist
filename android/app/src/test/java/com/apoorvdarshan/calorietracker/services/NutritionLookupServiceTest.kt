package com.apoorvdarshan.calorietracker.services

import com.apoorvdarshan.calorietracker.models.FoodEntry
import com.apoorvdarshan.calorietracker.models.FoodSource
import com.apoorvdarshan.calorietracker.models.NutritionDataSource
import com.apoorvdarshan.calorietracker.models.NutritionSourceMetadata
import com.apoorvdarshan.calorietracker.models.StructuredFoodItem
import com.apoorvdarshan.calorietracker.services.ai.FoodJsonParser
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionLookupServiceTest {
    @Test
    fun structuredFoodItemsParseFromAiResponse() {
        val analysis = FoodJsonParser.parseFood(
            """
            {
              "name": "Milk",
              "calories": 120,
              "protein": 8,
              "carbs": 12,
              "fat": 5,
              "serving_size_grams": 244,
              "structured_items": [
                {
                  "name": "milk",
                  "brand": "Fairlife",
                  "quantity": 1.5,
                  "unit": "cup",
                  "branded": true,
                  "variant": "fat free",
                  "preparation": "chilled",
                  "modifiers": ["high protein"],
                  "portion_estimated": true
                },
                { "name": "" }
              ]
            }
            """
        )

        assertEquals(NutritionDataSource.AI_ESTIMATE, analysis.nutritionDataSource)
        assertEquals(1, analysis.structuredItems.size)
        val item = analysis.structuredItems.single()
        assertEquals("Fairlife fat free chilled milk high protein", item.lookupQuery)
        assertEquals(1.5, item.quantity!!, 0.0)
        assertEquals("cup", item.unit)
        assertEquals(true, item.branded)
        assertTrue(item.portionIsEstimated)
    }

    @Test
    fun scorerRanksExactBrandAndModifierMatchFirst() {
        val item = StructuredFoodItem(
            name = "milk",
            brand = "Fairlife",
            quantity = 1.0,
            unit = "cup",
            variant = "fat free",
            modifiers = listOf("fat free")
        )
        val exact = candidate(
            id = "exact",
            source = NutritionDataSource.OPEN_FOOD_FACTS,
            name = "Fat Free Milk",
            brand = "Fairlife"
        )
        val conflicting = candidate(
            id = "conflict",
            source = NutritionDataSource.USDA_FOOD_DATA_CENTRAL,
            name = "Whole Milk",
            brand = "Acme"
        )

        val ranked = NutritionLookupScorer.ranked(item, listOf(conflicting, exact))

        assertEquals("exact", ranked.first().providerID)
        assertTrue(ranked.first().matchScore > ranked.last().matchScore)
    }

    @Test
    fun scorerPrefersUSDATieForGenericFoods() {
        val item = StructuredFoodItem(name = "rice", quantity = 100.0, unit = "g")
        val openFoodFacts = candidate(id = "off", source = NutritionDataSource.OPEN_FOOD_FACTS, name = "Rice")
        val usda = candidate(id = "usda", source = NutritionDataSource.USDA_FOOD_DATA_CENTRAL, name = "Rice")

        val ranked = NutritionLookupScorer.ranked(item, listOf(openFoodFacts, usda))

        assertEquals("usda", ranked.first().providerID)
    }

    @Test
    fun candidateScalingUsesRequestedMassAndPreservesSourceMetadata() {
        val source = candidate(
            id = "12345",
            source = NutritionDataSource.USDA_FOOD_DATA_CENTRAL,
            name = "Rice, white, cooked",
            servingQuantity = 100.0,
            servingUnit = "g",
            servingWeightGrams = 100.0,
            dataType = "Foundation",
            matchScore = 0.91
        )

        val analysis = source.scaledAnalysis(
            StructuredFoodItem(name = "rice", quantity = 150.0, unit = "g", preparation = "cooked")
        )

        assertEquals(195, analysis.calories)
        assertEquals(4.1, analysis.protein, 0.001)
        assertEquals(42.0, analysis.carbs, 0.001)
        assertEquals(150.0, analysis.servingSizeGrams, 0.0)
        assertTrue(analysis.servingSizeIsKnown)
        assertEquals(150.0, analysis.selectedServingQuantity!!, 0.0)
        assertEquals("g", analysis.servingUnitOptions.first().unit)
        assertEquals(NutritionDataSource.USDA_FOOD_DATA_CENTRAL, analysis.nutritionDataSource)

        val metadata = analysis.nutritionSourceMetadata!!
        assertEquals("12345", metadata.providerItemID)
        assertEquals("Rice, white, cooked", metadata.matchedFoodName)
        assertEquals(150.0, metadata.portionQuantity!!, 0.0)
        assertEquals("g", metadata.portionUnit)
        assertEquals("Foundation", metadata.databaseDescription)
        assertEquals(0.91, metadata.matchConfidence!!, 0.0)
    }

    @Test
    fun volumeServingScalesWithoutInventingMassConversion() {
        val source = candidate(
            id = "lactaid-fat-free",
            source = NutritionDataSource.OPEN_FOOD_FACTS,
            name = "Fat Free Milk",
            brand = "Lactaid",
            servingQuantity = 240.0,
            servingUnit = "mL",
            servingWeightGrams = null,
            facts = NutritionFacts(calories = 80.0, protein = 8.0, carbs = 12.0, fat = 0.0)
        )

        val analysis = source.scaledAnalysis(
            StructuredFoodItem(
                name = "Fat Free Milk",
                brand = "Lactaid",
                quantity = 467.0,
                unit = "mL",
                branded = true
            )
        )

        assertEquals(156, analysis.calories)
        assertEquals(15.6, analysis.protein, 0.001)
        assertEquals(23.4, analysis.carbs, 0.001)
        assertEquals(false, analysis.servingSizeIsKnown)
        val metadata = analysis.nutritionSourceMetadata!!
        assertEquals(467.0, metadata.portionQuantity!!, 0.0)
        assertEquals("mL", metadata.portionUnit)
        assertEquals(false, metadata.portionIsEstimated)
    }

    @Test
    fun unsupportedVolumeToMassPortionIsMarkedEstimated() {
        val source = candidate(
            id = "per-100g",
            source = NutritionDataSource.USDA_FOOD_DATA_CENTRAL,
            name = "Cooked cereal",
            servingQuantity = 100.0,
            servingUnit = "g",
            servingWeightGrams = 100.0
        )

        val analysis = source.scaledAnalysis(
            StructuredFoodItem(name = "Cooked cereal", quantity = 1.0, unit = "cup")
        )

        assertEquals(130, analysis.calories)
        assertTrue(analysis.nutritionSourceMetadata!!.portionIsEstimated)
    }

    @Test
    fun usdaSearchNutrientsUseStableIdsAndUnitConversion() {
        val nutrients = Json.parseToJsonElement(
            """
            [
              { "nutrientId": 1062, "nutrientName": "Energy", "unitName": "kJ", "value": 1000 },
              { "nutrientId": 1003, "nutrientName": "Protein", "unitName": "mg", "value": 1500 },
              { "nutrientId": 1093, "nutrientName": "Sodium, Na", "unitName": "g", "value": 0.25 },
              { "nutrientId": 1106, "nutrientName": "Vitamin A", "unitName": "mg", "value": 0.5 }
            ]
            """
        ) as JsonArray

        val facts = USDANutrientMapper.fromSearchNutrients(nutrients)!!

        assertEquals(239.00573614, facts.calories!!, 0.0001)
        assertEquals(1.5, facts.protein!!, 0.0001)
        assertEquals(250.0, facts.sodium!!, 0.0001)
        assertEquals(500.0, facts.vitaminA!!, 0.0001)
    }

    @Test
    fun foodEntryNutritionSourceRoundTripsAndDuplicates() {
        val format = Json { ignoreUnknownKeys = true }
        val metadata = NutritionSourceMetadata(
            providerItemID = "fdc-123",
            matchedFoodName = "Rice, white, cooked",
            servingQuantity = 100.0,
            servingUnit = "g",
            servingWeightGrams = 100.0,
            portionQuantity = 150.0,
            portionUnit = "g",
            matchConfidence = 0.91,
            databaseDescription = "Foundation"
        )
        val original = FoodEntry(
            name = "Rice",
            calories = 195,
            protein = 4.1,
            carbs = 42.0,
            fat = 0.5,
            source = FoodSource.TEXT_INPUT,
            nutritionDataSource = NutritionDataSource.USDA_FOOD_DATA_CENTRAL,
            nutritionSourceMetadata = metadata
        )

        val decoded = format.decodeFromString<FoodEntry>(format.encodeToString(original))
        val duplicated = decoded.duplicatedForLogging(Instant.parse("2027-01-15T12:00:00Z"))

        assertEquals(NutritionDataSource.USDA_FOOD_DATA_CENTRAL, decoded.nutritionDataSource)
        assertEquals(metadata, decoded.nutritionSourceMetadata)
        assertEquals(NutritionDataSource.USDA_FOOD_DATA_CENTRAL, duplicated.nutritionDataSource)
        assertEquals(metadata, duplicated.nutritionSourceMetadata)
    }

    @Test
    fun oldFoodEntryWithoutNutritionSourceDefaultsFromLoggingSource() {
        val entry = Json { ignoreUnknownKeys = true }.decodeFromString<FoodEntry>(
            """
            {
              "name": "Banana",
              "calories": 105,
              "protein": 1.3,
              "carbs": 27.0,
              "fat": 0.4,
              "source": "textInput"
            }
            """
        )

        assertEquals(NutritionDataSource.AI_ESTIMATE, entry.nutritionDataSource)
        assertEquals(null, entry.nutritionSourceMetadata)
    }

    private fun candidate(
        id: String,
        source: NutritionDataSource,
        name: String,
        brand: String? = null,
        servingQuantity: Double? = 100.0,
        servingUnit: String? = "g",
        servingWeightGrams: Double? = 100.0,
        facts: NutritionFacts = NutritionFacts(calories = 130.0, protein = 2.7, carbs = 28.0, fat = 0.3),
        dataType: String? = null,
        matchScore: Double = 0.0
    ): NutritionCandidate = NutritionCandidate(
        nutritionDataSource = source,
        providerID = id,
        name = name,
        brand = brand,
        servingQuantity = servingQuantity,
        servingUnit = servingUnit,
        servingWeightGrams = servingWeightGrams,
        facts = facts,
        dataType = dataType,
        matchScore = matchScore
    )
}
