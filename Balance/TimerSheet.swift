import SwiftUI
import UserNotifications

/// Pick an activity and a length; the time is logged automatically when the countdown ends.
struct TimerSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.dismiss) private var dismiss
    let side: Side
    @State private var categoryID: String
    @State private var minutes = 30

    init(side: Side) {
        self.side = side
        let last = UserDefaults.standard.string(forKey: "lastCategory") ?? ""
        _categoryID = State(initialValue: Category.with(id: last)?.side == side ? last : Category.list(side)[0].id)
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Activity", selection: $categoryID) {
                    ForEach(Category.list(side)) { Label($0.name, systemImage: $0.symbol).tag($0.id) }
                }
                Section {
                    MinutesPicker(minutes: $minutes, color: .side(side))
                } header: {
                    Text("Timer length")
                } footer: {
                    Text("When the timer ends, the time is logged for you. Stop early to log just the time so far.")
                }
            }
            .navigationTitle("\(side.label) timer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Start") {
                        let timer = ActiveTimer(side: side, categoryID: categoryID, start: .now, minutes: minutes)
                        store.startTimer(timer)
                        Task { await TimerNotification.schedule(timer) }
                        dismiss()
                    }
                }
            }
        }
    }
}

/// Local "timer done" alert so the user knows even with the phone locked.
enum TimerNotification {
    static let id = "balance.timer"

    static func schedule(_ timer: ActiveTimer) async {
        let center = UNUserNotificationCenter.current()
        guard (try? await center.requestAuthorization(options: [.alert, .sound])) == true else { return }
        let content = UNMutableNotificationContent()
        content.title = "Timer done"
        content.body = "\(Category.with(id: timer.categoryID)?.name ?? "Activity"): \(timer.minutes.minutesText) logged."
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, timer.end.timeIntervalSinceNow), repeats: false)
        try? await center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
    }

    static func cancel() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
    }
}
