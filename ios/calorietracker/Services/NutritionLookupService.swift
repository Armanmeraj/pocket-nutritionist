import Foundation

struct StructuredFoodItem: Codable, Equatable, Sendable, Identifiable {
    var name: String
    var brand: String?
    var quantity: Double?
    var unit: String?
    var branded: Bool?
    var variant: String?
    var preparation: String?
    var modifiers: [String]
    var portionIsEstimated: Bool

    var id: String { lookupQuery.lowercased() }

    init(
        name: String,
        brand: String? = nil,
        quantity: Double? = nil,
        unit: String? = nil,
        branded: Bool? = nil,
        variant: String? = nil,
        preparation: String? = nil,
        modifiers: [String] = [],
        portionIsEstimated: Bool = false
    ) {
        self.name = name
        self.brand = brand
        self.quantity = quantity
        self.unit = unit
        self.branded = branded
        self.variant = variant
        self.preparation = preparation
        self.modifiers = modifiers
        self.portionIsEstimated = portionIsEstimated
    }

    var lookupQuery: String {
        [brand, variant, preparation, name, modifiers.joined(separator: " ")]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
    }

    static func inferred(from analysis: GeminiService.FoodAnalysis, originalText: String? = nil) -> StructuredFoodItem {
        let source = originalText?.trimmingCharacters(in: .whitespacesAndNewlines)
        let parsed = NutritionTextInterpreter.parse(source ?? analysis.name)
        return StructuredFoodItem(
            name: parsed.name.isEmpty ? analysis.name : parsed.name,
            brand: parsed.brand,
            quantity: parsed.quantity ?? analysis.selectedServingQuantity,
            unit: parsed.unit ?? analysis.selectedServingUnit,
            branded: parsed.brand != nil,
            variant: parsed.variant,
            preparation: parsed.preparation,
            modifiers: parsed.modifiers,
            portionIsEstimated: !analysis.servingSizeIsKnown
        )
    }
}

struct NutritionFacts: Codable, Equatable, Sendable {
    var calories: Double?
    var protein: Double?
    var carbs: Double?
    var fat: Double?
    var sugar: Double?
    var addedSugar: Double?
    var fiber: Double?
    var saturatedFat: Double?
    var monounsaturatedFat: Double?
    var polyunsaturatedFat: Double?
    var cholesterol: Double?
    var caffeine: Double?
    var supplementalNutrients: [String: Double]
    var sodium: Double?
    var potassium: Double?
    var transFat: Double?
    var calcium: Double?
    var iron: Double?
    var magnesium: Double?
    var zinc: Double?
    var vitaminA: Double?
    var vitaminC: Double?
    var vitaminD: Double?
    var vitaminB12: Double?
    var vitaminE: Double?
    var vitaminK: Double?
    var folate: Double?
    var omega3: Double?

    init(
        calories: Double? = nil,
        protein: Double? = nil,
        carbs: Double? = nil,
        fat: Double? = nil,
        sugar: Double? = nil,
        addedSugar: Double? = nil,
        fiber: Double? = nil,
        saturatedFat: Double? = nil,
        monounsaturatedFat: Double? = nil,
        polyunsaturatedFat: Double? = nil,
        cholesterol: Double? = nil,
        caffeine: Double? = nil,
        supplementalNutrients: [String: Double] = [:],
        sodium: Double? = nil,
        potassium: Double? = nil,
        transFat: Double? = nil,
        calcium: Double? = nil,
        iron: Double? = nil,
        magnesium: Double? = nil,
        zinc: Double? = nil,
        vitaminA: Double? = nil,
        vitaminC: Double? = nil,
        vitaminD: Double? = nil,
        vitaminB12: Double? = nil,
        vitaminE: Double? = nil,
        vitaminK: Double? = nil,
        folate: Double? = nil,
        omega3: Double? = nil
    ) {
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.sugar = sugar
        self.addedSugar = addedSugar
        self.fiber = fiber
        self.saturatedFat = saturatedFat
        self.monounsaturatedFat = monounsaturatedFat
        self.polyunsaturatedFat = polyunsaturatedFat
        self.cholesterol = cholesterol
        self.caffeine = caffeine
        self.supplementalNutrients = supplementalNutrients
        self.sodium = sodium
        self.potassium = potassium
        self.transFat = transFat
        self.calcium = calcium
        self.iron = iron
        self.magnesium = magnesium
        self.zinc = zinc
        self.vitaminA = vitaminA
        self.vitaminC = vitaminC
        self.vitaminD = vitaminD
        self.vitaminB12 = vitaminB12
        self.vitaminE = vitaminE
        self.vitaminK = vitaminK
        self.folate = folate
        self.omega3 = omega3
    }

    var hasUsableNutrition: Bool {
        calories != nil || protein != nil || carbs != nil || fat != nil
    }

    func scaled(by factor: Double) -> NutritionFacts {
        func s(_ value: Double?) -> Double? { value.map { round($0 * factor * 10) / 10 } }
        return NutritionFacts(
            calories: calories.map { Double(Int(round($0 * factor))) },
            protein: s(protein),
            carbs: s(carbs),
            fat: s(fat),
            sugar: s(sugar),
            addedSugar: s(addedSugar),
            fiber: s(fiber),
            saturatedFat: s(saturatedFat),
            monounsaturatedFat: s(monounsaturatedFat),
            polyunsaturatedFat: s(polyunsaturatedFat),
            cholesterol: s(cholesterol),
            caffeine: s(caffeine),
            supplementalNutrients: supplementalNutrients.mapValues { round($0 * factor * 10) / 10 },
            sodium: s(sodium),
            potassium: s(potassium),
            transFat: s(transFat),
            calcium: s(calcium),
            iron: s(iron),
            magnesium: s(magnesium),
            zinc: s(zinc),
            vitaminA: s(vitaminA),
            vitaminC: s(vitaminC),
            vitaminD: s(vitaminD),
            vitaminB12: s(vitaminB12),
            vitaminE: s(vitaminE),
            vitaminK: s(vitaminK),
            folate: s(folate),
            omega3: s(omega3)
        )
    }

    static func from(_ analysis: GeminiService.FoodAnalysis) -> NutritionFacts {
        NutritionFacts(
            calories: Double(analysis.calories),
            protein: analysis.protein,
            carbs: analysis.carbs,
            fat: analysis.fat,
            sugar: analysis.sugar,
            addedSugar: analysis.addedSugar,
            fiber: analysis.fiber,
            saturatedFat: analysis.saturatedFat,
            monounsaturatedFat: analysis.monounsaturatedFat,
            polyunsaturatedFat: analysis.polyunsaturatedFat,
            cholesterol: analysis.cholesterol,
            caffeine: analysis.caffeine,
            supplementalNutrients: analysis.supplementalNutrients,
            sodium: analysis.sodium,
            potassium: analysis.potassium,
            transFat: analysis.transFat,
            calcium: analysis.calcium,
            iron: analysis.iron,
            magnesium: analysis.magnesium,
            zinc: analysis.zinc,
            vitaminA: analysis.vitaminA,
            vitaminC: analysis.vitaminC,
            vitaminD: analysis.vitaminD,
            vitaminB12: analysis.vitaminB12,
            vitaminE: analysis.vitaminE,
            vitaminK: analysis.vitaminK,
            folate: analysis.folate,
            omega3: analysis.omega3
        )
    }
}

struct NutritionCandidate: Codable, Equatable, Identifiable, Sendable {
    var nutritionDataSource: NutritionDataSource
    var providerID: String
    var barcode: String? = nil
    var name: String
    var brand: String? = nil
    var servingQuantity: Double? = nil
    var servingUnit: String? = nil
    var servingWeightGrams: Double? = nil
    var householdServingDescription: String? = nil
    var facts: NutritionFacts
    var dataType: String? = nil
    var matchScore: Double = 0

    var id: String { "\(nutritionDataSource.rawValue):\(providerID)" }

    var hasUsableNutrition: Bool {
        facts.hasUsableNutrition
    }

    func scaledAnalysis(for item: StructuredFoodItem) -> GeminiService.FoodAnalysis {
        let resolvedScale = NutritionPortionScaler.scaleFactor(for: item, candidate: self)
        let scale = resolvedScale ?? 1
        let scaledFacts = facts.scaled(by: scale)
        let servingSize = NutritionPortionScaler.displayServingAmount(for: item, candidate: self, scale: scale)
        let option = NutritionPortionScaler.servingOption(for: item, candidate: self, scaledServingAmount: servingSize.amount)
        let metadata = NutritionSourceMetadata(
            providerItemID: providerID,
            barcode: barcode,
            matchedFoodName: name,
            matchedBrand: brand,
            servingQuantity: servingQuantity,
            servingUnit: servingUnit,
            servingWeightGrams: servingWeightGrams,
            householdServingDescription: householdServingDescription,
            portionQuantity: item.quantity,
            portionUnit: item.unit,
            portionIsEstimated: item.portionIsEstimated || resolvedScale == nil,
            matchConfidence: matchScore,
            databaseDescription: dataType
        )

        return GeminiService.FoodAnalysis(
            name: [brand, name].compactMap { $0 }.joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? name,
            calories: Int(round(scaledFacts.calories ?? 0)),
            protein: scaledFacts.protein ?? 0,
            carbs: scaledFacts.carbs ?? 0,
            fat: scaledFacts.fat ?? 0,
            servingSizeGrams: servingSize.amount,
            emoji: nutritionDataSource == .openFoodFacts ? "🏷️" : "🍽️",
            sugar: scaledFacts.sugar,
            addedSugar: scaledFacts.addedSugar,
            fiber: scaledFacts.fiber,
            saturatedFat: scaledFacts.saturatedFat,
            monounsaturatedFat: scaledFacts.monounsaturatedFat,
            polyunsaturatedFat: scaledFacts.polyunsaturatedFat,
            cholesterol: scaledFacts.cholesterol,
            caffeine: scaledFacts.caffeine,
            supplementalNutrients: scaledFacts.supplementalNutrients,
            sodium: scaledFacts.sodium,
            potassium: scaledFacts.potassium,
            transFat: scaledFacts.transFat,
            calcium: scaledFacts.calcium,
            iron: scaledFacts.iron,
            magnesium: scaledFacts.magnesium,
            zinc: scaledFacts.zinc,
            vitaminA: scaledFacts.vitaminA,
            vitaminC: scaledFacts.vitaminC,
            vitaminD: scaledFacts.vitaminD,
            vitaminB12: scaledFacts.vitaminB12,
            vitaminE: scaledFacts.vitaminE,
            vitaminK: scaledFacts.vitaminK,
            folate: scaledFacts.folate,
            omega3: scaledFacts.omega3,
            servingUnitOptions: option.map { [$0] } ?? [],
            selectedServingUnit: option?.unit,
            selectedServingQuantity: item.quantity ?? 1,
            servingSizeIsKnown: servingSize.isKnownMass,
            nutritionDataSource: nutritionDataSource,
            nutritionSourceMetadata: metadata
        )
    }
}

protocol NutritionLookupProvider: Sendable {
    var source: NutritionDataSource { get }
    func search(for item: StructuredFoodItem) async throws -> [NutritionCandidate]
}

enum NutritionLookupError: Error, Equatable {
    case missingAPIKey
    case rateLimited
    case timeout
    case unavailable
    case invalidResponse
}

enum NutritionLookupResolution {
    case resolved(GeminiService.FoodAnalysis)
    case ambiguous(item: StructuredFoodItem, candidates: [NutritionCandidate], fallback: GeminiService.FoodAnalysis)
    case noMatch(GeminiService.FoodAnalysis)
}

struct NutritionLookupCoordinator: Sendable {
    var providers: [NutritionLookupProvider]
    var confirmedStore: NutritionConfirmedMappingStore?
    var queryCache: NutritionLookupQueryCache?
    var autoMatchThreshold: Double = 0.92
    var separationThreshold: Double = 0.06

    init(
        providers: [NutritionLookupProvider],
        confirmedStore: NutritionConfirmedMappingStore? = NutritionConfirmedMappingStore(),
        queryCache: NutritionLookupQueryCache? = .shared
    ) {
        self.providers = providers
        self.confirmedStore = confirmedStore
        self.queryCache = queryCache
    }

    static func live(previousEntries: [FoodEntry] = []) -> NutritionLookupCoordinator {
        var providers: [NutritionLookupProvider] = [PersonalNutritionLookupProvider(entries: previousEntries)]
        if let usdaKey = NutritionLookupSettings.usdaAPIKey {
            providers.append(USDAFoodDataCentralProvider(apiKey: usdaKey))
        }
        if NutritionLookupSettings.openFoodFactsEnabled {
            providers.append(OpenFoodFactsTextLookupProvider())
        }
        return NutritionLookupCoordinator(providers: providers)
    }

    func resolve(
        analysis fallback: GeminiService.FoodAnalysis,
        originalText: String? = nil
    ) async -> NutritionLookupResolution {
        guard NutritionLookupSettings.exactLookupEnabled else {
            return .noMatch(fallback.withNutritionSource(.aiEstimate))
        }
        let items = fallback.structuredItems.isEmpty
            ? [StructuredFoodItem.inferred(from: fallback, originalText: originalText)]
            : fallback.structuredItems
        guard !items.isEmpty else {
            return .noMatch(fallback.withNutritionSource(.aiEstimate))
        }

        var ingredientAnalyses: [GeminiService.FoodAnalysis] = []
        var anyDatabaseMatch = false
        var hasUnmappedUnmatchedComponent = false
        for item in items {
            if let confirmed = confirmedStore?.candidate(for: item) {
                anyDatabaseMatch = true
                ingredientAnalyses.append(confirmed.scaledAnalysis(for: item).withNutritionSource(.personalVerified))
                continue
            }

            let ranked = await rankedCandidates(for: item)
            if let top = ranked.first,
               top.matchScore >= autoMatchThreshold,
               ranked.dropFirst().first.map({ top.matchScore - $0.matchScore >= separationThreshold }) ?? true {
                anyDatabaseMatch = true
                ingredientAnalyses.append(top.scaledAnalysis(for: item))
            } else if ranked.count > 1, ranked.first?.matchScore ?? 0 >= 0.72, items.count == 1 {
                return .ambiguous(item: item, candidates: Array(ranked.prefix(5)), fallback: fallback.withNutritionSource(.aiEstimate))
            } else if let aiIngredient = fallback.ingredients.first(where: { NutritionLookupScorer.tokens($0.name).intersection(NutritionLookupScorer.tokens(item.name)).isEmpty == false }) {
                ingredientAnalyses.append(GeminiService.FoodAnalysis.fromIngredient(aiIngredient).withNutritionSource(.aiEstimate))
            } else {
                hasUnmappedUnmatchedComponent = true
            }
        }

        guard anyDatabaseMatch, !hasUnmappedUnmatchedComponent, !ingredientAnalyses.isEmpty else {
            return .noMatch(fallback.withNutritionSource(.aiEstimate))
        }
        if ingredientAnalyses.count == 1 {
            return .resolved(ingredientAnalyses[0])
        }
        return .resolved(GeminiService.FoodAnalysis.combined(
            name: fallback.name,
            emoji: fallback.emoji,
            analyses: ingredientAnalyses
        ))
    }

    func searchDatabase(query: String) async -> [NutritionCandidate] {
        let item = NutritionTextInterpreter.parse(query)
        return await rankedCandidates(for: item)
    }

    func rankedCandidates(for item: StructuredFoodItem) async -> [NutritionCandidate] {
        let personalProviders = providers.filter { $0.source == .personalVerified }
        let externalProviders = providers.filter { $0.source != .personalVerified }
        var candidates: [NutritionCandidate] = []
        for provider in personalProviders {
            guard let results = try? await provider.search(for: item) else { continue }
            candidates.append(contentsOf: results)
        }
        let cacheKey = externalCacheKey(for: item, providers: externalProviders)
        if let cacheKey, let cached = await queryCache?.candidates(for: cacheKey) {
            candidates.append(contentsOf: cached)
            return NutritionLookupScorer.ranked(item: item, candidates: candidates)
        }
        var externalCandidates: [NutritionCandidate] = []
        for provider in externalProviders {
            guard let results = try? await provider.search(for: item) else { continue }
            externalCandidates.append(contentsOf: results)
        }
        if let cacheKey {
            await queryCache?.remember(externalCandidates, for: cacheKey)
        }
        candidates.append(contentsOf: externalCandidates)
        return NutritionLookupScorer.ranked(item: item, candidates: candidates)
    }

    private func externalCacheKey(for item: StructuredFoodItem, providers: [NutritionLookupProvider]) -> String? {
        guard !providers.isEmpty else { return nil }
        let sources = providers.map { $0.source.rawValue }.sorted().joined(separator: ",")
        return "\(NutritionLookupScorer.normalizedText(item.lookupQuery))|\(sources)"
    }
}

actor NutritionLookupQueryCache {
    static let shared = NutritionLookupQueryCache()

    private struct Entry {
        var candidates: [NutritionCandidate]
        var savedAt: Date
    }

    private var entries: [String: Entry] = [:]
    private let ttl: TimeInterval

    init(ttl: TimeInterval = 6 * 60 * 60) {
        self.ttl = ttl
    }

    func candidates(for key: String) -> [NutritionCandidate]? {
        guard let entry = entries[key], Date().timeIntervalSince(entry.savedAt) < ttl else {
            entries[key] = nil
            return nil
        }
        return entry.candidates
    }

    func remember(_ candidates: [NutritionCandidate], for key: String) {
        entries[key] = Entry(candidates: candidates, savedAt: Date())
    }
}

actor NutritionProviderRateLimiter {
    static let shared = NutritionProviderRateLimiter()

    private var nextAllowedAt: [NutritionDataSource: Date] = [:]

    func waitIfNeeded(source: NutritionDataSource, minimumInterval: TimeInterval) async {
        let now = Date()
        if let next = nextAllowedAt[source], next > now {
            let seconds = next.timeIntervalSince(now)
            try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
        }
        nextAllowedAt[source] = Date().addingTimeInterval(minimumInterval)
    }
}

struct NutritionConfirmedMapping: Codable, Equatable, Sendable {
    var normalizedQuery: String
    var candidate: NutritionCandidate
    var updatedAt: Date
}

final class NutritionConfirmedMappingStore: @unchecked Sendable {
    private let defaults: UserDefaults
    private let key: String

    init(defaults: UserDefaults = .standard, key: String = "confirmedNutritionMappings") {
        self.defaults = defaults
        self.key = key
    }

    func candidate(for item: StructuredFoodItem) -> NutritionCandidate? {
        mappings()[NutritionLookupScorer.normalizedText(item.lookupQuery)]?.candidate
    }

    func remember(_ candidate: NutritionCandidate, for item: StructuredFoodItem) {
        var values = mappings()
        values[NutritionLookupScorer.normalizedText(item.lookupQuery)] = NutritionConfirmedMapping(
            normalizedQuery: NutritionLookupScorer.normalizedText(item.lookupQuery),
            candidate: candidate,
            updatedAt: Date()
        )
        save(values)
    }

    func replace(_ candidate: NutritionCandidate, for item: StructuredFoodItem) {
        remember(candidate, for: item)
    }

    func mappings() -> [String: NutritionConfirmedMapping] {
        guard let data = defaults.data(forKey: key),
              let decoded = try? JSONDecoder().decode([String: NutritionConfirmedMapping].self, from: data)
        else { return [:] }
        return decoded
    }

    private func save(_ values: [String: NutritionConfirmedMapping]) {
        guard let data = try? JSONEncoder().encode(values) else { return }
        defaults.set(data, forKey: key)
    }
}

struct PersonalNutritionLookupProvider: NutritionLookupProvider {
    let entries: [FoodEntry]
    var source: NutritionDataSource { .personalVerified }

    func search(for item: StructuredFoodItem) async throws -> [NutritionCandidate] {
        entries.compactMap { entry in
            guard entry.nutritionDataSource != .aiEstimate else { return nil }
            let score = NutritionLookupScorer.score(item: item, candidateName: entry.name, brand: entry.nutritionSourceMetadata?.matchedBrand)
            guard score >= 0.72 else { return nil }
            return NutritionCandidate(
                nutritionDataSource: .personalVerified,
                providerID: entry.nutritionSourceMetadata?.providerItemID ?? entry.id.uuidString,
                barcode: entry.nutritionSourceMetadata?.barcode,
                name: entry.nutritionSourceMetadata?.matchedFoodName ?? entry.name,
                brand: entry.nutritionSourceMetadata?.matchedBrand,
                servingQuantity: entry.selectedServingQuantity,
                servingUnit: entry.selectedServingUnit,
                servingWeightGrams: entry.servingSizeGrams,
                householdServingDescription: entry.nutritionSourceMetadata?.householdServingDescription,
                facts: NutritionFacts.from(entry),
                dataType: "Previously logged",
                matchScore: score
            )
        }
    }
}

extension NutritionFacts {
    static func from(_ entry: FoodEntry) -> NutritionFacts {
        NutritionFacts(
            calories: Double(entry.calories),
            protein: entry.protein,
            carbs: entry.carbs,
            fat: entry.fat,
            sugar: entry.sugar,
            addedSugar: entry.addedSugar,
            fiber: entry.fiber,
            saturatedFat: entry.saturatedFat,
            monounsaturatedFat: entry.monounsaturatedFat,
            polyunsaturatedFat: entry.polyunsaturatedFat,
            cholesterol: entry.cholesterol,
            caffeine: entry.caffeine,
            supplementalNutrients: entry.supplementalNutrients,
            sodium: entry.sodium,
            potassium: entry.potassium,
            transFat: entry.transFat,
            calcium: entry.calcium,
            iron: entry.iron,
            magnesium: entry.magnesium,
            zinc: entry.zinc,
            vitaminA: entry.vitaminA,
            vitaminC: entry.vitaminC,
            vitaminD: entry.vitaminD,
            vitaminB12: entry.vitaminB12,
            vitaminE: entry.vitaminE,
            vitaminK: entry.vitaminK,
            folate: entry.folate,
            omega3: entry.omega3
        )
    }
}

enum NutritionPortionScaler {
    static func scaleFactor(for item: StructuredFoodItem, candidate: NutritionCandidate) -> Double? {
        guard let quantity = item.quantity, quantity > 0 else { return 1 }
        let itemUnit = item.unit ?? candidate.servingUnit ?? "serving"
        let servingQuantity = candidate.servingQuantity.flatMap { $0 > 0 ? $0 : nil } ?? 1
        let servingUnit = candidate.servingUnit ?? "serving"

        if UnitNormalizer.sameUnit(itemUnit, servingUnit) {
            return quantity / servingQuantity
        }

        if let consumedGrams = UnitNormalizer.massInGrams(quantity: quantity, unit: itemUnit),
           let servingGrams = candidate.servingWeightGrams, servingGrams > 0 {
            return consumedGrams / servingGrams
        }

        if let consumedML = UnitNormalizer.volumeInMilliliters(quantity: quantity, unit: itemUnit),
           let servingML = UnitNormalizer.volumeInMilliliters(quantity: servingQuantity, unit: servingUnit),
           servingML > 0 {
            return consumedML / servingML
        }

        if UnitNormalizer.sameCountUnit(itemUnit, servingUnit) {
            return quantity / servingQuantity
        }

        return nil
    }

    static func displayServingAmount(
        for item: StructuredFoodItem,
        candidate: NutritionCandidate,
        scale: Double
    ) -> (amount: Double, isKnownMass: Bool) {
        if let quantity = item.quantity,
           let unit = item.unit,
           let grams = UnitNormalizer.massInGrams(quantity: quantity, unit: unit) {
            return (grams, true)
        }
        if let grams = candidate.servingWeightGrams, grams > 0 {
            return (grams * scale, true)
        }
        return (max(item.quantity ?? scale, 1), false)
    }

    static func servingOption(
        for item: StructuredFoodItem,
        candidate: NutritionCandidate,
        scaledServingAmount: Double
    ) -> ServingUnitOption? {
        if let unit = item.unit, let quantity = item.quantity, quantity > 0 {
            return ServingUnitOption(unit: unit, gramsPerUnit: scaledServingAmount / quantity, quantity: quantity)
        }
        if let unit = candidate.servingUnit,
           let quantity = candidate.servingQuantity,
           let grams = candidate.servingWeightGrams,
           quantity > 0, grams > 0 {
            return ServingUnitOption(unit: unit, gramsPerUnit: grams / quantity, quantity: quantity)
        }
        return nil
    }
}

enum UnitNormalizer {
    static func normalized(_ unit: String) -> String {
        let lower = unit
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: ".", with: "")
        switch lower {
        case "gram", "grams": return "g"
        case "kilogram", "kilograms", "kgs": return "kg"
        case "milligram", "milligrams": return "mg"
        case "ounce", "ounces", "ozs": return "oz"
        case "pound", "pounds", "lbs": return "lb"
        case "milliliter", "milliliters", "millilitre", "millilitres": return "ml"
        case "liter", "liters", "litre", "litres": return "l"
        case "fluid ounce", "fluid ounces", "fl ounce", "fl ounces": return "fl oz"
        case "tortillas": return "tortilla"
        case "pieces": return "piece"
        case "slices": return "slice"
        case "servings": return "serving"
        default:
            return lower.hasSuffix("s") && lower.count > 3 ? String(lower.dropLast()) : lower
        }
    }

    static func sameUnit(_ lhs: String, _ rhs: String) -> Bool {
        normalized(lhs) == normalized(rhs)
    }

    static func sameCountUnit(_ lhs: String, _ rhs: String) -> Bool {
        let countUnits: Set<String> = ["serving", "piece", "slice", "tortilla", "egg", "strip", "bar", "packet", "can", "cup"]
        let left = normalized(lhs)
        let right = normalized(rhs)
        return left == right && countUnits.contains(left)
    }

    static func massInGrams(quantity: Double, unit: String) -> Double? {
        switch normalized(unit) {
        case "g": quantity
        case "kg": quantity * 1_000
        case "mg": quantity / 1_000
        case "oz": quantity * 28.349523125
        case "lb": quantity * 453.59237
        default: nil
        }
    }

    static func volumeInMilliliters(quantity: Double, unit: String) -> Double? {
        switch normalized(unit) {
        case "ml": quantity
        case "l": quantity * 1_000
        case "fl oz": quantity * 29.5735295625
        case "cup": quantity * 236.5882365
        case "tbsp": quantity * 14.78676478125
        case "tsp": quantity * 4.92892159375
        default: nil
        }
    }
}

enum NutritionLookupScorer {
    private static let stopwords: Set<String> = ["a", "an", "and", "the", "of", "with", "food", "foods"]
    private static let importantModifiers: Set<String> = [
        "fat", "free", "skim", "whole", "wheat", "flour", "vanilla", "original",
        "cooked", "raw", "lean", "carb", "balance", "skinless", "boneless", "low",
        "reduced", "sugar", "unsweetened", "sweetened", "organic"
    ]

    static func ranked(item: StructuredFoodItem, candidates: [NutritionCandidate]) -> [NutritionCandidate] {
        candidates
            .filter(\.hasUsableNutrition)
            .map { candidate in
                var candidate = candidate
                candidate.matchScore = score(item: item, candidateName: candidate.name, brand: candidate.brand)
                return candidate
            }
            .sorted {
                if abs($0.matchScore - $1.matchScore) > 0.0001 { return $0.matchScore > $1.matchScore }
                return sourcePriority($0.nutritionDataSource) < sourcePriority($1.nutritionDataSource)
            }
    }

    static func score(item: StructuredFoodItem, candidateName: String, brand: String?) -> Double {
        let query = item.lookupQuery
        let queryTokens = tokens(query)
        let candidateText = [brand, candidateName].compactMap { $0 }.joined(separator: " ")
        let candidateTokens = tokens(candidateText)
        guard !queryTokens.isEmpty, !candidateTokens.isEmpty else { return 0 }

        let overlap = Double(queryTokens.intersection(candidateTokens).count) / Double(queryTokens.count)
        let brandScore: Double
        if let queryBrand = item.brand, !queryBrand.isEmpty {
            brandScore = normalizedText(queryBrand) == normalizedText(brand ?? "") ? 0.24 : -0.18
        } else {
            brandScore = 0.04
        }
        let exactBonus = normalizedText(candidateText).contains(normalizedText(query)) ? 0.12 : 0
        let modifierPenalty = missingModifierPenalty(queryTokens: queryTokens, candidateTokens: candidateTokens)
        let conflictPenalty = conflictPenalty(query: normalizedText(query), candidate: normalizedText(candidateText))
        return min(max(0.55 * overlap + brandScore + exactBonus + 0.12 - modifierPenalty - conflictPenalty, 0), 0.99)
    }

    static func tokens(_ text: String) -> Set<String> {
        Set(normalizedText(text).split(separator: " ").map(String.init).filter { !stopwords.contains($0) })
    }

    static func normalizedText(_ text: String) -> String {
        text
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .lowercased()
            .replacingOccurrences(of: #"[^a-z0-9%]+"#, with: " ", options: .regularExpression)
            .replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func missingModifierPenalty(queryTokens: Set<String>, candidateTokens: Set<String>) -> Double {
        let missing = queryTokens.intersection(importantModifiers).subtracting(candidateTokens)
        return Double(missing.count) * 0.12
    }

    private static func conflictPenalty(query: String, candidate: String) -> Double {
        var penalty = 0.0
        if (query.contains("fat free") || query.contains("skim")) && candidate.contains("whole") {
            penalty += 0.35
        }
        if query.contains("whole") && candidate.contains("fat free") {
            penalty += 0.30
        }
        if query.contains("whole wheat") && candidate.contains("flour") && !candidate.contains("whole wheat") {
            penalty += 0.22
        }
        if query.contains("cooked") && candidate.contains("raw") {
            penalty += 0.28
        }
        if query.contains("raw") && candidate.contains("cooked") {
            penalty += 0.28
        }
        return penalty
    }

    private static func sourcePriority(_ source: NutritionDataSource) -> Int {
        switch source {
        case .personalVerified: 0
        case .usdaFoodDataCentral: 1
        case .openFoodFacts: 2
        case .manual: 3
        case .mixed: 4
        case .aiEstimate: 5
        }
    }
}

enum NutritionTextInterpreter {
    static func parse(_ text: String) -> StructuredFoodItem {
        let cleaned = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let pattern = #"(?i)^\s*([0-9]+(?:[.,][0-9]+)?)\s*([a-zA-Z][a-zA-Z ]{0,14})?\s+(.+)$"#
        var quantity: Double?
        var unit: String?
        var remaining = cleaned
        if let regex = try? NSRegularExpression(pattern: pattern),
           let match = regex.firstMatch(in: cleaned, range: NSRange(cleaned.startIndex..., in: cleaned)),
           let valueRange = Range(match.range(at: 1), in: cleaned),
           let value = Double(cleaned[valueRange].replacingOccurrences(of: ",", with: ".")),
           let restRange = Range(match.range(at: 3), in: cleaned) {
            quantity = value
            unit = Range(match.range(at: 2), in: cleaned).map { String(cleaned[$0]).trimmingCharacters(in: .whitespacesAndNewlines) }
            remaining = String(cleaned[restRange])
        }

        let tokens = remaining.split(separator: " ").map(String.init)
        let knownBrands: Set<String> = ["lactaid", "mission", "cheerios", "kirkland", "chobani", "fairlife"]
        let brand = tokens.first.flatMap { knownBrands.contains($0.lowercased()) ? $0 : nil }
        let nameTokens = brand == nil ? tokens : Array(tokens.dropFirst())
        let normalized = NutritionLookupScorer.normalizedText(remaining)
        let preparation = normalized.contains("cooked") ? "cooked" : (normalized.contains("raw") ? "raw" : nil)
        let modifiers = ["fat free", "whole wheat", "whole", "skim", "vanilla", "original", "cooked", "raw", "boneless", "skinless", "carb balance"]
            .filter { normalized.contains($0) }
        return StructuredFoodItem(
            name: nameTokens.joined(separator: " ").nonEmpty ?? remaining,
            brand: brand,
            quantity: quantity,
            unit: unit?.nonEmpty,
            branded: brand != nil,
            preparation: preparation,
            modifiers: modifiers
        )
    }
}

struct USDAFoodDataCentralProvider: NutritionLookupProvider {
    var apiKey: String
    var session: URLSession = .shared
    var baseURL: URL = URL(string: "https://api.nal.usda.gov/fdc/v1")!
    var source: NutritionDataSource { .usdaFoodDataCentral }

    func search(for item: StructuredFoodItem) async throws -> [NutritionCandidate] {
        guard !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw NutritionLookupError.missingAPIKey
        }
        await NutritionProviderRateLimiter.shared.waitIfNeeded(source: source, minimumInterval: 0.25)
        let search = try await searchFoods(query: item.lookupQuery)
        let limited = Array(search.foods.prefix(8))
        var candidates: [NutritionCandidate] = []
        for food in limited {
            if let candidate = try? await detailsCandidate(fdcID: food.fdcID, fallback: food) {
                candidates.append(candidate)
            } else if let candidate = food.candidate {
                candidates.append(candidate)
            }
        }
        return candidates
    }

    private func searchFoods(query: String) async throws -> USDASearchResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent("foods/search"), resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "api_key", value: apiKey),
            URLQueryItem(name: "query", value: query),
            URLQueryItem(name: "pageSize", value: "25")
        ]
        let data = try await data(from: components.url!)
        do {
            return try JSONDecoder().decode(USDASearchResponse.self, from: data)
        } catch {
            throw NutritionLookupError.invalidResponse
        }
    }

    private func detailsCandidate(fdcID: Int, fallback: USDASearchFood) async throws -> NutritionCandidate? {
        var components = URLComponents(url: baseURL.appendingPathComponent("food/\(fdcID)"), resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "api_key", value: apiKey),
            URLQueryItem(name: "format", value: "full")
        ]
        let data = try await data(from: components.url!)
        let detail = try JSONDecoder().decode(USDAFoodDetail.self, from: data)
        return detail.candidate(fallback: fallback)
    }

    private func data(from url: URL) async throws -> Data {
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError where error.code == .timedOut {
            throw NutritionLookupError.timeout
        } catch {
            throw NutritionLookupError.unavailable
        }
        guard let http = response as? HTTPURLResponse else { throw NutritionLookupError.invalidResponse }
        switch http.statusCode {
        case 200..<300: return data
        case 429: throw NutritionLookupError.rateLimited
        case 500..<600: throw NutritionLookupError.unavailable
        default: throw NutritionLookupError.invalidResponse
        }
    }
}

private struct USDASearchResponse: Decodable {
    let foods: [USDASearchFood]
}

private struct USDASearchFood: Decodable {
    let fdcID: Int
    let description: String
    let brandOwner: String?
    let brandName: String?
    let dataType: String?
    let servingSize: Double?
    let servingSizeUnit: String?
    let foodNutrients: [USDASearchNutrient]?

    private enum CodingKeys: String, CodingKey {
        case fdcID = "fdcId"
        case description, brandOwner, brandName, dataType, servingSize, servingSizeUnit, foodNutrients
    }

    var candidate: NutritionCandidate? {
        guard let facts = USDANutrientMapper.facts(fromSearchNutrients: foodNutrients ?? []) else { return nil }
        return NutritionCandidate(
            nutritionDataSource: .usdaFoodDataCentral,
            providerID: String(fdcID),
            name: description,
            brand: brandName ?? brandOwner,
            servingQuantity: 100,
            servingUnit: "g",
            servingWeightGrams: 100,
            facts: facts,
            dataType: dataType,
            matchScore: 0
        )
    }
}

struct USDASearchNutrient: Decodable {
    let nutrientId: Int?
    let nutrientName: String?
    let unitName: String?
    let value: Double?
}

private struct USDAFoodDetail: Decodable {
    let fdcID: Int
    let description: String
    let brandOwner: String?
    let brandName: String?
    let dataType: String?
    let servingSize: Double?
    let servingSizeUnit: String?
    let householdServingFullText: String?
    let labelNutrients: USDALabelNutrients?
    let foodNutrients: [USDAFoodNutrient]?
    let foodPortions: [USDAFoodPortion]?

    private enum CodingKeys: String, CodingKey {
        case fdcID = "fdcId"
        case description, brandOwner, brandName, dataType, servingSize, servingSizeUnit
        case householdServingFullText, labelNutrients, foodNutrients, foodPortions
    }

    func candidate(fallback: USDASearchFood) -> NutritionCandidate? {
        let labelFacts = labelNutrients?.facts
        let usesLabelFacts = labelFacts?.hasUsableNutrition == true
        let facts = usesLabelFacts ? labelFacts : USDANutrientMapper.facts(fromFoodNutrients: foodNutrients ?? [])
        guard let facts, facts.hasUsableNutrition else { return nil }
        let servingUnit = servingSizeUnit ?? fallback.servingSizeUnit ?? "g"
        let servingQuantity = servingSize ?? fallback.servingSize ?? 100
        let servingWeight = UnitNormalizer.massInGrams(quantity: servingQuantity, unit: servingUnit)
            ?? foodPortions?.compactMap(\.gramWeight).first
            ?? (servingUnit.lowercased() == "g" ? servingQuantity : nil)
        let servingFacts = !usesLabelFacts && abs((servingWeight ?? 100) - 100) > 0.001
            ? facts.scaled(by: (servingWeight ?? 100) / 100)
            : facts
        return NutritionCandidate(
            nutritionDataSource: .usdaFoodDataCentral,
            providerID: String(fdcID),
            name: description,
            brand: brandName ?? brandOwner ?? fallback.brandName ?? fallback.brandOwner,
            servingQuantity: usesLabelFacts ? servingQuantity : (servingWeight ?? 100),
            servingUnit: usesLabelFacts ? servingUnit : "g",
            servingWeightGrams: servingWeight ?? 100,
            householdServingDescription: householdServingFullText,
            facts: servingFacts,
            dataType: dataType ?? fallback.dataType,
            matchScore: 0
        )
    }
}

private struct USDAFoodPortion: Decodable {
    let gramWeight: Double?
}

struct USDAFoodNutrient: Decodable {
    let amount: Double?
    let nutrient: USDANutrient?
}

struct USDANutrient: Decodable {
    let id: Int?
    let number: String?
    let name: String?
    let unitName: String?
}

private struct USDALabelNutrients: Decodable {
    let values: [String: Double]

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicCodingKey.self)
        var values: [String: Double] = [:]
        for key in container.allKeys {
            if let nested = try? container.decode([String: FlexibleDouble].self, forKey: key),
               let value = nested["value"]?.value {
                values[key.stringValue] = value
            } else if let value = try? container.decode(FlexibleDouble.self, forKey: key) {
                values[key.stringValue] = value.value
            }
        }
        self.values = values
    }

    var facts: NutritionFacts {
        NutritionFacts(
            calories: values["calories"],
            protein: values["protein"],
            carbs: values["carbohydrates"],
            fat: values["fat"],
            sugar: values["sugars"],
            addedSugar: values["addedSugars"],
            fiber: values["fiber"],
            saturatedFat: values["saturatedFat"],
            cholesterol: values["cholesterol"],
            sodium: values["sodium"],
            potassium: values["potassium"],
            transFat: values["transFat"],
            calcium: values["calcium"],
            iron: values["iron"]
        )
    }
}

enum USDANutrientMapper {
    static func facts(fromSearchNutrients nutrients: [USDASearchNutrient]) -> NutritionFacts? {
        var builder = Builder()
        for nutrient in nutrients {
            builder.apply(id: nutrient.nutrientId, name: nutrient.nutrientName, amount: nutrient.value, unit: nutrient.unitName)
        }
        return builder.facts.hasUsableNutrition ? builder.facts : nil
    }

    static func facts(fromFoodNutrients nutrients: [USDAFoodNutrient]) -> NutritionFacts? {
        var builder = Builder()
        for item in nutrients {
            builder.apply(id: item.nutrient?.id, name: item.nutrient?.name, amount: item.amount, unit: item.nutrient?.unitName)
        }
        return builder.facts.hasUsableNutrition ? builder.facts : nil
    }

    private struct Builder {
        var facts = NutritionFacts()

        mutating func apply(id: Int?, name: String?, amount: Double?, unit: String?) {
            guard let amount else { return }
            switch id {
            case 1008: facts.calories = amount
            case 1062 where unit?.lowercased() == "kj": facts.calories = amount * 0.23900573614
            case 1003: facts.protein = grams(amount, unit)
            case 1005: facts.carbs = grams(amount, unit)
            case 1004: facts.fat = grams(amount, unit)
            case 2000: facts.sugar = grams(amount, unit)
            case 1235: facts.addedSugar = grams(amount, unit)
            case 1079: facts.fiber = grams(amount, unit)
            case 1258: facts.saturatedFat = grams(amount, unit)
            case 1292: facts.monounsaturatedFat = grams(amount, unit)
            case 1293: facts.polyunsaturatedFat = grams(amount, unit)
            case 1253: facts.cholesterol = milligrams(amount, unit)
            case 1057: facts.caffeine = milligrams(amount, unit)
            case 1093: facts.sodium = milligrams(amount, unit)
            case 1092: facts.potassium = milligrams(amount, unit)
            case 1257, 1259: facts.transFat = grams(amount, unit)
            case 1087: facts.calcium = milligrams(amount, unit)
            case 1089: facts.iron = milligrams(amount, unit)
            case 1090: facts.magnesium = milligrams(amount, unit)
            case 1095: facts.zinc = milligrams(amount, unit)
            case 1106: facts.vitaminA = micrograms(amount, unit)
            case 1162: facts.vitaminC = milligrams(amount, unit)
            case 1114: facts.vitaminD = micrograms(amount, unit)
            case 1178: facts.vitaminB12 = micrograms(amount, unit)
            case 1109: facts.vitaminE = milligrams(amount, unit)
            case 1185: facts.vitaminK = micrograms(amount, unit)
            case 1177: facts.folate = micrograms(amount, unit)
            case 1272, 1404: facts.omega3 = grams(amount, unit)
            default:
                applyByName(name: name, amount: amount, unit: unit)
            }
        }

        private mutating func applyByName(name: String?, amount: Double, unit: String?) {
            let normalized = NutritionLookupScorer.normalizedText(name ?? "")
            if normalized.contains("energy") && normalized.contains("kcal") { facts.calories = amount }
            else if normalized == "protein" { facts.protein = grams(amount, unit) }
            else if normalized.contains("carbohydrate") { facts.carbs = grams(amount, unit) }
            else if normalized.contains("total lipid") || normalized == "fat" { facts.fat = grams(amount, unit) }
        }

        private func grams(_ amount: Double, _ unit: String?) -> Double {
            switch unit?.lowercased() {
            case "mg": amount / 1_000
            case "ug", "µg", "mcg": amount / 1_000_000
            default: amount
            }
        }

        private func milligrams(_ amount: Double, _ unit: String?) -> Double {
            switch unit?.lowercased() {
            case "g": amount * 1_000
            case "ug", "µg", "mcg": amount / 1_000
            default: amount
            }
        }

        private func micrograms(_ amount: Double, _ unit: String?) -> Double {
            switch unit?.lowercased() {
            case "g": amount * 1_000_000
            case "mg": amount * 1_000
            default: amount
            }
        }
    }
}

struct OpenFoodFactsTextLookupProvider: NutritionLookupProvider {
    var session: URLSession = .shared
    var baseURL: URL = URL(string: "https://world.openfoodfacts.org")!
    var source: NutritionDataSource { .openFoodFacts }

    func search(for item: StructuredFoodItem) async throws -> [NutritionCandidate] {
        await NutritionProviderRateLimiter.shared.waitIfNeeded(source: source, minimumInterval: 1.0)
        var components = URLComponents(url: baseURL.appendingPathComponent("cgi/search.pl"), resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "search_terms", value: item.lookupQuery),
            URLQueryItem(name: "search_simple", value: "1"),
            URLQueryItem(name: "action", value: "process"),
            URLQueryItem(name: "json", value: "1"),
            URLQueryItem(name: "page_size", value: "10"),
            URLQueryItem(name: "fields", value: "code,product_name,generic_name,brands,serving_size,serving_quantity,nutriments")
        ]
        let (data, response) = try await session.data(for: URLRequest(url: components.url!))
        guard let http = response as? HTTPURLResponse else { throw NutritionLookupError.invalidResponse }
        switch http.statusCode {
        case 200..<300: break
        case 429: throw NutritionLookupError.rateLimited
        case 500..<600: throw NutritionLookupError.unavailable
        default: throw NutritionLookupError.invalidResponse
        }
        let decoded = try JSONDecoder().decode(OpenFoodFactsSearchResponse.self, from: data)
        return decoded.products.compactMap(\.candidate)
    }
}

private struct OpenFoodFactsSearchResponse: Decodable {
    let products: [OpenFoodFactsSearchProduct]
}

private struct OpenFoodFactsSearchProduct: Decodable {
    let code: String?
    let productName: String?
    let genericName: String?
    let brands: String?
    let servingSize: String?
    let servingQuantity: FlexibleDouble?
    let nutriments: [String: FlexibleDouble]?

    private enum CodingKeys: String, CodingKey {
        case code
        case productName = "product_name"
        case genericName = "generic_name"
        case brands
        case servingSize = "serving_size"
        case servingQuantity = "serving_quantity"
        case nutriments
    }

    var candidate: NutritionCandidate? {
        guard let nutriments else { return nil }
        let servingGrams = max(servingQuantity?.value ?? OpenFoodFactsSearchProduct.grams(from: servingSize) ?? 100, 1)
        let scale = servingGrams / 100
        func value(_ key: String) -> Double? {
            nutriments["\(key)_serving"]?.value ?? nutriments["\(key)_100g"].map { $0.value * scale }
        }
        let facts = NutritionFacts(
            calories: value("energy-kcal") ?? value("energy").map { $0 * 0.23900573614 },
            protein: value("proteins"),
            carbs: value("carbohydrates"),
            fat: value("fat"),
            sugar: value("sugars"),
            addedSugar: value("added-sugars"),
            fiber: value("fiber"),
            saturatedFat: value("saturated-fat"),
            monounsaturatedFat: value("monounsaturated-fat"),
            polyunsaturatedFat: value("polyunsaturated-fat"),
            cholesterol: value("cholesterol").map { $0 * 1_000 },
            caffeine: value("caffeine").map { $0 * 1_000 },
            sodium: value("sodium").map { $0 * 1_000 },
            potassium: value("potassium").map { $0 * 1_000 },
            transFat: value("trans-fat"),
            calcium: value("calcium").map { $0 * 1_000 },
            iron: value("iron").map { $0 * 1_000 },
            magnesium: value("magnesium").map { $0 * 1_000 },
            zinc: value("zinc").map { $0 * 1_000 },
            vitaminA: value("vitamin-a").map { $0 * 1_000_000 },
            vitaminC: value("vitamin-c").map { $0 * 1_000 },
            vitaminD: value("vitamin-d").map { $0 * 1_000_000 },
            vitaminB12: value("vitamin-b12").map { $0 * 1_000_000 },
            vitaminE: value("vitamin-e").map { $0 * 1_000 },
            vitaminK: value("vitamin-k").map { $0 * 1_000_000 },
            folate: value("folates").map { $0 * 1_000_000 },
            omega3: value("omega-3-fat")
        )
        guard facts.calories != nil || facts.protein != nil || facts.carbs != nil || facts.fat != nil else { return nil }
        let brand = brands?.split(separator: ",").first.map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
        return NutritionCandidate(
            nutritionDataSource: .openFoodFacts,
            providerID: code ?? UUID().uuidString,
            barcode: code,
            name: productName ?? genericName ?? "Open Food Facts product",
            brand: brand,
            servingQuantity: 1,
            servingUnit: "serving",
            servingWeightGrams: servingGrams,
            householdServingDescription: servingSize,
            facts: facts,
            dataType: "Open Food Facts",
            matchScore: 0
        )
    }

    private static func grams(from servingSize: String?) -> Double? {
        guard let servingSize else { return nil }
        let item = NutritionTextInterpreter.parse(servingSize)
        guard let quantity = item.quantity, let unit = item.unit else { return nil }
        return UnitNormalizer.massInGrams(quantity: quantity, unit: unit)
    }
}

private struct FlexibleDouble: Decodable {
    let value: Double

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let double = try? container.decode(Double.self) {
            value = double
        } else if let int = try? container.decode(Int.self) {
            value = Double(int)
        } else {
            let string = try container.decode(String.self)
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .replacingOccurrences(of: ",", with: ".")
            guard let parsed = Double(string) else {
                throw DecodingError.dataCorruptedError(in: container, debugDescription: "Not a number")
            }
            value = parsed
        }
    }
}

private struct DynamicCodingKey: CodingKey {
    let stringValue: String
    let intValue: Int?

    init?(stringValue: String) {
        self.stringValue = stringValue
        self.intValue = nil
    }

    init?(intValue: Int) {
        self.stringValue = "\(intValue)"
        self.intValue = intValue
    }
}

extension GeminiService.FoodAnalysis {
    func withNutritionSource(_ source: NutritionDataSource, metadata: NutritionSourceMetadata? = nil) -> GeminiService.FoodAnalysis {
        var copy = self
        copy.nutritionDataSource = source
        copy.nutritionSourceMetadata = metadata ?? copy.nutritionSourceMetadata
        return copy
    }

    static func fromIngredient(_ ingredient: MealIngredient) -> GeminiService.FoodAnalysis {
        GeminiService.FoodAnalysis(
            name: ingredient.name,
            calories: ingredient.calories,
            protein: ingredient.protein,
            carbs: ingredient.carbs,
            fat: ingredient.fat,
            servingSizeGrams: ingredient.grams,
            nutritionDataSource: ingredient.nutritionDataSource ?? .aiEstimate,
            nutritionSourceMetadata: ingredient.nutritionSourceMetadata
        )
    }

    static func combined(
        name: String,
        emoji: String?,
        analyses: [GeminiService.FoodAnalysis]
    ) -> GeminiService.FoodAnalysis {
        let totalSources = Set(analyses.map(\.nutritionDataSource))
        return GeminiService.FoodAnalysis(
            name: name,
            calories: analyses.reduce(0) { $0 + $1.calories },
            protein: analyses.reduce(0) { $0 + $1.protein },
            carbs: analyses.reduce(0) { $0 + $1.carbs },
            fat: analyses.reduce(0) { $0 + $1.fat },
            servingSizeGrams: analyses.reduce(0) { $0 + $1.servingSizeGrams },
            emoji: emoji,
            sugar: sum(analyses.map(\.sugar)),
            addedSugar: sum(analyses.map(\.addedSugar)),
            fiber: sum(analyses.map(\.fiber)),
            saturatedFat: sum(analyses.map(\.saturatedFat)),
            monounsaturatedFat: sum(analyses.map(\.monounsaturatedFat)),
            polyunsaturatedFat: sum(analyses.map(\.polyunsaturatedFat)),
            cholesterol: sum(analyses.map(\.cholesterol)),
            caffeine: sum(analyses.map(\.caffeine)),
            supplementalNutrients: SupplementalNutrient.allCases.reduce(into: [:]) { result, nutrient in
                let value = analyses.reduce(0) { $0 + ($1.supplementalNutrients[nutrient.rawValue] ?? 0) }
                if value > 0 { result[nutrient.rawValue] = value }
            },
            sodium: sum(analyses.map(\.sodium)),
            potassium: sum(analyses.map(\.potassium)),
            transFat: sum(analyses.map(\.transFat)),
            calcium: sum(analyses.map(\.calcium)),
            iron: sum(analyses.map(\.iron)),
            magnesium: sum(analyses.map(\.magnesium)),
            zinc: sum(analyses.map(\.zinc)),
            vitaminA: sum(analyses.map(\.vitaminA)),
            vitaminC: sum(analyses.map(\.vitaminC)),
            vitaminD: sum(analyses.map(\.vitaminD)),
            vitaminB12: sum(analyses.map(\.vitaminB12)),
            vitaminE: sum(analyses.map(\.vitaminE)),
            vitaminK: sum(analyses.map(\.vitaminK)),
            folate: sum(analyses.map(\.folate)),
            omega3: sum(analyses.map(\.omega3)),
            progressiveMeal: false,
            ingredients: analyses.map { analysis in
                MealIngredient(
                    name: analysis.name,
                    grams: analysis.servingSizeGrams,
                    calories: analysis.calories,
                    protein: analysis.protein,
                    carbs: analysis.carbs,
                    fat: analysis.fat,
                    nutritionDataSource: analysis.nutritionDataSource,
                    nutritionSourceMetadata: analysis.nutritionSourceMetadata
                )
            },
            nutritionDataSource: totalSources.count == 1 ? (totalSources.first ?? .aiEstimate) : .mixed
        )
    }

    private static func sum(_ values: [Double?]) -> Double? {
        let present = values.compactMap { $0 }
        guard !present.isEmpty else { return nil }
        return round(present.reduce(0, +) * 10) / 10
    }
}

private extension String {
    var nonEmpty: String? {
        isEmpty ? nil : self
    }
}
