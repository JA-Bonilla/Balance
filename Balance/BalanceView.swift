import SwiftUI

/// Caption block under the sky: totals and a one-line read on the balance.
struct BalanceView: View {
    let growth: Int
    let fun: Int

    private var balance: Double { AppData.balance(growth: growth, fun: fun) }

    private var caption: String {
        if growth + fun == 0 { return "Log some minutes to wake your Bonsi" }
        switch balance {
        case ..<(-0.8): return "Deep night · the moon covers the sun"
        case ..<(-0.45): return "Dusk · the moon is taking over"
        case ..<(-0.15): return "Moon is rising"
        case ...0.15: return "Golden hour · in balance"
        case ...0.45: return "Sun is rising"
        case ...0.8: return "Clear skies · lots of sun"
        default: return "Heat wave · time to rest and play"
        }
    }

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Label("Growth \(growth.minutesShort)", systemImage: "sun.max.fill").labelStyle(Dot(color: .growth))
                Spacer()
                Label("Fun \(fun.minutesShort)", systemImage: "moon.fill").labelStyle(Dot(color: .fun))
            }
            .font(.caption.weight(.semibold))

            Text(caption).font(.headline).multilineTextAlignment(.center)
        }
        .shadow(color: .black.opacity(0.55), radius: 3, y: 1) // keeps text legible over bright sand/sky
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(caption)
        .accessibilityValue("\(growth) growth minutes, \(fun) fun minutes this week")
    }
}

/// The hero graphic, drawn full-bleed behind the home list. Sun = growth, moon = fun, Bonsi in the middle.
/// Balanced (1 growth : 5 fun) is golden hour; more fun slides the moon over the sun into night; more growth
/// brightens the sky and finally scorches it into desert.
struct SkyBackdrop: View {
    let growth: Int
    let fun: Int
    var stage = 3
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var balance: Double { growth + fun == 0 ? 0 : AppData.balance(growth: growth, fun: fun) }

    var body: some View {
        LivingSky(balance: balance, stage: stage, reduceMotion: reduceMotion)
            .animation(reduceMotion ? nil : .easeInOut(duration: 1.4), value: balance)
            // Bright skies (lots of growth) get a charcoal scrim behind the status bar and toolbar buttons for contrast.
            .overlay(alignment: .top) {
                let t = min(max(balance, 0), 1)
                LinearGradient(colors: [Color.appBackground.opacity(0.1 + 0.27 * t), .clear], startPoint: .top, endPoint: .bottom)
                    .frame(height: 140)
                    .animation(reduceMotion ? nil : .easeInOut(duration: 1.4), value: balance)
                    .allowsHitTesting(false)
            }
    }
}

/// Colored icon + primary-colored text (amber text fails contrast on light backgrounds).
private struct Dot: LabelStyle {
    let color: Color
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 4) {
            configuration.icon.foregroundStyle(color).imageScale(.small)
            configuration.title
        }
    }
}

#Preview {
    ZStack(alignment: .top) {
        Color.appBackground
        SkyBackdrop(growth: 60, fun: 300)
        BalanceView(growth: 60, fun: 300).padding(.top, 440)
    }
    .ignoresSafeArea()
}
