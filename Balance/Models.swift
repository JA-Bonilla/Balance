import Foundation

enum Side: String, Codable, CaseIterable, Identifiable {
    case growth, fun
    var id: String { rawValue }
    var label: String { self == .growth ? "Growth" : "Fun" }
}

enum Source: String, Codable {
    case manual, health
}

struct Category: Identifiable, Hashable {
    let id: String
    let name: String
    let symbol: String
    let side: Side
    let blurb: String

    static let growth: [Category] = [
        .init(id: "reading", name: "Reading", symbol: "book.fill", side: .growth, blurb: "Books, articles, long-form reading"),
        .init(id: "exercise", name: "Exercise", symbol: "figure.run", side: .growth, blurb: "Cardio, strength, mobility"),
        .init(id: "language", name: "Language", symbol: "character.bubble.fill", side: .growth, blurb: "Practicing a new language"),
        .init(id: "skill", name: "Skill Practice", symbol: "wrench.and.screwdriver.fill", side: .growth, blurb: "Deliberate practice on a craft"),
        .init(id: "meditation", name: "Meditation", symbol: "brain.head.profile", side: .growth, blurb: "Mindfulness and quiet focus"),
        .init(id: "journaling", name: "Journaling", symbol: "square.and.pencil", side: .growth, blurb: "Writing and reflecting on your day"),
        .init(id: "creative", name: "Creative Work", symbol: "paintbrush.pointed.fill", side: .growth, blurb: "Drawing, writing, making things"),
        .init(id: "courses", name: "Courses", symbol: "graduationcap.fill", side: .growth, blurb: "Structured learning and classes"),
        .init(id: "music", name: "Music Practice", symbol: "music.note", side: .growth, blurb: "Instrument practice and theory"),
        .init(id: "cooking", name: "Cooking", symbol: "frying.pan.fill", side: .growth, blurb: "Cooking from scratch, meal prep"),
        .init(id: "coding", name: "Coding", symbol: "chevron.left.forwardslash.chevron.right", side: .growth, blurb: "Programming and technical projects"),
        .init(id: "career", name: "Career & Finance", symbol: "chart.line.uptrend.xyaxis", side: .growth, blurb: "Career and money goals"),
    ]

    static let fun: [Category] = [
        .init(id: "social", name: "Social Media", symbol: "bubble.left.and.bubble.right.fill", side: .fun, blurb: "Scrolling and posting"),
        .init(id: "video", name: "Video", symbol: "play.tv.fill", side: .fun, blurb: "Short and long-form video"),
        .init(id: "gaming", name: "Gaming", symbol: "gamecontroller.fill", side: .fun, blurb: "Video and mobile games"),
        .init(id: "goingout", name: "Going Out", symbol: "party.popper.fill", side: .fun, blurb: "Parties, bars, nights out"),
        .init(id: "tv", name: "TV", symbol: "tv.fill", side: .fun, blurb: "Shows and movies"),
        .init(id: "browsing", name: "Browsing", symbol: "safari.fill", side: .fun, blurb: "Open-ended browsing"),
        .init(id: "listening", name: "Music & Podcasts", symbol: "headphones", side: .fun, blurb: "Listening for entertainment"),
    ]

    static let all = growth + fun
    static func with(id: String) -> Category? { all.first { $0.id == id } }
    static func list(_ side: Side) -> [Category] { side == .growth ? growth : fun }
}

struct Goal: Codable, Identifiable, Hashable {
    var id = UUID()
    var categoryID: String
    var weeklyMinutes: Int
}

struct Entry: Codable, Identifiable, Hashable {
    var id = UUID()
    var side: Side
    var categoryID: String
    var minutes: Int
    var date: Date
    var source: Source = .manual
}

struct AppData: Codable {
    var goals: [Goal] = []
    var entries: [Entry] = []
    var healthEnabled = false

    func minutes(_ side: Side, in interval: DateInterval) -> Int {
        entries.filter { $0.side == side && interval.contains($0.date) }.reduce(0) { $0 + $1.minutes }
    }

    func minutes(category: String, in interval: DateInterval) -> Int {
        entries.filter { $0.categoryID == category && interval.contains($0.date) }.reduce(0) { $0 + $1.minutes }
    }

    /// -1 = all fun, 0 = even, +1 = all growth.
    static func balance(growth: Int, fun: Int) -> Double {
        let total = growth + fun
        return total == 0 ? 0 : Double(growth - fun) / Double(total)
    }
}

extension DateInterval {
    static func thisWeek(_ now: Date = .now, calendar: Calendar = .current) -> DateInterval {
        calendar.dateInterval(of: .weekOfYear, for: now)!
    }
}
