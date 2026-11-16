import Foundation

enum Side: String, Codable, CaseIterable, Identifiable {
    case growth, fun
    var id: String { rawValue }
    var label: String { self == .growth ? "Growth" : "Fun" }
}

enum Source: String, Codable {
    case manual, health
}

struct Category: Identifiable, Hashable, Codable {
    var id: String
    var name: String
    var symbol: String
    var side: Side
    var blurb: String = ""
    /// Removed custom activities stay stored so old entries still show their name and icon.
    var archived = false

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
    static func list(_ side: Side) -> [Category] { side == .growth ? growth : fun }

    /// Icons offered for custom activities.
    static let customSymbols = [
        "star.fill", "heart.fill", "leaf.fill", "flame.fill", "bolt.fill", "sun.max.fill",
        "moon.fill", "figure.walk", "figure.yoga", "dumbbell.fill", "bicycle", "tennisball.fill",
        "soccerball", "basketball.fill", "book.closed.fill", "pencil", "paintpalette.fill", "camera.fill",
        "guitars.fill", "pianokeys", "globe", "laptopcomputer", "gamecontroller.fill", "tv.fill",
        "film.fill", "popcorn.fill", "cup.and.saucer.fill", "wineglass.fill", "fork.knife", "cart.fill",
        "car.fill", "airplane", "pawprint.fill", "person.2.fill", "bubble.left.fill", "music.mic",
    ]
}

struct Goal: Codable, Identifiable, Hashable {
    var id = UUID()
    var categoryID: String
    var minutesPerDay: Int
    var daysPerWeek: Int
    var weeklyMinutes: Int { minutesPerDay * daysPerWeek }
}

extension Goal {
    private enum LegacyKeys: String, CodingKey { case weeklyMinutes }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(UUID.self, forKey: .id)
        categoryID = try c.decode(String.self, forKey: .categoryID)
        if let perDay = try c.decodeIfPresent(Int.self, forKey: .minutesPerDay) {
            minutesPerDay = perDay
            daysPerWeek = try c.decode(Int.self, forKey: .daysPerWeek)
        } else {
            // v0.1 files stored only a weekly total; spread it over 5 days.
            let weekly = try decoder.container(keyedBy: LegacyKeys.self).decode(Int.self, forKey: .weeklyMinutes)
            daysPerWeek = 5
            minutesPerDay = max(1, (weekly + 4) / 5)
        }
    }
}

struct Entry: Codable, Identifiable, Hashable {
    var id = UUID()
    var side: Side
    var categoryID: String
    var minutes: Int
    var date: Date
    var source: Source = .manual
}

/// A running countdown; becomes an Entry when it ends or is stopped.
struct ActiveTimer: Codable, Hashable {
    var side: Side
    var categoryID: String
    var start: Date
    var minutes: Int
    var end: Date { start.addingTimeInterval(Double(minutes) * 60) }
}

struct AppData: Codable {
    var goals: [Goal] = []
    var entries: [Entry] = []
    var healthEnabled = false
    /// Health samples the user deleted, so re-imports skip them.
    var hiddenHealthIDs: Set<UUID> = []
    var activeTimer: ActiveTimer?
    var customCategories: [Category] = []

    /// Built-in plus the user's active custom activities for one side.
    func categories(_ side: Side) -> [Category] {
        Category.list(side) + customCategories.filter { $0.side == side && !$0.archived }
    }

    /// Any category by id, including archived custom ones.
    func category(_ id: String) -> Category? {
        Category.all.first { $0.id == id } ?? customCategories.first { $0.id == id }
    }

    func minutes(_ side: Side, in interval: DateInterval) -> Int {
        entries.filter { $0.side == side && interval.contains($0.date) }.reduce(0) { $0 + $1.minutes }
    }

    func minutes(category: String, in interval: DateInterval) -> Int {
        entries.filter { $0.categoryID == category && interval.contains($0.date) }.reduce(0) { $0 + $1.minutes }
    }

    /// Growth minutes per fun minute that counts as balanced. Typical adults run ~5–10 fun : 1 growth.
    static let growthPerFun = 0.2

    /// -1 = all fun, 0 = balanced (growth ≈ 20% of fun), +1 = all growth.
    static func balance(growth: Int, fun: Int) -> Double {
        let g = Double(growth), target = growthPerFun * Double(fun)
        return g + target == 0 ? 0 : (g - target) / (g + target)
    }
}

extension AppData {
    /// Missing keys fall back to defaults, so files from older versions still load.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        goals = try c.decodeIfPresent([Goal].self, forKey: .goals) ?? []
        entries = try c.decodeIfPresent([Entry].self, forKey: .entries) ?? []
        healthEnabled = try c.decodeIfPresent(Bool.self, forKey: .healthEnabled) ?? false
        hiddenHealthIDs = try c.decodeIfPresent(Set<UUID>.self, forKey: .hiddenHealthIDs) ?? []
        activeTimer = try c.decodeIfPresent(ActiveTimer.self, forKey: .activeTimer)
        customCategories = try c.decodeIfPresent([Category].self, forKey: .customCategories) ?? []
    }

    /// Minutes between two times, rounded to the nearest 5.
    static func roundedMinutes(from start: Date, to end: Date) -> Int {
        Int((end.timeIntervalSince(start) / 300).rounded()) * 5
    }
}

extension Int {
    /// Minutes as "45 min", "2 hr", "1 hr 30 min".
    var minutesText: String {
        let h = self / 60, m = self % 60
        return h == 0 ? "\(m) min" : m == 0 ? "\(h) hr" : "\(h) hr \(m) min"
    }

    /// Compact form for chips and labels: "45m", "2h", "1h 30m".
    var minutesShort: String {
        let h = self / 60, m = self % 60
        return h == 0 ? "\(m)m" : m == 0 ? "\(h)h" : "\(h)h \(m)m"
    }
}

extension DateInterval {
    static func thisWeek(_ now: Date = .now, calendar: Calendar = .current) -> DateInterval {
        calendar.dateInterval(of: .weekOfYear, for: now)!
    }
}
