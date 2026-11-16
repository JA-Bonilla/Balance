import SwiftUI

/// Log or edit minutes. New entries default to the last-used side/category so a repeat log is 3 taps.
struct LogSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.dismiss) private var dismiss
    @AppStorage("lastSide") private var lastSide = Side.growth.rawValue
    @AppStorage("lastCategory") private var lastCategory = "reading"
    @State private var draft: Entry
    private let isNew: Bool

    init(entry: Entry?) {
        isNew = entry == nil
        let side = Side(rawValue: UserDefaults.standard.string(forKey: "lastSide") ?? "") ?? .growth
        let cat = UserDefaults.standard.string(forKey: "lastCategory") ?? "reading"
        _draft = State(initialValue: entry ?? Entry(side: side, categoryID: cat, minutes: 30, date: .now))
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Side", selection: $draft.side) {
                    ForEach(Side.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)
                .onChange(of: draft.side) { _, side in
                    if Category.with(id: draft.categoryID)?.side != side { draft.categoryID = Category.list(side)[0].id }
                }

                Picker("Activity", selection: $draft.categoryID) {
                    ForEach(Category.list(draft.side)) { Label($0.name, systemImage: $0.symbol).tag($0.id) }
                }

                Section("Time spent") {
                    MinutesPicker(minutes: $draft.minutes, color: .side(draft.side))
                }

                DatePicker("When", selection: $draft.date, in: ...Date.now)
            }
            .navigationTitle(isNew ? "Log time" : "Edit entry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        store.upsert(draft)
                        lastSide = draft.side.rawValue
                        lastCategory = draft.categoryID
                        dismiss()
                    }
                }
            }
        }
    }
}

/// Preset chips (15m … 4h) plus a 5-minute stepper for anything else.
struct MinutesPicker: View {
    @Binding var minutes: Int
    let color: Color
    static let presets = [15, 30, 60, 90, 120, 180, 240]

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
            ForEach(Self.presets, id: \.self) { m in
                Button { minutes = m } label: {
                    Text(m.minutesShort).lineLimit(1).minimumScaleFactor(0.7).frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(minutes == m ? color : .secondary)
                .accessibilityLabel(m.minutesText)
            }
        }
        Stepper(minutes.minutesText, value: $minutes, in: 1...1440, step: 5)
    }
}
