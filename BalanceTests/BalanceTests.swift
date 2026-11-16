import Foundation
import Testing
import UIKit
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

    @Test func minutesFormatting() {
        #expect(45.minutesText == "45 min")
        #expect(60.minutesText == "1 hr")
        #expect(90.minutesText == "1 hr 30 min")
        #expect(240.minutesShort == "4h")
        #expect(90.minutesShort == "1h 30m")
    }

    @MainActor @Test func timerLogsFullEarlyOrNothing() {
        let store = Store(url: URL.temporaryDirectory.appending(path: "\(UUID().uuidString).json"))
        let start = Date(timeIntervalSince1970: 1_000_000)

        // Ran out while the app was closed: logs the full planned length.
        store.startTimer(ActiveTimer(side: .growth, categoryID: "reading", start: start, minutes: 30))
        store.finishTimerIfDue(now: start.addingTimeInterval(3600))
        #expect(store.data.activeTimer == nil)
        #expect(store.data.entries.last?.minutes == 30)
        #expect(store.data.entries.last?.date == start)

        // Not due yet: keeps running.
        store.startTimer(ActiveTimer(side: .fun, categoryID: "gaming", start: start, minutes: 60))
        store.finishTimerIfDue(now: start.addingTimeInterval(600))
        #expect(store.data.activeTimer != nil)

        // Stopped early: logs time so far.
        store.stopTimer(at: start.addingTimeInterval(20 * 60))
        #expect(store.data.entries.last?.minutes == 20)

        // Stopped within seconds: logs nothing.
        store.startTimer(ActiveTimer(side: .fun, categoryID: "gaming", start: start, minutes: 60))
        store.stopTimer(at: start.addingTimeInterval(10))
        #expect(store.data.entries.count == 2)
    }

    @Test func customTimeRoundsToNearestFive() {
        let s = Date(timeIntervalSince1970: 0)
        #expect(AppData.roundedMinutes(from: s, to: s.addingTimeInterval(92 * 60)) == 90)
        #expect(AppData.roundedMinutes(from: s, to: s.addingTimeInterval(93 * 60)) == 95)
        #expect(AppData.roundedMinutes(from: s, to: s.addingTimeInterval(2 * 60)) == 0)
    }

    @MainActor @Test func customActivitiesListAndArchive() {
        let store = Store(url: URL.temporaryDirectory.appending(path: "\(UUID().uuidString).json"))
        let garden = Category(id: "custom-1", name: "Gardening", symbol: "leaf.fill", side: .growth)
        store.add(garden)
        #expect(store.data.categories(.growth).contains(garden))
        #expect(!store.data.categories(.fun).contains(garden))
        store.archive(garden)
        #expect(!store.data.categories(.growth).contains { $0.id == "custom-1" })
        #expect(store.data.category("custom-1")?.name == "Gardening") // old entries still resolve
    }

    @Test func allIconsAndSoundsExist() {
        for s in Category.customSymbols + Category.all.map(\.symbol) {
            #expect(UIImage(systemName: s) != nil, "missing SF Symbol \(s)")
        }
        for sound in TimerSound.allCases where sound != .standard {
            #expect(Bundle.main.url(forResource: sound.rawValue, withExtension: "wav") != nil, "missing \(sound.rawValue).wav")
        }
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
