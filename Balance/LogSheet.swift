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

                Section("Minutes") {
                    HStack {
                        ForEach([15, 30, 60], id: \.self) { m in
                            Button("\(m)") { draft.minutes = m }
                                .buttonStyle(.bordered)
                                .tint(draft.minutes == m ? Color.side(draft.side) : .secondary)
                        }
                    }
                    Stepper("\(draft.minutes) min", value: $draft.minutes, in: 1...1440, step: 5)
                }

                DatePicker("When", selection: $draft.date, in: ...Date.now)
            }
            .navigationTitle(isNew ? "Log minutes" : "Edit entry")
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
