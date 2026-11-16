import Foundation
import Testing
@testable import Balance

struct BalanceTests {
    @Test func balanceScore() {
        #expect(AppData.balance(growth: 0, fun: 0) == 0)
        #expect(AppData.balance(growth: 60, fun: 60) == 0)
        #expect(AppData.balance(growth: 90, fun: 0) == 1)
        #expect(AppData.balance(growth: 0, fun: 45) == -1)
        #expect(AppData.balance(growth: 30, fun: 90) == -0.5)
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

    @MainActor @Test func storeRoundTripAndCorruptFileIsKept() throws {
        let dir = URL.temporaryDirectory.appending(path: UUID().uuidString)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let url = dir.appending(path: "balance.json")

        let store = Store(url: url)
        store.upsert(Goal(categoryID: "reading", weeklyMinutes: 60))
        store.upsert(Entry(side: .fun, categoryID: "gaming", minutes: 15, date: .now))
        let reloaded = Store(url: url)
        #expect(reloaded.data.goals.count == 1)
        #expect(reloaded.data.entries.first?.minutes == 15)

        try Data("not json".utf8).write(to: url)
        let broken = Store(url: url)
        #expect(broken.error != nil)
        #expect(broken.data.goals.isEmpty)
        let files = try FileManager.default.contentsOfDirectory(atPath: dir.path)
        #expect(files.contains { $0.contains("unreadable") })
    }
}
