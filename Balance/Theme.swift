import SwiftUI

/// "Sun & Moon" tokens. Growth is the sun (amber), fun is the moon (lavender), life is the plant (green).
/// Sun amber is for fills/icons only: too low-contrast for small text on light backgrounds.
extension Color {
    static let growth = Color(light: 0xC98A00, dark: 0xF2B84B)
    static let fun = Color(light: 0x7A5BD6, dark: 0xB79CFF)
    static let life = Color(light: 0x3F8F57, dark: 0x8FD694)
    static let accentInk = Color(light: 0x2B2D7A, dark: 0xA7A8FF)
    static let appBackground = Color(hex: 0x1D1C22)

    static func side(_ side: Side) -> Color { side == .growth ? .growth : .fun }

    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light) })
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat(hex >> 16 & 0xFF) / 255, green: CGFloat(hex >> 8 & 0xFF) / 255, blue: CGFloat(hex & 0xFF) / 255, alpha: 1)
    }
}

extension Color {
    /// Raised card/row surface on the charcoal background.
    static let surface = Color(hex: 0x27262E)
    init(hex: UInt32) { self.init(uiColor: UIColor(hex: hex)) }
}

extension View {
    /// The app is always charcoal: hides the system list background and paints the app background instead.
    func charcoalBackground() -> some View {
        scrollContentBackground(.hidden).background(Color.appBackground)
    }
}
