import AVFoundation
import SwiftUI
import UserNotifications

/// Pick an activity and a length; the time is logged automatically when the countdown ends.
struct TimerSheet: View {
    @Environment(Store.self) private var store
    @Environment(\.dismiss) private var dismiss
    let side: Side
    @State private var categoryID: String
    @State private var minutes = 30

    init(side: Side, data: AppData) {
        self.side = side
        let last = UserDefaults.standard.string(forKey: "lastCategory") ?? ""
        let options = data.categories(side)
        _categoryID = State(initialValue: options.contains { $0.id == last } ? last : options[0].id)
    }

    var body: some View {
        NavigationStack {
            Form {
                ActivityPicker(side: side, selection: $categoryID)
                Section {
                    MinutesPicker(minutes: $minutes, color: .side(side))
                } header: {
                    Text("Timer length")
                } footer: {
                    Text("When the timer ends, the time is logged for you. Stop early to log just the time so far.")
                }
            }
            .navigationTitle("\(side.label) Timer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Start") {
                        let timer = ActiveTimer(side: side, categoryID: categoryID, start: .now, minutes: minutes)
                        let name = store.data.category(categoryID)?.name ?? "Activity"
                        store.startTimer(timer)
                        Task { await TimerNotification.schedule(timer, activityName: name) }
                        dismiss()
                    }
                }
            }
        }
    }
}

/// Sounds bundled in Balance/Sounds (iOS only allows bundled files, max 30 s, for notification sounds).
enum TimerSound: String, CaseIterable, Identifiable {
    case standard = "default", chime, bell, pulse, gentle
    var id: String { rawValue }
    var label: String { self == .standard ? "iOS Default" : rawValue.capitalized }
    static let key = "timerSound"
    static var current: TimerSound { TimerSound(rawValue: UserDefaults.standard.string(forKey: key) ?? "") ?? .chime }

    var notificationSound: UNNotificationSound {
        self == .standard ? .default : UNNotificationSound(named: UNNotificationSoundName("\(rawValue).wav"))
    }

    @MainActor private static var player: AVAudioPlayer?

    /// Plays the sound in-app so the user can hear it while choosing.
    @MainActor func preview() {
        if self == .standard {
            AudioServicesPlaySystemSound(1007) // the stock tri-tone notification sound
            return
        }
        guard let url = Bundle.main.url(forResource: rawValue, withExtension: "wav") else { return }
        Self.player = try? AVAudioPlayer(contentsOf: url)
        Self.player?.play()
    }
}

/// Local "timer done" alert so the user knows even with the phone locked.
enum TimerNotification {
    static let id = "balance.timer"

    static func schedule(_ timer: ActiveTimer, activityName: String) async {
        let center = UNUserNotificationCenter.current()
        guard (try? await center.requestAuthorization(options: [.alert, .sound])) == true else { return }
        let content = UNMutableNotificationContent()
        content.title = "Timer done"
        content.body = "\(activityName): \(timer.minutes.minutesText) logged."
        content.sound = TimerSound.current.notificationSound
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, timer.end.timeIntervalSinceNow), repeats: false)
        try? await center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
    }

    static func cancel() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
    }
}

/// Shows the timer alert (with sound) even while Balance is open.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
