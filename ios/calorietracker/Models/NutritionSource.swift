import Foundation

enum NutritionDataSource: String, Codable, CaseIterable, Sendable {
    case personalVerified
    case openFoodFacts
    case usdaFoodDataCentral
    case aiEstimate
    case manual
    case mixed

    var badgeText: String {
        switch self {
        case .personalVerified: "Saved"
        case .openFoodFacts: "Open Food Facts"
        case .usdaFoodDataCentral: "USDA"
        case .aiEstimate: "AI Estimate"
        case .manual: "Manual"
        case .mixed: "Mixed"
        }
    }
}

struct NutritionSourceMetadata: Codable, Equatable, Sendable {
    var providerItemID: String?
    var barcode: String?
    var matchedFoodName: String?
    var matchedBrand: String?
    var servingQuantity: Double?
    var servingUnit: String?
    var servingWeightGrams: Double?
    var householdServingDescription: String?
    var portionQuantity: Double?
    var portionUnit: String?
    var portionIsEstimated: Bool
    var matchConfidence: Double?
    var databaseDescription: String?

    init(
        providerItemID: String? = nil,
        barcode: String? = nil,
        matchedFoodName: String? = nil,
        matchedBrand: String? = nil,
        servingQuantity: Double? = nil,
        servingUnit: String? = nil,
        servingWeightGrams: Double? = nil,
        householdServingDescription: String? = nil,
        portionQuantity: Double? = nil,
        portionUnit: String? = nil,
        portionIsEstimated: Bool = false,
        matchConfidence: Double? = nil,
        databaseDescription: String? = nil
    ) {
        self.providerItemID = providerItemID
        self.barcode = barcode
        self.matchedFoodName = matchedFoodName
        self.matchedBrand = matchedBrand
        self.servingQuantity = servingQuantity
        self.servingUnit = servingUnit
        self.servingWeightGrams = servingWeightGrams
        self.householdServingDescription = householdServingDescription
        self.portionQuantity = portionQuantity
        self.portionUnit = portionUnit
        self.portionIsEstimated = portionIsEstimated
        self.matchConfidence = matchConfidence
        self.databaseDescription = databaseDescription
    }

    var portionSummary: String? {
        if let quantity = portionQuantity, let unit = portionUnit, !unit.isEmpty {
            return "\(NutritionNumberFormatter.quantity(quantity)) \(unit)"
        }
        if let servingQuantity, let servingUnit, !servingUnit.isEmpty {
            return "\(NutritionNumberFormatter.quantity(servingQuantity)) \(servingUnit)"
        }
        if let servingWeightGrams, servingWeightGrams > 0 {
            return "\(NutritionNumberFormatter.quantity(servingWeightGrams)) g"
        }
        return nil
    }
}

enum NutritionLookupSettings {
    static let exactLookupEnabledKey = "exactNutritionLookupEnabled"
    static let openFoodFactsEnabledKey = "openFoodFactsTextLookupEnabled"
    static let usdaAPIKeyKeychainKey = "nutrition.usdaFoodDataCentral.apiKey"

    static var exactLookupEnabled: Bool {
        get {
            if UserDefaults.standard.object(forKey: exactLookupEnabledKey) == nil { return true }
            return UserDefaults.standard.bool(forKey: exactLookupEnabledKey)
        }
        set { UserDefaults.standard.set(newValue, forKey: exactLookupEnabledKey) }
    }

    static var openFoodFactsEnabled: Bool {
        get {
            if UserDefaults.standard.object(forKey: openFoodFactsEnabledKey) == nil { return true }
            return UserDefaults.standard.bool(forKey: openFoodFactsEnabledKey)
        }
        set { UserDefaults.standard.set(newValue, forKey: openFoodFactsEnabledKey) }
    }

    static var usdaAPIKey: String? {
        get { KeychainHelper.load(key: usdaAPIKeyKeychainKey) }
        set {
            let trimmed = newValue?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if trimmed.isEmpty {
                KeychainHelper.delete(key: usdaAPIKeyKeychainKey)
            } else {
                KeychainHelper.save(key: usdaAPIKeyKeychainKey, value: trimmed)
            }
        }
    }
}

enum NutritionNumberFormatter {
    static func quantity(_ value: Double) -> String {
        if abs(value.rounded() - value) < 0.0001 {
            return String(Int(value.rounded()))
        }
        if abs(value) < 10 {
            return String(format: "%.2f", value)
                .replacingOccurrences(of: #"0+$"#, with: "", options: .regularExpression)
                .replacingOccurrences(of: #"\.$"#, with: "", options: .regularExpression)
        }
        return String(format: "%.1f", value)
            .replacingOccurrences(of: #"0+$"#, with: "", options: .regularExpression)
            .replacingOccurrences(of: #"\.$"#, with: "", options: .regularExpression)
    }
}
