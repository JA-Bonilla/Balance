import SwiftUI

@main
struct BalanceApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var store = Store()

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
            .tint(.accentInk)
            .alert("Balance", isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) {
                Button("OK") {}
            } message: {
                Text(store.error ?? "")
            }
        }
    }
}
