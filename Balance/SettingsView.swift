import SwiftUI

struct SettingsView: View {
    @Environment(Store.self) private var store
    @State private var editingGoal: Goal?
    @State private var addingActivity = false
    @AppStorage(TimerSound.key) private var timerSound = TimerSound.chime.rawValue

    var body: some View {
        @Bindable var store = store
        Form {
            Section("Goals") {
                ForEach(store.data.goals) { goal in
                    let c = store.data.category(goal.categoryID)
                    Button { editingGoal = goal } label: {
                        LabeledContent {
                            Text("\(goal.minutesPerDay) min × \(goal.daysPerWeek)d")
                        } label: {
                            Label(c?.name ?? goal.categoryID, systemImage: c?.symbol ?? "circle")
                        }
                    }
                    .foregroundStyle(.primary)
                }
                .onDelete { offsets in
                    // Keep at least one goal; deleting the last would drop the user back into onboarding.
                    guard store.data.goals.count > offsets.count else { return }
                    offsets.map { store.data.goals[$0] }.forEach(store.delete)
                }
                Button("Add goal", systemImage: "plus") {
                    let used = Set(store.data.goals.map(\.categoryID))
                    editingGoal = Goal(categoryID: store.data.categories(.growth).first { !used.contains($0.id) }?.id ?? "reading", minutesPerDay: 15, daysPerWeek: 5)
                }
            }
            .listRowBackground(Color.surface)

            Section {
                ForEach(store.data.customCategories.filter { !$0.archived }) { c in
                    Label {
                        LabeledContent(c.name, value: c.side.label)
                    } icon: {
                        Image(systemName: c.symbol).foregroundStyle(Color.side(c.side))
                    }
                    .swipeActions {
                        Button("Remove", systemImage: "trash", role: .destructive) { store.archive(c) }
                            .tint(.red)
                    }
                }
                Button("Add activity", systemImage: "plus") { addingActivity = true }
            } header: {
                Text("Custom activities")
            } footer: {
                Text("Removing an activity hides it from the lists; time you already logged keeps it.")
            }
            .listRowBackground(Color.surface)

            Section("Timer") {
                Picker("Alarm sound", selection: $timerSound) {
                    ForEach(TimerSound.allCases) { Text($0.label).tag($0.rawValue) }
                }
                .onChange(of: timerSound) { _, new in TimerSound(rawValue: new)?.preview() }
            }
            .listRowBackground(Color.surface)

            Section {
                Toggle("Count Health workouts & mindful minutes", isOn: Binding(
                    get: { store.data.healthEnabled },
                    set: { on in Task { await setHealth(on) } }
                ))
                .disabled(!HealthSync.isAvailable)
            } header: {
                Text("Data sources")
            } footer: {
                Text("Workouts count as Exercise and mindful minutes as Meditation. Screen Time tracking needs a paid Apple Developer account and isn't available yet.")
            }
            .listRowBackground(Color.surface)

            Section {
                Text("Your data is a single file, balance.json, in Files › On My iPhone › Balance. Nothing leaves your phone.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
            .listRowBackground(Color.surface)
        }
        .navigationTitle("Settings")
        .charcoalBackground()
        .sheet(item: $editingGoal) { GoalSheet(goal: $0) }
        .sheet(isPresented: $addingActivity) { ActivitySheet() }
    }

    private func setHealth(_ on: Bool) async {
        if on {
            do { try await HealthSync.requestAccess() } catch {
                store.error = "Couldn't get Health access: \(error.localizedDescription)"
                return
            }
        }
        store.data.healthEnabled = on
        if !on { store.data.entries.removeAll { $0.source == .health } }
        store.save()
    }
}

struct GoalSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State var goal: Goal

    var body: some View {
        NavigationStack {
            Form {
                ActivityPicker(side: .growth, selection: $goal.categoryID)
                TargetFields(perDay: $goal.minutesPerDay, days: $goal.daysPerWeek)
            }
            .navigationTitle("Goal")
            .charcoalBackground()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { store.upsert(goal); dismiss() } }
            }
        }
        .presentationDetents([.medium])
    }
}

/// Create a custom growth or fun activity with a name and icon.
struct ActivitySheet: View {
    @Environment(Store.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var side = Side.growth
    @State private var symbol = Category.customSymbols[0]

    private var trimmed: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Side", selection: $side) {
                    ForEach(Side.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)

                TextField("Name, e.g. Gardening", text: $name)
                    .onChange(of: name) { _, new in if new.count > 30 { name = String(new.prefix(30)) } }

                Section("Icon") {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 12) {
                        ForEach(Category.customSymbols, id: \.self) { s in
                            Button { symbol = s } label: {
                                Image(systemName: s)
                                    .font(.title3)
                                    .frame(width: 44, height: 44)
                                    .foregroundStyle(symbol == s ? Color.white : Color.side(side))
                                    .background(symbol == s ? Color.side(side) : Color.clear, in: .circle)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(s.replacingOccurrences(of: ".fill", with: "").replacingOccurrences(of: ".", with: " "))
                            .accessibilityAddTraits(symbol == s ? .isSelected : [])
                        }
                    }
                    .padding(.vertical, 4)
                }
                .listRowBackground(Color.surface)
            }
            .navigationTitle("New Activity")
            .charcoalBackground()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        store.add(Category(id: "custom-\(UUID().uuidString)", name: trimmed, symbol: symbol, side: side))
                        dismiss()
                    }
                    .disabled(trimmed.isEmpty)
                }
            }
        }
        .tint(Color.side(side))
    }
}
