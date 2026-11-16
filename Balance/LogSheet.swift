import SwiftUI

/// Log or edit time. New entries default to the last-used side/category so a repeat log is 3 taps.
struct LogSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.dismiss) private var dismiss
    @AppStorage("lastSide") private var lastSide = Side.growth.rawValue
    @AppStorage("lastCategory") private var lastCategory = "reading"
    @State private var draft: Entry
    @State private var custom = false
    // Start on 5-minute marks so the values match what the 5-minute pickers display.
    @State private var end = Date(timeIntervalSince1970: (Date.now.timeIntervalSince1970 / 300).rounded(.down) * 300)
    @State private var start = Date(timeIntervalSince1970: (Date.now.timeIntervalSince1970 / 300).rounded(.down) * 300 - 3600)
    private let isNew: Bool

    init(entry: Entry?) {
        isNew = entry == nil
        let side = Side(rawValue: UserDefaults.standard.string(forKey: "lastSide") ?? "") ?? .growth
        let cat = UserDefaults.standard.string(forKey: "lastCategory") ?? "reading"
        _draft = State(initialValue: entry ?? Entry(side: side, categoryID: cat, minutes: 30, date: .now))
    }

    private var customMinutes: Int { AppData.roundedMinutes(from: start, to: end) }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Side", selection: $draft.side) {
                    ForEach(Side.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)
                .onChange(of: draft.side) { _, side in
                    if store.data.category(draft.categoryID)?.side != side { draft.categoryID = store.data.categories(side)[0].id }
                }

                ActivityPicker(side: draft.side, selection: $draft.categoryID)

                Section {
                    MinutesPicker(minutes: $draft.minutes, color: .side(draft.side), custom: $custom)
                    if custom {
                        DatePicker("Start", selection: $start, in: ...Date.now)
                        DatePicker("End", selection: $end, in: start...Date.now)
                        LabeledContent("Time spent", value: customMinutes > 0 ? customMinutes.minutesText : "—")
                    }
                } header: {
                    Text("Time spent")
                } footer: {
                    if custom { Text("Rounded to the nearest 5 minutes.") }
                }

                if !custom {
                    DatePicker("When", selection: $draft.date, in: ...Date.now)
                }
            }
            .navigationTitle(isNew ? "Log Time" : "Edit Entry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if custom {
                            draft.minutes = customMinutes
                            draft.date = start
                        }
                        store.upsert(draft)
                        lastSide = draft.side.rawValue
                        lastCategory = draft.categoryID
                        dismiss()
                    }
                    .disabled(custom && customMinutes <= 0)
                }
            }
        }
    }
}

/// Built-in and custom activities for one side; keeps an archived current choice visible when editing.
struct ActivityPicker: View {
    @Environment(Store.self) private var store
    let side: Side
    @Binding var selection: String

    var body: some View {
        var options = store.data.categories(side)
        if !options.contains(where: { $0.id == selection }), let current = store.data.category(selection) {
            options.append(current)
        }
        return Picker("Activity", selection: $selection) {
            ForEach(options) { Label($0.name, systemImage: $0.symbol).tag($0.id) }
        }
    }
}

/// Preset chips (15m … 4h) plus a 5-minute stepper; optionally a "Custom" chip the caller handles.
struct MinutesPicker: View {
    @Binding var minutes: Int
    let color: Color
    var custom: Binding<Bool>?
    static let presets = [15, 30, 60, 90, 120, 180, 240]

    var body: some View {
        let isCustom = custom?.wrappedValue == true
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
            ForEach(Self.presets, id: \.self) { m in
                chip(m.minutesShort, selected: !isCustom && minutes == m) {
                    minutes = m
                    custom?.wrappedValue = false
                }
                .accessibilityLabel(m.minutesText)
            }
            if let custom {
                chip("Custom", selected: isCustom) { custom.wrappedValue = true }
                    .accessibilityLabel("Custom start and end time")
            }
        }
        if !isCustom {
            Stepper(minutes.minutesText, value: $minutes, in: 1...1440, step: 5)
        }
    }

    private func chip(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).lineLimit(1).minimumScaleFactor(0.7).frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)
        .tint(selected ? color : .secondary)
    }
}
