import SwiftUI

struct SettingsView: View {
    @Environment(Store.self) private var store
    @State private var editingGoal: Goal?

    var body: some View {
        @Bindable var store = store
        Form {
            Section("Goals") {
                ForEach(store.data.goals) { goal in
                    let c = Category.with(id: goal.categoryID)
                    Button { editingGoal = goal } label: {
                        LabeledContent {
                            Text("\(goal.weeklyMinutes) min / week")
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
                    editingGoal = Goal(categoryID: Category.growth.first { !used.contains($0.id) }?.id ?? "reading", weeklyMinutes: 60)
                }
            }

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

            Section {
                Text("Your data is a single file, balance.json, in Files › On My iPhone › Balance. Nothing leaves your phone.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Settings")
        .sheet(item: $editingGoal) { GoalSheet(goal: $0) }
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
                Picker("Area", selection: $goal.categoryID) {
                    ForEach(Category.growth) { Label($0.name, systemImage: $0.symbol).tag($0.id) }
                }
                Stepper("\(goal.weeklyMinutes) min / week", value: $goal.weeklyMinutes, in: 15...1200, step: 15)
            }
            .navigationTitle("Goal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { store.upsert(goal); dismiss() } }
            }
        }
        .presentationDetents([.medium])
    }
}
