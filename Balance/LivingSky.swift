import SwiftUI

struct RGB: Equatable {
    var r: Double, g: Double, b: Double
    init(_ hex: UInt32) {
        r = Double(hex >> 16 & 0xFF) / 255; g = Double(hex >> 8 & 0xFF) / 255; b = Double(hex & 0xFF) / 255
    }
    init(r: Double, g: Double, b: Double) { self.r = r; self.g = g; self.b = b }
    static func mix(_ a: RGB, _ b: RGB, _ t: Double) -> RGB {
        RGB(r: a.r + (b.r - a.r) * t, g: a.g + (b.g - a.g) * t, b: a.b + (b.b - a.b) * t)
    }
    var color: Color { Color(red: r, green: g, blue: b) }
    func color(_ opacity: Double) -> Color { Color(red: r, green: g, blue: b, opacity: opacity) }
}

private func mix(_ a: Double, _ b: Double, _ t: Double) -> Double { a + (b - a) * t }

/// Everything about the sky that changes with balance. Positions are in a 342×340 design box.
struct SkyParams {
    var top, bottom, horizon, sunColor, sunGlow, moonColor, ground: RGB
    var horizonA, stars, sunX, sunY, sunR, sunGlowA, moonX, moonY, moonR, moonA, rim, clouds, grass, desert: Double

    static func mix(_ a: SkyParams, _ b: SkyParams, _ t: Double) -> SkyParams {
        func m(_ x: Double, _ y: Double) -> Double { x + (y - x) * t }
        return SkyParams(
            top: .mix(a.top, b.top, t), bottom: .mix(a.bottom, b.bottom, t), horizon: .mix(a.horizon, b.horizon, t),
            sunColor: .mix(a.sunColor, b.sunColor, t), sunGlow: .mix(a.sunGlow, b.sunGlow, t),
            moonColor: .mix(a.moonColor, b.moonColor, t), ground: .mix(a.ground, b.ground, t),
            horizonA: m(a.horizonA, b.horizonA), stars: m(a.stars, b.stars), sunX: m(a.sunX, b.sunX), sunY: m(a.sunY, b.sunY),
            sunR: m(a.sunR, b.sunR), sunGlowA: m(a.sunGlowA, b.sunGlowA), moonX: m(a.moonX, b.moonX), moonY: m(a.moonY, b.moonY),
            moonR: m(a.moonR, b.moonR), moonA: m(a.moonA, b.moonA), rim: m(a.rim, b.rim), clouds: m(a.clouds, b.clouds),
            grass: m(a.grass, b.grass), desert: m(a.desert, b.desert))
    }

    static let eclipse = SkyParams(
        top: RGB(0x07061A), bottom: RGB(0x2A1F4D), horizon: RGB(0x6B4FA8), sunColor: RGB(0xF2B84B), sunGlow: RGB(0xF2B84B),
        moonColor: RGB(0x2B2447), ground: RGB(0x17162A),
        horizonA: 0.3, stars: 14, sunX: 171, sunY: 142, sunR: 46, sunGlowA: 0.5, moonX: 176, moonY: 146, moonR: 44, moonA: 1, rim: 1, clouds: 0, grass: 0, desert: 0)
    static let dusk = SkyParams(
        top: RGB(0x2B2350), bottom: RGB(0xC0708A), horizon: RGB(0xFF9A6B), sunColor: RGB(0xFFB45A), sunGlow: RGB(0xFF9A6B),
        moonColor: RGB(0xCBB8FF), ground: RGB(0x2A2438),
        horizonA: 0.45, stars: 6, sunX: 120, sunY: 148, sunR: 40, sunGlowA: 0.5, moonX: 158, moonY: 131, moonR: 36, moonA: 1, rim: 0, clouds: 0, grass: 0, desert: 0)
    static let golden = SkyParams(
        top: RGB(0x4B3F7A), bottom: RGB(0xF0B27A), horizon: RGB(0xFFD08A), sunColor: RGB(0xF2B84B), sunGlow: RGB(0xFFD08A),
        moonColor: RGB(0xCBB8FF), ground: RGB(0x2C2F26),
        horizonA: 0.5, stars: 3, sunX: 95, sunY: 134, sunR: 36, sunGlowA: 0.5, moonX: 238, moonY: 130, moonR: 28, moonA: 0.95, rim: 0, clouds: 0, grass: 0, desert: 0)
    static let day = SkyParams(
        top: RGB(0x4FA8E8), bottom: RGB(0xD6F0FF), horizon: RGB(0xFFFFFF), sunColor: RGB(0xFFD84F), sunGlow: RGB(0xFFE680),
        moonColor: RGB(0xE8E0FF), ground: RGB(0x3E7F4B),
        horizonA: 0, stars: 0, sunX: 105, sunY: 152, sunR: 46, sunGlowA: 0.6, moonX: 280, moonY: 134, moonR: 16, moonA: 0.5, rim: 0, clouds: 1, grass: 1, desert: 0)
    static let desertSky = SkyParams(
        top: RGB(0xF08A3C), bottom: RGB(0xFFE3A6), horizon: RGB(0xFFE3A6), sunColor: RGB(0xFFF3B0), sunGlow: RGB(0xFFE680),
        moonColor: RGB(0xE8E0FF), ground: RGB(0xD9B26F),
        horizonA: 0, stars: 0, sunX: 171, sunY: 156, sunR: 54, sunGlowA: 0.7, moonX: 280, moonY: 134, moonR: 10, moonA: 0, rim: 0, clouds: 0, grass: 0, desert: 1)

    /// -1 total eclipse, -0.5 dusk, 0 golden hour (on target), +0.5 bright day, +1 desert.
    static let stops: [(b: Double, p: SkyParams)] = [(-1, eclipse), (-0.5, dusk), (0, golden), (0.5, day), (1, desertSky)]

    static func at(_ balance: Double) -> SkyParams {
        let b = min(max(balance, -1), 1)
        for i in 1..<stops.count where b <= stops[i].b {
            let (lo, hi) = (stops[i - 1], stops[i])
            return mix(lo.p, hi.p, (b - lo.b) / (hi.b - lo.b))
        }
        return desertSky
    }
}

/// The hero graphic: sun = growth, moon = fun, a potted Bonsi in the middle. Balance (-1…1) moves the whole sky.
/// Animatable so balance changes ease between sky states.
struct LivingSky: View {
    var balance: Double
    var stage: Int
    var reduceMotion = false

    private static let stars: [CGPoint] = [
        .init(x: 30, y: 30), .init(x: 80, y: 55), .init(x: 130, y: 22), .init(x: 200, y: 40), .init(x: 260, y: 28),
        .init(x: 310, y: 60), .init(x: 50, y: 100), .init(x: 300, y: 110), .init(x: 110, y: 80), .init(x: 230, y: 90),
        .init(x: 20, y: 150), .init(x: 320, y: 160), .init(x: 160, y: 50), .init(x: 70, y: 20),
    ]

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { tl in
            Canvas { ctx, size in
                Self.draw(ctx, size: size, balance: balance, stage: stage, t: reduceMotion ? 0 : tl.date.timeIntervalSinceReferenceDate)
            }
        }
        .aspectRatio(342.0 / 380.0, contentMode: .fit)
        // Full-bleed at the sides and top; only the ground melts into the charcoal app background.
        .mask(LinearGradient(stops: [.init(color: .black, location: 0), .init(color: .black, location: 0.82), .init(color: .clear, location: 1)],
                             startPoint: .top, endPoint: .bottom))
        .accessibilityHidden(true)
    }

    private static func glow(_ ctx: GraphicsContext, _ x: Double, _ y: Double, _ r: Double, _ color: Color, blur: Double) {
        ctx.drawLayer { l in
            l.addFilter(.blur(radius: blur))
            l.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)), with: .color(color))
        }
    }

    private static func dot(_ ctx: GraphicsContext, _ x: Double, _ y: Double, _ r: Double, _ color: Color) {
        ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)), with: .color(color))
    }

    static func draw(_ base: GraphicsContext, size: CGSize, balance: Double, stage: Int, t: Double) {
        let p = SkyParams.at(balance)
        var ctx = base
        let k = size.width / 342
        ctx.scaleBy(x: k, y: k)

        ctx.fill(Path(CGRect(x: 0, y: 0, width: 342, height: 380)),
                 with: .linearGradient(Gradient(colors: [p.top.color, p.bottom.color]), startPoint: .zero, endPoint: CGPoint(x: 0, y: 340)))

        for (i, s) in stars.enumerated() {
            let a = min(max(p.stars - Double(i), 0), 1) * (0.7 + 0.3 * sin(t * 1.7 + Double(i) * 1.3))
            if a > 0.01 { dot(ctx, s.x, s.y, i % 3 == 0 ? 1.8 : 1.3, .white.opacity(a * 0.9)) }
        }
        if p.horizonA > 0.01 {
            ctx.drawLayer { l in
                l.addFilter(.blur(radius: 40))
                l.fill(Path(ellipseIn: CGRect(x: -39, y: 245, width: 420, height: 130)), with: .color(p.horizon.color(p.horizonA)))
            }
        }
        if p.clouds > 0.01 {
            for (x0, y, w, speed) in [(250.0, 70.0, 90.0, 4.0), (70.0, 140.0, 70.0, 2.5)] {
                let x = -60 + (x0 + 60 + t * speed).truncatingRemainder(dividingBy: 460)
                ctx.drawLayer { l in
                    l.addFilter(.blur(radius: 3))
                    l.fill(Path(ellipseIn: CGRect(x: x - w / 2, y: y - w * 0.175, width: w, height: w * 0.35)), with: .color(.white.opacity(0.8 * p.clouds)))
                }
            }
        }

        // Sun (growth)
        let pulse = 1 + 0.03 * sin(t * 1.4)
        glow(ctx, p.sunX, p.sunY, p.sunR * 1.7 * pulse, p.sunGlow.color(p.sunGlowA), blur: p.sunR * 0.7)
        if p.desert > 0.01 {
            for kk in [1.5, 2.0] {
                let r = p.sunR * kk * (1 + 0.02 * sin(t * 1.2 + kk))
                ctx.stroke(Path(ellipseIn: CGRect(x: p.sunX - r, y: p.sunY - r, width: r * 2, height: r * 2)),
                           with: .color(RGB(0xFFF3B0).color(0.35 / kk * p.desert)), lineWidth: 2)
            }
        }
        dot(ctx, p.sunX, p.sunY, p.sunR, p.sunColor.color)

        // Moon (fun)
        if p.moonA > 0.01 {
            let mx = p.moonX + 3 * sin(t * 0.4), my = p.moonY + 2 * sin(t * 0.55)
            let lavender = RGB(0xB79CFF)
            glow(ctx, mx, my, p.moonR * 1.5, lavender.color(0.25 * p.moonA), blur: p.moonR * 0.6)
            dot(ctx, mx, my, p.moonR, p.moonColor.color(p.moonA))
            if p.rim > 0.01 {
                ctx.stroke(Path(ellipseIn: CGRect(x: mx - p.moonR, y: my - p.moonR, width: p.moonR * 2, height: p.moonR * 2)),
                           with: .color(lavender.color(p.rim)), lineWidth: 2)
            }
            let craterA = (1 - p.rim) * 0.55 * p.moonA
            if craterA > 0.01 {
                for (dx, dy, d) in [(-8.0, -6.0, 9.0), (7.0, 5.0, 6.0), (-2.0, 10.0, 5.0)] {
                    dot(ctx, mx + dx * p.moonR / 28, my + dy * p.moonR / 28, d / 2, RGB(0x9C86D9).color(craterA))
                }
            }
        }

        // Ground
        ctx.fill(Path(ellipseIn: CGRect(x: -59, y: 275, width: 460, height: 140)), with: .color(p.ground.color))
        if p.grass > 0.01 {
            for (x, y) in [(60.0, 292.0), (95, 298), (250, 294), (285, 290), (215, 302)] {
                ctx.fill(Path(ellipseIn: CGRect(x: x - 7, y: y - 4, width: 14, height: 8)), with: .color(RGB(0x6FBF7C).color(p.grass)))
            }
        }
        if p.desert > 0.01 {
            let a = p.desert
            for (x, y, w, h) in [(70.0, 302.0, 160.0, 18.0), (270, 304, 170, 20)] {
                ctx.fill(Path(ellipseIn: CGRect(x: x - w / 2, y: y - h / 2, width: w, height: h)), with: .color(RGB(0xE6C384).color(a)))
            }
            for (x, y, l, deg) in [(120.0, 316.0, 40.0, -20.0), (210, 320, 36, 15), (250, 310, 30, -30)] {
                var c = ctx
                c.translateBy(x: x + l / 2, y: y)
                c.rotate(by: .degrees(-deg))
                c.fill(Path(CGRect(x: -l / 2, y: -1, width: l, height: 2)), with: .color(RGB(0xB88E4D).color(a)))
            }
            let cactus = RGB(0x5C8F4F).color(a)
            for r in [CGRect(x: 288, y: 254, width: 12, height: 44), CGRect(x: 276, y: 268, width: 8, height: 6), CGRect(x: 276, y: 262, width: 6, height: 18),
                      CGRect(x: 300, y: 276, width: 8, height: 6), CGRect(x: 302, y: 264, width: 6, height: 16)] {
                ctx.fill(Path(roundedRect: r, cornerRadius: min(r.width, r.height) / 2), with: .color(cactus))
            }
        }

        // Bonsi: sleepy under a moon-heavy sky, worried as the sun scorches the garden.
        let mood: BonsiMood = balance < -0.3 ? .sleepy : (p.desert > 0.5 ? .worried : .happy)
        var tree = ctx
        tree.translateBy(x: 171 - 110, y: 326 - 209)
        tree.scaleBy(x: 1.1, y: 1.1)
        BonsiRenderer.draw(tree, stage: stage, mood: mood, wilt: p.desert, t: t)
    }
}

extension LivingSky: @preconcurrency Animatable {
    var animatableData: Double {
        get { balance }
        set { balance = newValue }
    }
}

#Preview("Sky states") {
    ScrollView {
        VStack(spacing: 16) {
            ForEach([-1.0, -0.5, 0, 0.5, 1], id: \.self) { b in
                LivingSky(balance: b, stage: 3)
            }
        }
        .padding()
    }
    .background(Color.appBackground)
}
