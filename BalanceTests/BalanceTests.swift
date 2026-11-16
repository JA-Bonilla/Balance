import Foundation
import Testing
@testable import Balance

struct BalanceTests {
    @Test func balanceScore() {
        #expect(AppData.balance(growth: 0, fun: 0) == 0)
        #expect(AppData.balance(growth: 60, fun: 300) == 0)   // 1 growth : 5 fun is balanced
        #expect(AppData.balance(growth: 90, fun: 0) == 1)
        #expect(AppData.balance(growth: 0, fun: 45) == -1)
        // Agent's worked examples (weekly minutes).
        #expect(abs(AppData.balance(growth: 210, fun: 1260) - -0.0909) < 0.001)
        #expect(abs(AppData.balance(growth: 60, fun: 1500) - -0.6667) < 0.001)
        #expect(abs(AppData.balance(growth: 400, fun: 600) - 0.5385) < 0.001)
    }

    @Test func weeklyMinutesIgnoreOtherWeeksAndSides() {
        let week = DateInterval.thisWeek()
        let lastWeek = week.start.addingTimeInterval(-3600)
        let data = AppData(entries: [
            Entry(side: .growth, categoryID: "reading", minutes: 30, date: week.start.addingTimeInterval(60)),
            Entry(side: .growth, categoryID: "exercise", minutes: 20, date: week.start.addingTimeInterval(120)),
            Entry(side: .fun, categoryID: "social", minutes: 45, date: week.start.addingTimeInterval(60)),
            Entry(side: .growth, categoryID: "reading", minutes: 999, date: lastWeek),
        ])
        #expect(data.minutes(.growth, in: week) == 50)
        #expect(data.minutes(.fun, in: week) == 45)
        #expect(data.minutes(category: "reading", in: week) == 30)
    }

    @Test func v01FileMigrates() throws {
        let old = #"{"goals":[{"id":"27D35D15-0370-4BB3-A1AB-1796E951729F","categoryID":"reading","weeklyMinutes":60}],"entries":[],"healthEnabled":false}"#
        let data = try JSONDecoder.balance.decode(AppData.self, from: Data(old.utf8))
        #expect(data.goals.first?.minutesPerDay == 12)
        #expect(data.goals.first?.daysPerWeek == 5)
        #expect(data.hiddenHealthIDs.isEmpty)
    }

    @MainActor @Test func deletedHealthEntryStaysDeleted() throws {
        let store = Store(url: URL.temporaryDirectory.appending(path: "\(UUID().uuidString).json"))
        let sample = Entry(side: .growth, categoryID: "exercise", minutes: 30, date: .now, source: .health)
        let window = DateInterval(start: .now.addingTimeInterval(-60), duration: 120)
        store.replaceHealthEntries([sample], in: window)
        store.delete(sample)
        store.replaceHealthEntries([sample], in: window)
        #expect(store.data.entries.isEmpty)
    }

    @MainActor @Test func storeRoundTripAndCorruptFileIsKept() throws {
        let dir = URL.temporaryDirectory.appending(path: UUID().uuidString)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let url = dir.appending(path: "balance.json")

        let store = Store(url: url)
        store.upsert(Goal(categoryID: "reading", minutesPerDay: 15, daysPerWeek: 4))
        store.upsert(Entry(side: .fun, categoryID: "gaming", minutes: 15, date: .now))
        let reloaded = Store(url: url)
        #expect(reloaded.data.goals.first?.weeklyMinutes == 60)
        #expect(reloaded.data.entries.first?.minutes == 15)

        try Data("not json".utf8).write(to: url)
        let broken = Store(url: url)
        #expect(broken.error != nil)
        #expect(broken.data.goals.isEmpty)
        let files = try FileManager.default.contentsOfDirectory(atPath: dir.path)
        #expect(files.contains { $0.contains("unreadable") })
    }
}
