# Balance

> A privacy-first iOS (and later Android) app that rewards time spent growing (reading, learning, practicing) while accepting a realistic amount of fun, with a living bonsai and sky that reflect your balance.

**Created:** 2026-11-17
**Type:** Application
**Stack:** SwiftUI (iOS 26+) + local JSON storage + StoreKit 2 + HealthKit + WidgetKit; later Kotlin/Jetpack Compose on Android; Rive for artist-made animation; no backend
**Skill Loadout:** PAUL (required), AEGIS (recommended), sonarqube scan (optional)
**Quality Gates:** unit test pass, "Data Not Collected" privacy label, VoiceOver/Dynamic Type check, TestFlight smoke test

---

## Problem Statement

**What it solves.** Most screen-time apps restrict or shame the time people spend on fun (Opal, One Sec, ScreenZen). Few reward the other half of life: the book, the skill, the practice people keep meaning to do. Balance tracks growth time against fun time at a *realistic* ratio (about 1 growth minute per 5 fun minutes counts as balanced, `growthPerFun = 0.2`, instead of an unrealistic 50/50) and gives gentle positive reinforcement through a living sky and a bonsai character, Bonsi, who grows with lifetime growth hours.

**Who it's for.** Built first for the founder (dogfooding: positive reinforcement for reading books and learning skills). The intended audience is anyone who wants to grow; early marketing can still lead with a narrow hook such as readers and learners.

**Why build vs buy.** No product found rewards growth rather than blocking fun, at a realistic ratio, with no account and no data collection.

**Pitch (draft, wants a warmer tone):** "You already know how to have fun. Balance rewards you for the other part: the book, the skill, the thing you keep meaning to do. Do a little, and watch something grow."

**First 5 minutes:** pick one growth activity, set a doable goal, log or time a first session, and see the sky and Bonsi respond.

---

## Business & Market

### Competitors (initial scan; third-party figures are rough)

| Competitor | Model | Price (reported) | Takeaway |
|-----------|-------|------------------|----------|
| Opal | App blocking, scheduled focus; freemium | Pro about $8-20/mo or about $100/yr | Category leader; founder reports about $5M ARR on a 7-person team. Funding reports conflict. |
| One Sec | Friction before opening apps | About $20/yr or $50 lifetime | Restricts only; no reward for growth. |
| ScreenZen | Delay timers and limits | Free tier plus paid | Less polished; restriction-based. |
| Forest | Grows a tree while you stay off the phone | $3.99 one-time plus IAP | Gamified; reviewers say the effect fades by week three. |
| Finch | Virtual pet cared for through self-care goals | About $30-48/yr | Closest model to Bonsi; attachment to the pet is its main retention driver. |

**Not yet researched:** Daylio, Streaks, Gentler Streak, Apple Screen Time and Journal, Rize/Toggl-style trackers.

**Gap / positioning:** competitors restrict; Balance gives permission and progress. Tagline direction: "the anti-blocker" / "rewards growth instead of blocking fun". Privacy line: "No account. No tracking. Your data stays on your phone."

### Retention Strategy
- **Day 1-7:** the first log produces a visible reward (sky shift, Bonsi sprouts).
- **Day 7-30:** a gentle rhythm with no punishing streaks. Bonsi loses leaves after several days without growth and regrows them on the next log; he never dies.
- **Day 30-90:** long-term meaning: Bonsi stages (seed, sprout at 1h, sapling at 10h, bonsai at 30h), then seasons and a garden of past achievements.
- **Anchor:** Bonsi is the reason users stay: a sense of accomplishment, or at minimum another motivator.
- Bonsi state is derived from entries (lifetime growth hours, days since last growth log), never stored separately.

### Monetization (non-overbearing)
- **Free forever:** logging, timers, goals, the sky, basic Bonsi, local-first data.
- **One-time lifetime unlock (about $12-20, to be tested):** multiple growth goals (one tree each, the "garden"), extra tree species, pots and sky skins, and history/trends if those are not kept free.
- **No subscription.** No ads. No selling data. No upgrade nudges via push notifications.
- Soft, low-pressure upgrade prompt after a milestone (for example reaching the sapling stage). No hard paywall at launch.
- **Hard rules:** no selling data; no annoying push notifications; no data retention beyond the on-device file.
- **Trade-off acknowledged:** one-time revenue depends on volume; near-zero running costs (no servers) make this workable. Measurement is limited to App Store Connect stats and an optional feedback form.

### Marketing Ideas
- **Before launch:** build-in-public short videos of Bonsi growing (TikTok/Reels/Shorts); waitlist page with opt-in email only.
- **Launch:** Product Hunt and Hacker News (privacy-first, no-account angle); communities (r/getdisciplined, r/books, r/languagelearning, r/selfimprovement, ADHD and habit communities) with the founder story, not ads; micro-creators in book/study/productivity niches given free lifetime codes; App Store optimization (reading goals, habit tracker, balance); Apple editorial pitch.
- **Ongoing:** home-screen widgets (free daily visibility), shareable Bonsi cards, gift-a-tree lifetime codes, press angle "the anti-Opal".

### Benchmarks (context only, verify against primary sources)
- RevenueCat reports about 10.7% download-to-paid for hard paywalls vs about 2.1% for freemium, with long-term annual retention roughly equal (about 27-28%). Health/wellness day-30 and day-90 retention is reported above benchmark, but no reliable category figure was found.

---

## Tech Stack

| Layer | Choice | Rationale |
|-------|--------|-----------|
| iOS app | SwiftUI, iOS 26+, Swift 6, iPhone only (existing) | Fastest path to a polished iPhone MVP; already built |
| Android app (later) | Kotlin + Jetpack Compose | Native feel; accepted second codebase because the app is lightweight |
| Storage | Local `balance.json` in Documents, exposed in Files app (existing) | Privacy promise; no backend cost |
| Backend | None | No accounts, no server, no retained data |
| Cache | None | Not needed |
| Payments | StoreKit 2 (iOS), Play Billing (Android): one non-consumable lifetime unlock | One-time purchase model; restore purchases supported |
| Health | HealthKit (iOS, existing), Health Connect (Android, later) | Auto-count workouts and mindful minutes |
| Widgets | WidgetKit (iOS), Glance (Android) | Daily visibility without notifications |
| Animation | Rive (or Lottie) for artist-made Bonsi states | One asset works on both platforms; replaces the Canvas-drawn Bonsi at the art pass |
| Deployment | TestFlight, then App Store; Play internal testing, then Play Store | See Deployment Strategy |

### Research Needed
- Whether the artist is comfortable with Rive or Lottie.
- Current Google Play requirements for new personal developer accounts (reportedly a 12-tester, 14-day closed test before production; verify).
- Screen Time API availability and entitlements (the app currently says it "isn't available yet" and cites needing a paid developer account).
- Competitors not yet researched (see Business & Market).

### Shared contract across platforms
Keep one written spec for the `balance.json` format and shared constants (1:5 ratio, Bonsi stage thresholds, built-in activity list) so the two apps stay consistent.

---

## Data Model

### Entities

| Entity | Key Fields | Relationships |
|--------|-----------|---------------|
| Entry (exists) | id, side (growth/fun), categoryID, minutes, date, source (manual/health) | Belongs to a Category |
| Goal (exists) | id, categoryID, minutesPerDay, daysPerWeek | Belongs to a Category |
| Category (exists) | id, name, symbol, side, blurb, archived | Built-in list plus user custom ones; archived ones keep old entries readable |
| ActiveTimer (exists) | side, categoryID, start, minutes | Becomes an Entry when finished |
| AppData (exists) | goals, entries, healthEnabled, hiddenHealthIDs, activeTimer, customCategories | Root of the file |
| Tree (new, paid garden) | id, goalID or categoryID, species, skin | One tree per growth goal |
| Entitlement (new) | lifetimeUnlocked | Resolved from StoreKit/Play; not user data |

### Notes
- Add `schemaVersion` to `balance.json` so the Android app and future versions can read it safely. The current decoder already tolerates missing keys and a legacy weekly-only goal format.
- Bonsi stage and leaf state are **derived** from entries, not stored.
- Health samples the user deletes are remembered in `hiddenHealthIDs` so re-imports skip them.
- An unreadable file is moved aside (never overwritten) with an error shown.

---

## API Surface

No server and no network API.

### Auth Strategy
None: no accounts.

### Route Groups
N/A.

### Internal vs External
- **Public endpoints:** none.
- **Internal/admin endpoints:** none.
- **On-device interfaces only:** StoreKit/Play Billing (purchase and restore), HealthKit/Health Connect (read-only), local notifications (opt-in), widgets reading the local file or a shared container.
- **Feedback:** an external short form (for example Tally or Google Forms), optional and linked from Settings; no in-app tracking.

---

## Deployment Strategy

### Local Development
- Xcode project generated from `project.yml` (XcodeGen); bundle id `com.jabonilla.balance`; unit tests in `BalanceTests`.
- Simulator and on-device builds; no services required.

### Staging / Production
- **iOS:** buy the Apple Developer Program account now ($99/yr). TestFlight internal testers, then external testers by email or public link (a short beta review applies, usually a day or two). Later: App Store release with a privacy policy URL (required for HealthKit) and the "Data Not Collected" label.
- **Android (later):** Google Play Console ($25 one-time); internal testing track (no review), then closed testing and production per Google's current rules.
- CI/CD is not needed for the MVP; add Xcode Cloud or a simple GitHub Actions build if release cadence grows.
- Versioning: marketing version 0.1 now; 0.2 for the TestFlight MVP; 1.0 for public launch.

---

## Security Considerations

- **Auth/Authz model:** none; no accounts.
- **Data minimization:** no analytics, no third-party SDKs, no data leaves the device; HealthKit is read-only and on-device.
- **Privacy:** target the "Data Not Collected" App Store label; publish a one-page privacy policy (required for HealthKit).
- **File protection:** enable iOS file protection on `balance.json`. Note the file is deliberately visible in the Files app for backup and recovery.
- **Input validation:** minutes and dates from pickers and Health samples; custom activity names; import robustness for corrupt or older files.
- **Purchases:** rely on StoreKit/Play entitlements and restore purchases; no custom receipt server.
- **Secrets management:** none needed (no keys, no backend).
- **Rate limiting:** N/A.
- **Children:** not targeted.

---

## UI/UX Needs

### Design System
Current: SwiftUI with a custom `Theme`, the living sky (`LivingSky`), and the Canvas-drawn Bonsi (`BonsiView`). At the art pass, the artist delivers sprites and Rive animations (states such as growing, happy, sleepy, worried/leaves falling). Design tone: warm, not robotic.

### Key Views / Pages

| View | Purpose | Complexity |
|------|---------|------------|
| Home | Sky and Bonsi, balance gauge, timers, goals, entries | High: animated, scroll-linked backdrop |
| Log sheet | Log or edit an entry with custom start/end (5-minute steps) | Medium |
| Timer sheet | Start a countdown for an activity; sound plus notification at end | Medium |
| Onboarding | Pick one growth activity and a goal; explains fun tracking | Low |
| Settings | Goals, custom activities, timer, Health data, data location, feedback link, purchase/restore | Medium |
| Garden (later) | Multiple trees, one per goal; unlockable skins | Medium |
| Weekly recap (later) | Short summary of the week | Low-medium |

### Real-Time Requirements
None beyond local animation and timers; widgets refresh on a schedule.

### Responsive Needs
iPhone portrait only today; Android phones later. VoiceOver labels and Dynamic Type support should be checked before the public launch.

---

## Integration Points

| Integration | Type | Purpose | Auth |
|------------|------|---------|------|
| HealthKit | On-device API | Import workouts as Exercise and mindful sessions as Meditation (last 28 days) | User permission |
| Health Connect (later) | On-device API | Android equivalent | User permission |
| StoreKit 2 / Play Billing | Platform purchase API | One-time lifetime unlock | Apple ID / Google account |
| WidgetKit / Glance | Platform widgets | Show Bonsi and balance on the home or lock screen | None |
| Local notifications | Platform API | Timer end; optional opt-in gentle reminder, off by default | User permission |
| TestFlight / Play testing | Distribution | Friends-and-family testing | Apple ID / Google account |
| Feedback form | External web form | Optional tester feedback | None |
| iCloud (later, iOS only) | CloudKit private database | Backup/sync inside the user's own Apple account | Apple ID |

**If an integration is down:** the app keeps working offline; Health import and purchases fail gracefully, and the user can retry.

---

## Phase Breakdown

### Phase 1: iOS Friends-and-Family MVP (target 2026-11-26)
- **Build:** finish and commit the Bonsi and living-sky work; make tests pass; add `schemaVersion`; a Settings link to the feedback form; privacy policy page; purchase the Apple Developer account; archive, upload to TestFlight, and invite testers.
- **Testable:** unit tests pass; clean install on a device via TestFlight; a first-time tester can onboard and log time without help.
- **Outcome:** friends and family with iPhones are using it.

### Phase 2: Feedback Window (about 2 weeks)
- **Build:** collect feedback through the short form; fix the highest-impact issues.
- **Testable:** at least 5-15 testers use it for about two weeks; answers to: did they log repeatedly without nagging, did they mention Bonsi unprompted, would they be very disappointed if it disappeared.
- **Outcome:** a go/no-go decision and a short prioritized fix list.

### Phase 3: Android MVP (target date to be set; assumed mid-January)
- **Build:** the core loop in Kotlin/Compose reading the shared `balance.json` spec: onboarding, logging, timers, goals, gauge, sky and Bonsi; Play internal testing.
- **Testable:** Android friends install and log time; data format matches the spec.
- **Outcome:** Android friends and family are testing.

### Phase 4: Art Pass
- **Build:** agree artist terms (pricing, ownership of assets, any equity) in writing first; artist delivers sprites and Rive animations; integrate on iOS (and Android).
- **Testable:** animation states map to app states; performance holds on older devices.
- **Outcome:** Bonsi and the sky look and feel ready to show publicly.

### Phase 5: v1.0 Public Launch
- **Build:** widgets; opt-in gentle reminder; lifetime unlock with 2-3 extra trees; App Store and Play listings; privacy policy; launch marketing (see Business & Market).
- **Testable:** purchase and restore work end to end; App Store review passes; "Data Not Collected" label accurate.
- **Outcome:** a stranger downloads, uses it, and pays once.

### Phase 6: Growth
- **Build:** garden (multiple goals and trees), weekly recap, shareable Bonsi cards, iCloud sync (iOS), Apple Watch quick-log.
- **Testable:** multi-goal trees reflect per-goal hours; sync does not duplicate or lose entries.
- **Outcome:** better paid conversion and retention.

---

## Skill Loadout & Quality Gates

### Skills Used During Build

| Skill | When It Fires | Purpose |
|-------|--------------|---------|
| PAUL | Whole build (required) | Structured milestones and phases |
| AEGIS | After Phase 1 and before Phase 5 | Security and quality audit |
| sonarqube scan | End of each phase (optional) | Code quality |
| ui-ux-pro-max | Not used | Targets web frontends; the native UI and art come from SwiftUI and the artist |

### Quality Gates

| Gate | Threshold | When |
|------|-----------|------|
| Unit tests | All pass; cover balance math, goal/legacy decoding, Bonsi stage derivation | Each phase |
| Privacy | Matches the "Data Not Collected" label; no third-party SDKs | Phases 1 and 5 |
| Accessibility | VoiceOver labels and Dynamic Type checked | Before Phase 5 |
| Performance | Smooth scrolling with animated backdrop on the oldest supported device | Phases 4 and 5 |
| TestFlight smoke test | Clean install, onboarding, log, timer, Health import | Each TestFlight build |

---

## Design Decisions

1. **Business lens**: treated as an emerging app idea run as a business, validated through friends and family before public launch.
2. **Positioning**: reward growth instead of blocking fun, at a realistic 1:5 growth-to-fun ratio — competitors restrict; none found reward.
3. **Audience**: anyone who wants to grow, with a narrow marketing hook at launch — keeps the product broad but the story sharp.
4. **Bonsi is the retention anchor**: a source of accomplishment and motivation, and the main reason users stay.
5. **Gentle reminders**: Bonsi may lose leaves when growth is neglected, but never dies, and regrows on the next log — avoids guilt-driven uninstalls.
6. **Monetization**: freemium with a one-time lifetime unlock for multiple goals/trees and skins; no subscription — matches the founder's preference and the privacy stance.
7. **Hard rules**: no selling data, no annoying push notifications, minimal data retention (local-first, no account).
8. **Platform**: native iOS first, native Android later; two codebases accepted because the app is lightweight; Android widens the market and several friends use it.
9. **Distribution**: TestFlight friends-and-family MVP by Thanksgiving 2026-11-26; Android follows.
10. **Derived state**: Bonsi's stage and leaf state are computed from entries, not stored — supports minimal data retention.
11. **Art pipeline**: use Rive or Lottie so the artist's work serves both platforms.
12. **Support resources**: AI assistance and a friend who is a graphic artist; paid marketing and operations help comes after validation.

---

## Open Questions

1. **Android friends in the meantime:** should they wait for Phase 3, or get a demo video? What is the Android MVP date? (Assumed mid-January.)
2. **Feedback form:** assumed acceptable as optional and external. Confirm.
3. **Apple Developer account:** to be purchased; the existing team ID in `project.yml` may be a free personal team. Confirm account type.
4. **Artist terms:** pricing, asset ownership, and any equity are undiscussed. Settle in writing before art work begins.
5. **Pricing of the lifetime unlock** (about $12-20) and which items are free vs paid (history and trends in particular).
6. **Screen Time tracking:** is it wanted, and what entitlements would it need?
7. **Budget** for art and marketing (not yet stated).
8. **Later help:** what form (contractors, partner, equity) is wanted for marketing, deployment, and upkeep?
9. **Remaining market research:** Daylio, Streaks, Gentler Streak, Apple's own apps.
10. **App name and trademark** check, and the Bonsi character name.

---

## Next Actions

- [ ] Buy the Apple Developer Program account today.
- [ ] Commit the Bonsi and LivingSky work; run the tests.
- [ ] Add `schemaVersion` to `balance.json`.
- [ ] Write the privacy policy page and create the feedback form.
- [ ] Archive, upload to TestFlight, and invite 5-15 testers before 2026-11-26.
- [ ] Talk to the artist about pricing, ownership, equity, and Rive/Lottie.
- [ ] Decide the Android MVP date and set up a Play Console account.

---

## References

- Existing code: `Balance/` (Models, Store, HomeView, BalanceView, BonsiView, LivingSky, HealthSync), `project.yml`, `BalanceTests/`
- [Opal pricing](https://www.opalapp.com/pricing)
- [Opal founder interview (RevenueCat)](https://www.plushcap.com/content/revenuecat/blog/revenuecat-if-people-pay-thats-a-strong-signal-youve-built-something-valuable-kenneth-schlenker-opal)
- [Finch pricing and reviews](https://apppricinglab.com/app/apple/1528595748)
- [Screen time app comparison](https://habitdoom.com/blog/best-apps-reduce-screen-time)
- [RevenueCat subscription benchmarks 2026](https://www.revenuecat.com/blog/growth/subscription-app-trends-benchmarks-2026.md)
- [AppsOps retention benchmarks](https://appsops.store/news/subscription-retention-benchmarks-app-economy-mid-2026)

---

*Last updated: 2026-11-17*
*Planned with SEED v1.0 · Chris AI Systems · For the official Agentic OS and to permanently remove attribution, visit https://chrisai.cv/skool*
