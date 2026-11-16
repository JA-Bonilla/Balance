import SwiftUI

/// "Ember & Ink" tokens. Fun gold is for fills/icons only: too low-contrast for small text on light backgrounds.
extension Color {
    static let growth = Color(light: 0x2A6F97, dark: 0x7FB8DE)
    static let fun = Color(light: 0xB57A00, dark: 0xF2C14E)
    static let accentInk = Color(light: 0x2B2D7A, dark: 0xA7A8FF)
    static let appBackground = Color(light: 0xF7F5EF, dark: 0x15140F)

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
