# SEED State: balance

## Meta
- **Type:** application (emerging app idea, framed as a business)
- **Started:** 2026-11-17
- **Last Updated:** 2026-11-17
- **Position:** Sections 2-8 and 9 — technical proposal and phase breakdown awaiting approval
- **BASE v2:** false

## Gathered Data

### Project Name
balance

### Core Description
Native iOS app (SwiftUI, iOS 26+) that tracks growth vs fun time at a realistic 1:5 ratio. Existing: onboarding, timers, logging, HealthKit import, per-day goals, custom activities, local JSON storage. Uncommitted: LivingSky (sky reflects balance) and Bonsi (bonsai character that grows with lifetime growth hours; "Roots" stand-in).

### User direction
Treat as a business: competitors, user attraction, retention, non-overbearing monetization, app features, business roadmap, marketing strategies.

### Sections Completed
| Section | Status | Summary |
|---------|--------|---------|
| Problem Statement | complete | Dogfooding: built by the founder as positive reinforcement for reading books and learning skills. Pitch is permission and progress, not restriction; tone should be warmer, less robotic. Bonsi is the reason users stay: a sense of accomplishment, or at least another motivator. |
| Tech Stack | pending | — |
| Data Model | pending | — |
| API Surface | pending | — |
| Deployment Strategy | pending | — |
| Security Considerations | pending | — |
| UI/UX Needs | pending | — |
| Integration Points | pending | — |
| Phase Breakdown | pending | — |
| Skill Loadout | pending | — |

### Raw Responses
Type: emerging app idea; business lens.
Problem Statement: Built for self as positive reinforcement for reading and learning skills. Draft pitch ("not about doing less of what you enjoy, about making sure you're growing too, and a living world rewards you") feels right but slightly robotic. Bonsi = retention anchor / sense of accomplishment / extra motivator.
Market research so far: Opal (freemium, ~$100/yr Pro), One Sec, ScreenZen, Forest, Finch (pet retention, ~$30-48/yr). Gap: competitors restrict; none reward growth vs fun at a realistic ratio. Not yet researched: Daylio, Streaks, Gentler Streak, Apple Screen Time/Journal, Rize/Toggl.
Audience: built for anyone who wants to grow (broad), though early marketing may still lead with a niche.
Bonsi emotional rules: gentle reminding, not punishment; he can lose leaves when growth is neglected but never dies.
Monetization: freemium. Free = logging, timers, goals, sky, basic Bonsi. Paid = splitting growth goals / multiple trees, skins and tree variety. One-time lifetime purchase only, no subscription. Hard rules: no selling data, no annoying push notifications, minimal-to-zero data retention (local-first).
Roadmap (agreed): v0.2 launch-ready polish + TestFlight; v1.0 App Store launch (free core, widgets, opt-in reminder, lifetime unlock with 2-3 extra trees); v1.1 garden (multi-goal trees), weekly recap, shareable cards; v1.2 iCloud sync, Watch quick-log; v2 seasons and achievement garden.
Plan: solo-build to an MVP for friends and family feedback before any public deployment. If the concept validates, a graphic-artist friend does animations and sprites for public launch. After that, needs help with real-world marketing, deployment, and upkeep.
Resources: no paid Apple Developer account yet but can pay easily. Artist is a friend; pricing and equity not discussed (open item). Assistance = AI and the artist only. Target: friends-and-family MVP on both iOS and Android by Thanksgiving (2026-11-26, 9 days from 2026-11-17).
FLAG: app is native SwiftUI + HealthKit, iOS only. Android by Thanksgiving is not realistic without a rewrite; decision needed (iOS-only TestFlight first vs cross-platform). Possible artist pipeline: Rive or Lottie for cross-platform animation.
Platform decision: native SwiftUI iOS now, separate native Android app later (two codebases accepted because the app is lightweight); Android matters because friends use it and it widens the market. iOS TestFlight build to friends and family by Thanksgiving (2026-11-26) is still the goal. Pay for Apple Developer account now.
Proposed (awaiting approval): no backend; StoreKit 2 / Play Billing non-consumable lifetime unlock; derive Bonsi state from entries; add schemaVersion; Tree entity per goal for the garden; no analytics or third-party SDKs; feedback via external short form; Rive for artist animations.
Next group: confirm technical proposal (stack, data model, API surface, security, deployment, UI/UX) and phase breakdown, then draft PLANNING.md.
