import SwiftUI

enum BonsiMood { case happy, sleepy, worried }

extension AppData {
    /// Bonsi grows with lifetime growth hours (a stand-in for "Roots"): 0 seed, 1+ sprout, 10+ sapling, 30+ bonsai.
    var bonsiStage: Int {
        let hours = entries.filter { $0.side == .growth }.reduce(0) { $0 + $1.minutes } / 60
        return hours >= 30 ? 3 : hours >= 10 ? 2 : hours >= 1 ? 1 : 0
    }
}

/// The caricature bonsai: a potted tree with a face on its trunk. Draws into a 200×200 design box (pot base at y=190).
enum BonsiRenderer {
    private static func rgb(_ hex: UInt32, _ o: Double = 1) -> Color {
        Color(red: Double(hex >> 16 & 0xFF) / 255, green: Double(hex >> 8 & 0xFF) / 255, blue: Double(hex & 0xFF) / 255, opacity: o)
    }

    private static func ellipse(_ c: GraphicsContext, _ x: Double, _ y: Double, _ w: Double, _ h: Double, _ color: Color) {
        c.fill(Path(ellipseIn: CGRect(x: x - w / 2, y: y - h / 2, width: w, height: h)), with: .color(color))
    }

    /// Rounded rect centred at (x, y), rotated `deg` counter-clockwise.
    private static func bar(_ c: GraphicsContext, _ x: Double, _ y: Double, _ w: Double, _ h: Double, _ deg: Double, _ color: Color, radius: Double? = nil) {
        var cc = c
        cc.translateBy(x: x, y: y)
        cc.rotate(by: .degrees(-deg))
        cc.fill(Path(roundedRect: CGRect(x: -w / 2, y: -h / 2, width: w, height: h), cornerRadius: radius ?? min(w, h) / 2), with: .color(color))
    }

    private static func mix(_ a: UInt32, _ b: UInt32, _ t: Double) -> Color {
        func ch(_ s: Int) -> Double {
            let x = Double(a >> UInt32(s) & 0xFF), y = Double(b >> UInt32(s) & 0xFF)
            return (x + (y - x) * t) / 255
        }
        return Color(red: ch(16), green: ch(8), blue: ch(0))
    }

    static func draw(_ ctx: GraphicsContext, stage: Int, mood: BonsiMood, wilt: Double, t: Double) {
        let cx = 100.0, base = 190.0
        let dark = rgb(0x1C1D1A)
        // Pot (static)
        ellipse(ctx, cx, base + 2, 110, 14, .black.opacity(0.25))
        ctx.fill(Path(roundedRect: CGRect(x: cx - 32, y: base - 30, width: 64, height: 30), cornerRadius: 7), with: .color(rgb(0xC97B55)))
        ctx.fill(Path(roundedRect: CGRect(x: cx - 39, y: base - 40, width: 78, height: 12), cornerRadius: 6), with: .color(rgb(0xDC8F68)))
        ctx.fill(Path(roundedRect: CGRect(x: cx - 22, y: base - 26, width: 6, height: 16), cornerRadius: 3), with: .color(rgb(0xE39A74)))
        ellipse(ctx, cx, base - 40, 58, 8, rgb(0x4A3B2E))

        // Tree sways gently from the soil line and breathes.
        var c = ctx
        c.translateBy(x: cx, y: base - 40)
        c.rotate(by: .degrees(1.6 * sin(t * 0.9)))
        c.scaleBy(x: 1, y: 1 + 0.012 * sin(t * 1.6))
        c.translateBy(x: -cx, y: -(base - 40))

        let blink = t.truncatingRemainder(dividingBy: 4.3) < 0.14

        if stage == 0 {
            ellipse(c, cx, base - 52, 18, 24, rgb(0x8A6B4A))
            bar(c, cx + 3, base - 70, 8, 14, 25, rgb(0x8FD694))
            c.fill(Path(CGRect(x: cx - 6, y: base - 53, width: 5, height: 2)), with: .color(dark))
            c.fill(Path(CGRect(x: cx + 2, y: base - 53, width: 5, height: 2)), with: .color(dark))
            return
        }

        let tw = [0, 26, 36, 46][stage], th = [0, 40, 56, 70][stage]
        let tb = base - 40, tt = tb - Double(th)
        let w = Double(tw), h = Double(th)
        ellipse(c, cx, tb - 2, w * 1.5, 14, rgb(0x8A5E3C))
        c.fill(Path(roundedRect: CGRect(x: cx - w / 2, y: tt, width: w, height: h), cornerRadius: w / 2.4), with: .color(rgb(0x9A6A44)))
        c.fill(Path(roundedRect: CGRect(x: cx + w / 2 - 10, y: tt + 10, width: 4, height: h * 0.6), cornerRadius: 2), with: .color(rgb(0xB07F58)))
        if stage >= 2 {
            bar(c, cx - w / 2 - 6, tt + 8, 7, 30, 40, rgb(0x8A5E3C))
            bar(c, cx + w / 2 + 6, tt + 8, 7, 30, -40, rgb(0x8A5E3C))
        }

        // Foliage (greens fade to dry yellow as `wilt` rises)
        let healthy: [UInt32] = [0x6FBF7C, 0x7BC987, 0x8FD694, 0x5FA06B]
        let dry: [UInt32] = [0xB8B86A, 0xC4B66B, 0xA89A55, 0xB0A860]
        let puffs: [(Double, Double, Double)]
        switch stage {
        case 1: puffs = [(cx - 10, tt - 2, 14), (cx + 10, tt - 6, 14)]
        case 2: puffs = [(cx - 24, tt + 6, 20), (cx + 24, tt + 2, 20), (cx, tt - 12, 25)]
        default: puffs = [(cx - 44, tt + 20, 26), (cx + 46, tt + 14, 26), (cx - 20, tt - 8, 31), (cx + 22, tt - 14, 31), (cx, tt - 36, 30)]
        }
        for (i, p) in puffs.enumerated() {
            let (x, y, r) = p
            let dx = 1.2 * sin(t * 1.3 + Double(i)), dy = 8 * wilt + 0.8 * sin(t * 1.1 + Double(i) * 2)
            ellipse(c, x + dx, y + dy, r * 2, r * 1.6, mix(healthy[i % 4], dry[i % 4], wilt))
            ellipse(c, x + dx - r * 0.3, y + dy - r * 0.3, r * 0.9, r * 0.6, .white.opacity(0.18))
        }
        if stage == 3 {
            let a = max(0, 1 - wilt * 3)
            for (i, p) in [(cx - 30, tt - 12), (cx + 34, tt - 24), (cx + 54, tt + 12), (cx - 48, tt + 14)].enumerated() {
                ellipse(c, p.0, p.1, 10, 10, rgb(0xCBB8FF, a))
                ellipse(c, p.0, p.1, 4, 4, rgb(0xFFF3B0, a * (0.7 + 0.3 * sin(t * 2 + Double(i)))))
            }
        }

        // Face
        let fs = w / 46, fy = tt + h * 0.5
        for dx in [-10.0, 10.0] {
            let ex = cx + dx * fs
            if mood == .sleepy {
                var p = Path(); p.move(to: CGPoint(x: ex - 6 * fs, y: fy)); p.addQuadCurve(to: CGPoint(x: ex + 6 * fs, y: fy), control: CGPoint(x: ex, y: fy + 5 * fs))
                c.stroke(p, with: .color(dark), style: StrokeStyle(lineWidth: 2.6, lineCap: .round))
            } else {
                let open = blink ? 0.12 : 1.0
                ellipse(c, ex, fy - 2, 14 * fs, 16 * fs * open, .white)
                ellipse(c, ex + (mood == .worried ? 0 : dx * 0.08 * fs), fy - 1, 7 * fs, 9 * fs * open, dark)
            }
        }
        let worried = mood == .worried || wilt > 0.5
        bar(c, cx - 10 * fs, fy - (worried ? 14 : 16) * fs, 13 * fs, 3, worried ? 15 : 0, rgb(0x5A3B22))
        bar(c, cx + 10 * fs, fy - (worried ? 14 : 16) * fs, 13 * fs, 3, worried ? -15 : 0, rgb(0x5A3B22))
        let my = fy + 12 * fs
        var mouth = Path()
        switch (mood, worried) {
        case (.sleepy, _):
            ellipse(c, cx, my + 2, 6 * fs, 6 * fs, dark)
        case (_, true):
            mouth.move(to: CGPoint(x: cx - 8 * fs, y: my + 5)); mouth.addQuadCurve(to: CGPoint(x: cx + 8 * fs, y: my + 5), control: CGPoint(x: cx, y: my - 4 * fs))
            c.stroke(mouth, with: .color(dark), style: StrokeStyle(lineWidth: 2.6, lineCap: .round))
        default:
            mouth.move(to: CGPoint(x: cx - 9 * fs, y: my)); mouth.addQuadCurve(to: CGPoint(x: cx + 9 * fs, y: my), control: CGPoint(x: cx, y: my + 11 * fs))
            c.stroke(mouth, with: .color(dark), style: StrokeStyle(lineWidth: 2.6, lineCap: .round))
        }
        ellipse(c, cx - 20 * fs, fy + 9 * fs, 10 * fs, 6, rgb(0xF4A7A0, 0.6))
        ellipse(c, cx + 20 * fs, fy + 9 * fs, 10 * fs, 6, rgb(0xF4A7A0, 0.6))
        if wilt > 0.5 { ellipse(c, cx + w / 2 + 10, tt + 14 + 3 * sin(t * 3), 8, 12, rgb(0x7FD6F5, min(1, (wilt - 0.5) * 4))) }
        if mood == .sleepy {
            for (i, size) in [16.0, 11.0].enumerated() {
                let drift = (t * 6 + Double(i) * 10).truncatingRemainder(dividingBy: 20)
                c.draw(Text("z").font(.system(size: size, weight: .bold)).foregroundStyle(rgb(0xCBB8FF, 1 - drift / 20)),
                       at: CGPoint(x: cx + w / 2 + 16 + Double(i) * 16, y: tt - 14 - Double(i) * 14 - drift))
            }
        }
    }
}

/// Standalone Bonsi, for previews and reuse.
struct BonsiView: View {
    var stage: Int
    var mood: BonsiMood = .happy
    var wilt: Double = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { tl in
            Canvas { ctx, size in
                var c = ctx
                let k = size.width / 200
                c.scaleBy(x: k, y: k)
                BonsiRenderer.draw(c, stage: stage, mood: mood, wilt: wilt, t: reduceMotion ? 0 : tl.date.timeIntervalSinceReferenceDate)
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .accessibilityHidden(true)
    }
}

#Preview {
    HStack {
        ForEach(0..<4) { BonsiView(stage: $0) }
    }
    .padding()
    .background(Color.appBackground)
}
