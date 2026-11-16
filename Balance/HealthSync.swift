import Foundation
import HealthKit

/// Imports workouts (as Exercise) and mindful sessions (as Meditation) as growth minutes.
enum HealthSync {
    static let store = HKHealthStore()
    static var isAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    static func requestAccess() async throws {
        try await store.requestAuthorization(toShare: [], read: [.workoutType(), HKCategoryType(.mindfulSession)])
    }

    /// shortcut: imports the last 28 days only; widen if users want older history.
    static func fetch(days: Int = 28) async throws -> (entries: [Entry], interval: DateInterval) {
        let end = Date.now
        let start = Calendar.current.date(byAdding: .day, value: -days, to: Calendar.current.startOfDay(for: end))!
        let interval = DateInterval(start: start, end: end)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end)

        let workouts = try await HKSampleQueryDescriptor(predicates: [.workout(predicate)], sortDescriptors: []).result(for: store)
        let mindful = try await HKSampleQueryDescriptor(predicates: [.categorySample(type: HKCategoryType(.mindfulSession), predicate: predicate)], sortDescriptors: []).result(for: store)

        let entries =
            workouts.map { entry(for: $0, category: "exercise") } +
            mindful.map { entry(for: $0, category: "meditation") }
        return (entries.filter { $0.minutes > 0 }, interval)
    }

    private static func entry(for sample: HKSample, category: String) -> Entry {
        Entry(id: sample.uuid, side: .growth, categoryID: category,
              minutes: Int((sample.endDate.timeIntervalSince(sample.startDate) / 60).rounded()),
              date: sample.startDate, source: .health)
    }
}
