import SwiftUI

struct HomeView: View {
    @Environment(Store.self) private var store
    @State private var editing: Entry?
    @State private var logging = false
    @State private var timerSide: Side?
    @State private var skyScroll: CGFloat = 0 // how far the list has scrolled from rest
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        let week = DateInterval.thisWeek()
        let data = store.data
        NavigationStack {
            GeometryReader { geo in
            let skyHeight = geo.size.width * 380 / 342
            List {
                // The sky is a backdrop behind the list (see .background below); this row only reserves its space.
                Section {
                    Color.clear
                        .frame(height: max(0, skyHeight - geo.safeAreaInsets.top - 56))
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                    BalanceView(growth: data.minutes(.growth, in: week), fun: data.minutes(.fun, in: week))
                        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 12, trailing: 20))
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                }

                Section {
                    if let t = data.activeTimer {
                        RunningTimerRow(timer: t) {
                            store.stopTimer()
                            TimerNotification.cancel()
                        }
                    } else {
                        HStack {
                            TimerButton(side: .fun) { timerSide = .fun }
                            Spacer()
                            TimerButton(side: .growth) { timerSide = .growth }
                        }
                    }
                }
                .listRowBackground(Color.surface)

                Section("Goals") {
                    ForEach(data.goals) { goal in
                        let c = data.category(goal.categoryID)
                        let done = data.minutes(category: goal.categoryID, in: week)
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Label(c?.name ?? goal.categoryID, systemImage: c?.symbol ?? "circle")
                                Spacer()
                                Text("\(done.minutesShort) / \(goal.weeklyMinutes.minutesShort)").foregroundStyle(.secondary).monospacedDigit()
                            }
                            ProgressView(value: min(Double(done), Double(goal.weeklyMinutes)), total: Double(goal.weeklyMinutes))
                                .tint(.growth)
                        }
                    }
                }
                .listRowBackground(Color.surface)

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
                .listRowBackground(Color.surface)
            }
            .navigationBarTitleDisplayMode(.inline)
            .scrollContentBackground(.hidden)
            .scrollEdgeEffectHidden(true, for: .top) // no blurry bar over the sky
            .toolbarBackground(.hidden, for: .navigationBar)
            .background(alignment: .top) {
                // Full-bleed sky from the very top of the screen; the list scrolls over it.
                SkyBackdrop(growth: data.minutes(.growth, in: week), fun: data.minutes(.fun, in: week), stage: data.bonsiStage)
                    .offset(y: -max(skyScroll, 0)) // the sky and Bonsi scroll away with the list
                    .ignoresSafeArea(edges: .top)
            }
            .background(Color.appBackground)
            // At rest contentOffset.y == -contentInsets.top (status bar + nav bar), so the sum is the distance scrolled.
            .onScrollGeometryChange(for: CGFloat.self) { $0.contentOffset.y + $0.contentInsets.top } action: { _, distance in
                skyScroll = distance
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    NavigationLink { SettingsView() } label: { ToolbarIcon(systemName: "gearshape") }
                        .accessibilityLabel("Settings")
                }
                .sharedBackgroundVisibility(.hidden)
                ToolbarItem(placement: .topBarTrailing) {
                    Button { logging = true } label: { ToolbarIcon(systemName: "plus") }
                        .accessibilityLabel("Log minutes")
                }
                .sharedBackgroundVisibility(.hidden)
            }
            .sheet(isPresented: $logging) { LogSheet(entry: nil) }
            .sheet(item: $editing) { LogSheet(entry: $0) }
            .sheet(item: $timerSide) { TimerSheet(side: $0, data: store.data) }
            .refreshable { await syncHealth() }
            .task { await syncHealth() }
            .task(id: data.activeTimer) {
                // Log the timer the moment it ends while the app is open.
                guard let t = data.activeTimer else { return }
                try? await Task.sleep(for: .seconds(max(0, t.end.timeIntervalSinceNow)))
                if !Task.isCancelled { store.finishTimerIfDue() }
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active { store.finishTimerIfDue() }
            }
            }
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
    @Environment(Store.self) private var store
    let entry: Entry

    var body: some View {
        let c = store.data.category(entry.categoryID)
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
            Text(entry.minutes.minutesText).monospacedDigit()
        }
    }
}

struct TimerButton: View {
    let side: Side
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: "timer").foregroundStyle(Color.side(side))
                Text("\(side.label) Timer").foregroundStyle(Color.side(side))
            }
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, 12).padding(.vertical, 8)
            .background(.fill.tertiary, in: .capsule)
        }
        .buttonStyle(.borderless) // keeps the two buttons in one list row independently tappable
    }
}

struct RunningTimerRow: View {
    @Environment(Store.self) private var store
    let timer: ActiveTimer
    let stop: () -> Void

    var body: some View {
        let c = store.data.category(timer.categoryID)
        HStack {
            Image(systemName: "timer").foregroundStyle(Color.side(timer.side))
            VStack(alignment: .leading) {
                Text(c?.name ?? timer.categoryID)
                Text("\(timer.minutes.minutesText) timer").font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Text(timerInterval: timer.start...timer.end, countsDown: true)
                .font(.title3.monospacedDigit())
                .frame(minWidth: 70, alignment: .trailing)
            Button("Stop", action: stop)
                .buttonStyle(.bordered)
                .accessibilityLabel("Stop timer and log time so far")
        }
    }
}

/// Toolbar button over the sky: white glyph on a light translucent disc (no heavy glass or black).
struct ToolbarIcon: View {
    let systemName: String
    var body: some View {
        Image(systemName: systemName)
            .font(.body.weight(.semibold))
            .foregroundStyle(.white)
            .frame(width: 40, height: 40)
            .background(.black.opacity(0.22), in: .circle)
            .overlay(Circle().stroke(.white.opacity(0.22), lineWidth: 1))
    }
}
