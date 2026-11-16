import SwiftUI

struct HomeView: View {
    @Environment(Store.self) private var store
    @State private var editing: Entry?
    @State private var logging = false

    var body: some View {
        let week = DateInterval.thisWeek()
        let data = store.data
        NavigationStack {
            List {
                Section {
                    BalanceView(growth: data.minutes(.growth, in: week), fun: data.minutes(.fun, in: week))
                        .padding(.vertical, 8)
                } header: {
                    Text("This week")
                }

                Section("Goals") {
                    ForEach(data.goals) { goal in
                        let c = Category.with(id: goal.categoryID)
                        let done = data.minutes(category: goal.categoryID, in: week)
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Label(c?.name ?? goal.categoryID, systemImage: c?.symbol ?? "circle")
                                Spacer()
                                Text("\(done) / \(goal.weeklyMinutes) min").foregroundStyle(.secondary).monospacedDigit()
                            }
                            ProgressView(value: min(Double(done), Double(goal.weeklyMinutes)), total: Double(goal.weeklyMinutes))
                                .tint(.growth)
                        }
                    }
                }

                Section("Recent") {
                    let recent = data.entries.sorted { $0.date > $1.date }.prefix(20)
                    if recent.isEmpty {
                        Text("Nothing logged yet. Tap + to log minutes.").foregroundStyle(.secondary)
                    }
                    ForEach(recent) { entry in
                        EntryRow(entry: entry)
                            .contentShape(.rect)
                            .onTapGesture { if entry.source == .manual { editing = entry } }
                            .swipeActions(edge: .trailing) {
                                Button("Delete", systemImage: "trash", role: .destructive) { store.delete(entry) }
                                    .tint(.red) // the app-wide accent tint would otherwise recolor it
                            }
                    }
                }
            }
            .navigationTitle("Balance")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    NavigationLink { SettingsView() } label: { Image(systemName: "gearshape") }
                        .accessibilityLabel("Settings")
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { logging = true } label: { Image(systemName: "plus") }
                        .accessibilityLabel("Log minutes")
                }
            }
            .sheet(isPresented: $logging) { LogSheet(entry: nil) }
            .sheet(item: $editing) { LogSheet(entry: $0) }
            .refreshable { await syncHealth() }
            .task { await syncHealth() }
        }
    }

    private func syncHealth() async {
        guard store.data.healthEnabled, HealthSync.isAvailable else { return }
        do {
            let (entries, interval) = try await HealthSync.fetch()
            store.replaceHealthEntries(entries, in: interval)
        } catch {
            store.error = "Couldn't read Health: \(error.localizedDescription)"
        }
    }
}

struct EntryRow: View {
    let entry: Entry

    var body: some View {
        let c = Category.with(id: entry.categoryID)
        HStack {
            Image(systemName: c?.symbol ?? "circle").foregroundStyle(Color.side(entry.side)).frame(width: 28)
            VStack(alignment: .leading) {
                Text(c?.name ?? entry.categoryID)
                Text(entry.date, format: .dateTime.weekday().hour().minute()).font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            if entry.source == .health {
                Image(systemName: "heart.fill").font(.caption).foregroundStyle(.secondary).accessibilityLabel("From Health")
            }
            Text("\(entry.minutes) min").monospacedDigit()
        }
    }
}
