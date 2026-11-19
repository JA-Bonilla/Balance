import SwiftUI

@main
struct BalanceApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var store = Store()

    init() {
        // SwiftUI's DatePicker has no step option; this sets every time picker in the app to 5-minute steps.
        UIDatePicker.appearance().minuteInterval = 5
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if store.data.goals.isEmpty {
                    OnboardingView()
                } else {
                    HomeView()
                }
            }
            .environment(store)
            .tint(.growth)
            .preferredColorScheme(.dark)
            .alert("Balance", isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) {
                Button("OK") {}
            } message: {
                Text(store.error ?? "")
            }
        }
    }
}
