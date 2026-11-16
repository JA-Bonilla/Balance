import SwiftUI

/// Centered split gauge: marker slides toward fun (left, gold) or growth (right, blue); centered = balanced.
struct BalanceView: View {
    let growth: Int
    let fun: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var balance: Double { AppData.balance(growth: growth, fun: fun) }

    private enum Lean { case empty, balanced, growth, fun, growthHeavy, funHeavy }

    private var lean: Lean {
        if growth + fun == 0 { return .empty }
        switch balance {
        case ..<(-0.5): return .funHeavy
        case ..<(-0.15): return .fun
        case ...0.15: return .balanced
        case ...0.5: return .growth
        default: return .growthHeavy
        }
    }

    private var caption: String {
        switch lean {
        case .empty: "Log some minutes to see your balance"
        case .balanced: "Balanced"
        case .growth: "Leaning toward growth"
        case .fun: "Leaning toward fun"
        case .growthHeavy: "Growth-heavy"
        case .funHeavy: "Fun-heavy"
        }
    }

    var body: some View {
        VStack(spacing: 12) {
            GeometryReader { geo in
                let x = CGFloat(balance) * (geo.size.width / 2 - 13)
                ZStack {
                    Capsule().fill(Color.secondary.opacity(0.2)).frame(height: 14)
                    Capsule().fill(Color.fun).frame(width: max(-x, 0), height: 14).offset(x: min(x, 0) / 2)
                    Capsule().fill(Color.growth).frame(width: max(x, 0), height: 14).offset(x: max(x, 0) / 2)
                    Rectangle().fill(Color.primary.opacity(0.3)).frame(width: 2, height: 22)
                    Circle()
                        .fill(.background)
                        .overlay(Circle().stroke(lean == .balanced ? Color.primary : Color.primary.opacity(0.5), lineWidth: 2))
                        .frame(width: 26, height: 26)
                        .scaleEffect(lean == .balanced ? 1.15 : 1)
                        .offset(x: x)
                }
                .frame(maxHeight: .infinity)
                .animation(reduceMotion ? nil : .spring(response: 0.5, dampingFraction: 0.65), value: balance)
            }
            .frame(height: 30)

            HStack {
                Label("Fun \(fun.minutesShort)", systemImage: "circle.fill").labelStyle(Dot(color: .fun))
                Spacer()
                Label("Growth \(growth.minutesShort)", systemImage: "circle.fill").labelStyle(Dot(color: .growth))
            }
            .font(.caption.weight(.semibold))

            Text(caption).font(.headline)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(caption)
        .accessibilityValue("\(growth) growth minutes, \(fun) fun minutes this week")
    }
}

/// Colored dot + primary-colored text (gold text fails contrast on light backgrounds).
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
    VStack(spacing: 32) {
        BalanceView(growth: 60, fun: 60)
        BalanceView(growth: 150, fun: 40)
        BalanceView(growth: 20, fun: 200)
        BalanceView(growth: 0, fun: 0)
    }
    .padding()
}
