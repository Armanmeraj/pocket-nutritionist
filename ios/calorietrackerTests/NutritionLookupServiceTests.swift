import Foundation
import Testing
@testable import calorietracker

@Suite("Nutrition lookup")
@MainActor
struct NutritionLookupServiceTests {
    @Test func structuredFoodItemsParseFromAIResponse() throws {
        let analysis = try GeminiService.parseFoodAnalysis(from: """
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
        """)

        #expect(analysis.nutritionDataSource == .aiEstimate)
        #expect(analysis.structuredItems.count == 1)
        let item = try #require(analysis.structuredItems.first)
        #expect(item.lookupQuery == "Fairlife fat free chilled milk high protein")
        #expect(item.quantity == 1.5)
        #expect(item.unit == "cup")
        #expect(item.branded == true)
        #expect(item.portionIsEstimated)
    }

    @Test func scorerRanksExactBrandAndModifierMatchFirst() {
        let item = StructuredFoodItem(
            name: "milk",
            brand: "Fairlife",
            quantity: 1,
            unit: "cup",
            variant: "fat free",
            modifiers: ["fat free"]
        )
        let exact = candidate(
            id: "exact",
            source: .openFoodFacts,
            name: "Fat Free Milk",
            brand: "Fairlife"
        )
        let conflicting = candidate(
            id: "conflict",
            source: .usdaFoodDataCentral,
            name: "Whole Milk",
            brand: "Acme"
        )

        let ranked = NutritionLookupScorer.ranked(item: item, candidates: [conflicting, exact])

        #expect(ranked.first?.providerID == "exact")
        #expect((ranked.first?.matchScore ?? 0) > (ranked.last?.matchScore ?? 0))
    }

    @Test func scorerPrefersUSDATieForGenericFoods() {
        let item = StructuredFoodItem(name: "rice", quantity: 100, unit: "g")
        let openFoodFacts = candidate(id: "off", source: .openFoodFacts, name: "Rice")
        let usda = candidate(id: "usda", source: .usdaFoodDataCentral, name: "Rice")

        let ranked = NutritionLookupScorer.ranked(item: item, candidates: [openFoodFacts, usda])

        #expect(ranked.first?.providerID == "usda")
    }

    @Test func candidateScalingUsesRequestedMassAndPreservesSourceMetadata() throws {
        let source = candidate(
            id: "12345",
            source: .usdaFoodDataCentral,
            name: "Rice, white, cooked",
            servingQuantity: 100,
            servingUnit: "g",
            servingWeightGrams: 100,
            dataType: "Foundation",
            matchScore: 0.91
        )

        let analysis = source.scaledAnalysis(
            for: StructuredFoodItem(name: "rice", quantity: 150, unit: "g", preparation: "cooked")
        )

        #expect(analysis.calories == 195)
        #expect(abs(analysis.protein - 4.1) < 0.001)
        #expect(abs(analysis.carbs - 42.0) < 0.001)
        #expect(analysis.servingSizeGrams == 150)
        #expect(analysis.servingSizeIsKnown)
        #expect(analysis.selectedServingQuantity == 150)
        #expect(analysis.servingUnitOptions.first?.unit == "g")
        #expect(analysis.nutritionDataSource == .usdaFoodDataCentral)

        let metadata = try #require(analysis.nutritionSourceMetadata)
        #expect(metadata.providerItemID == "12345")
        #expect(metadata.matchedFoodName == "Rice, white, cooked")
        #expect(metadata.portionQuantity == 150)
        #expect(metadata.portionUnit == "g")
        #expect(metadata.databaseDescription == "Foundation")
        #expect(metadata.matchConfidence == 0.91)
    }

    @Test func volumeServingScalesWithoutInventingMassConversion() throws {
        let source = candidate(
            id: "lactaid-fat-free",
            source: .openFoodFacts,
            name: "Fat Free Milk",
            brand: "Lactaid",
            servingQuantity: 240,
            servingUnit: "mL",
            servingWeightGrams: nil,
            facts: NutritionFacts(calories: 80, protein: 8, carbs: 12, fat: 0)
        )

        let analysis = source.scaledAnalysis(
            for: StructuredFoodItem(name: "Fat Free Milk", brand: "Lactaid", quantity: 467, unit: "mL", branded: true)
        )

        #expect(analysis.calories == 156)
        #expect(abs(analysis.protein - 15.6) < 0.001)
        #expect(abs(analysis.carbs - 23.4) < 0.001)
        #expect(analysis.servingSizeIsKnown == false)
        let metadata = try #require(analysis.nutritionSourceMetadata)
        #expect(metadata.portionQuantity == 467)
        #expect(metadata.portionUnit == "mL")
        #expect(metadata.portionIsEstimated == false)
    }

    @Test func unsupportedVolumeToMassPortionIsMarkedEstimated() throws {
        let source = candidate(
            id: "per-100g",
            source: .usdaFoodDataCentral,
            name: "Cooked cereal",
            servingQuantity: 100,
            servingUnit: "g",
            servingWeightGrams: 100
        )

        let analysis = source.scaledAnalysis(
            for: StructuredFoodItem(name: "Cooked cereal", quantity: 1, unit: "cup")
        )

        #expect(analysis.calories == 130)
        let metadata = try #require(analysis.nutritionSourceMetadata)
        #expect(metadata.portionIsEstimated)
    }

    @Test func unitAndMicronutrientScalingUseMathematicalConversionsOnly() throws {
        #expect(abs((UnitNormalizer.massInGrams(quantity: 1, unit: "oz") ?? 0) - 28.349523125) < 0.0001)
        #expect(UnitNormalizer.massInGrams(quantity: 1, unit: "cup") == nil)
        #expect(UnitNormalizer.volumeInMilliliters(quantity: 2, unit: "cup") != nil)

        let scaled = NutritionFacts(
            calories: 80,
            protein: 8,
            carbs: 12,
            fat: 0,
            sodium: 120,
            vitaminA: 150,
            vitaminD: 2.5
        ).scaled(by: 2.5)

        #expect(scaled.calories == 200)
        #expect(scaled.protein == 20)
        #expect(scaled.sodium == 300)
        #expect(scaled.vitaminA == 375)
        #expect(scaled.vitaminD == 6.3)
    }

    @Test func usdaSearchNutrientsUseStableIDsAndUnitConversion() throws {
        let facts = try #require(USDANutrientMapper.facts(fromSearchNutrients: [
            USDASearchNutrient(nutrientId: 1062, nutrientName: "Energy", unitName: "kJ", value: 1_000),
            USDASearchNutrient(nutrientId: 1003, nutrientName: "Protein", unitName: "mg", value: 1_500),
            USDASearchNutrient(nutrientId: 1093, nutrientName: "Sodium, Na", unitName: "g", value: 0.25),
            USDASearchNutrient(nutrientId: 1106, nutrientName: "Vitamin A", unitName: "mg", value: 0.5)
        ]))

        #expect(abs((facts.calories ?? 0) - 239.00573614) < 0.0001)
        #expect(abs((facts.protein ?? 0) - 1.5) < 0.0001)
        #expect(abs((facts.sodium ?? 0) - 250) < 0.0001)
        #expect(abs((facts.vitaminA ?? 0) - 500) < 0.0001)
    }

    @Test func unmappedMultiComponentMealFallsBackToWholeAIEstimate() async throws {
        let matched = StructuredFoodItem(
            name: "milk",
            brand: "Fairlife",
            quantity: 1,
            unit: "cup",
            variant: "fat free",
            modifiers: ["fat free"]
        )
        let unmapped = StructuredFoodItem(name: "egg", quantity: 1, unit: "large")
        let fallback = GeminiService.FoodAnalysis(
            name: "Fairlife fat free milk and egg",
            calories: 210,
            protein: 9,
            carbs: 28,
            fat: 6,
            servingSizeGrams: 150,
            structuredItems: [matched, unmapped]
        )
        let coordinator = NutritionLookupCoordinator(
            providers: [StaticNutritionLookupProvider(candidates: [
                candidate(id: "milk", source: .usdaFoodDataCentral, name: "Fat Free Milk", brand: "Fairlife")
            ])],
            confirmedStore: nil,
            queryCache: nil
        )

        let resolution = await coordinator.resolve(analysis: fallback)

        guard case .noMatch(let analysis) = resolution else {
            Issue.record("Expected full AI fallback when an unmatched component cannot be mapped.")
            return
        }
        #expect(analysis.calories == 210)
        #expect(analysis.nutritionDataSource == .aiEstimate)
    }

    @Test func foodEntryNutritionSourceRoundTripsAndDuplicates() throws {
        let metadata = NutritionSourceMetadata(
            providerItemID: "fdc-123",
            matchedFoodName: "Rice, white, cooked",
            servingQuantity: 100,
            servingUnit: "g",
            servingWeightGrams: 100,
            portionQuantity: 150,
            portionUnit: "g",
            matchConfidence: 0.91,
            databaseDescription: "Foundation"
        )
        let original = FoodEntry(
            name: "Rice",
            calories: 195,
            protein: 4.1,
            carbs: 42,
            fat: 0.5,
            source: .textInput,
            nutritionDataSource: .usdaFoodDataCentral,
            nutritionSourceMetadata: metadata
        )

        let decoded = try JSONDecoder().decode(FoodEntry.self, from: JSONEncoder().encode(original))
        let duplicated = decoded.duplicatedForLogging(at: Date(timeIntervalSince1970: 1_800_000_000))

        #expect(decoded.nutritionDataSource == .usdaFoodDataCentral)
        #expect(decoded.nutritionSourceMetadata == metadata)
        #expect(duplicated.nutritionDataSource == .usdaFoodDataCentral)
        #expect(duplicated.nutritionSourceMetadata == metadata)
    }

    @Test func oldFoodEntryWithoutNutritionSourceDefaultsFromLoggingSource() throws {
        let data = #"""
        {
          "id": "00000000-0000-0000-0000-000000000001",
          "name": "Banana",
          "calories": 105,
          "protein": 1.3,
          "carbs": 27,
          "fat": 0.4,
          "timestamp": 0,
          "source": "textInput"
        }
        """#.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(FoodEntry.self, from: data)

        #expect(decoded.nutritionDataSource == .aiEstimate)
        #expect(decoded.nutritionSourceMetadata == nil)
    }

    @Test func confirmedMappingCanBeRememberedAndReplaced() throws {
        let suiteName = "NutritionLookupServiceTests.\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let store = NutritionConfirmedMappingStore(defaults: defaults, key: "testMappings")
        let item = StructuredFoodItem(name: "Fat Free Milk", brand: "Lactaid", quantity: 467, unit: "mL")
        let first = candidate(id: "first", source: .openFoodFacts, name: "Fat Free Milk", brand: "Lactaid")
        let replacement = candidate(id: "replacement", source: .usdaFoodDataCentral, name: "Milk, fat free", brand: "Lactaid")

        store.remember(first, for: item)
        #expect(store.candidate(for: item)?.providerID == "first")

        store.replace(replacement, for: item)
        #expect(store.candidate(for: item)?.providerID == "replacement")
    }

    private func candidate(
        id: String,
        source: NutritionDataSource,
        name: String,
        brand: String? = nil,
        servingQuantity: Double? = 100,
        servingUnit: String? = "g",
        servingWeightGrams: Double? = 100,
        facts: NutritionFacts? = nil,
        dataType: String? = nil,
        matchScore: Double = 0
    ) -> NutritionCandidate {
        NutritionCandidate(
            nutritionDataSource: source,
            providerID: id,
            barcode: nil,
            name: name,
            brand: brand,
            servingQuantity: servingQuantity,
            servingUnit: servingUnit,
            servingWeightGrams: servingWeightGrams,
            householdServingDescription: nil,
            facts: facts ?? NutritionFacts(calories: 130, protein: 2.7, carbs: 28, fat: 0.3),
            dataType: dataType,
            matchScore: matchScore
        )
    }
}

private struct StaticNutritionLookupProvider: NutritionLookupProvider {
    let candidates: [NutritionCandidate]
    var source: NutritionDataSource { .usdaFoodDataCentral }

    func search(for item: StructuredFoodItem) async throws -> [NutritionCandidate] {
        candidates.filter {
            NutritionLookupScorer.tokens($0.name).intersection(NutritionLookupScorer.tokens(item.name)).isEmpty == false
        }
    }
}
