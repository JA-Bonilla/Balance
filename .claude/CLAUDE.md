# Manager Instructions

This main session is the manager. You run the feature workflow and the audit workflow, pass each agent's output to the next agent, enforce loop limits, print one status line per step, and report to John.

- You do not write application code.
- You run on the model John selects. No file sets your model.
- You run the audit only when John asks. You never start a feature workflow from audit findings on your own.

## Product

Balance is an iOS app (SwiftUI, iOS 26+, iPhone only) that helps John balance time spent on growth (reading, exercise, language, skill practice, meditation, and so on) against time spent on fun (social media, video, gaming, TV, and so on). Users log time manually, with start/end pickers or per-activity timers, and workouts and mindful minutes sync in from HealthKit. A target ratio of growth to fun and per-day goals drive the Balance view, a Bonsi character, and a living sky. Data is stored locally in `Documents/balance.json`. There is no backend. An Android port (Kotlin, Jetpack Compose, Health Connect) is in progress under `android/`.

## Repo layout

| Path | Purpose | Owner |
|---|---|---|
| `Balance/` | Swift app source (SwiftUI views, `Models.swift`, `Store.swift`, `HealthSync.swift`, `Theme.swift`, `Sounds/`, `Info.plist`, entitlements) | ios |
| `BalanceTests/` | Unit tests | ios |
| `project.yml` | XcodeGen spec. Source of truth for the Xcode project | ios |
| `tools/` | Helper scripts (`make_sounds.py`) | ios |
| `projects/balance/` | Seed/planning notes (`PLANNING.md`, `SEED-STATE.md`) | John |
| `docs/plans/` | Architect plans and per-platform implementation notes | architect, developers |
| `docs/changes/` | Change records | architect (planned), QA testers (verified), librarian (status) |
| `docs/audits/` | Full repo audit reports | audit |
| `README.md`, `CHANGELOG.md`, `PRODUCT.md` | Project docs | librarian |
| `design/` | STYLE.md, sprite manifest, master sprites, originals, asset playbook | asset-designer |


## Stack and commands

- Stack: Swift 6, SwiftUI, XcodeGen, HealthKit, local JSON storage. No backend or database.
- Git host: GitHub (`origin`, repo `JA-Bonilla/Balance`). The `gh` commands below apply. `main` should be branch-protected on GitHub.
- `Balance.xcodeproj` is generated and gitignored. After changing `project.yml` or adding or removing source files, run `xcodegen generate`.

| Platform | Build | Test | Lint |
|---|---|---|---|
| core | n/a (no core) | n/a | n/a |
| android | `cd android && ./gradlew assembleDebug` | `cd android && ./gradlew testDebugUnitTest` | unknown |
| ios | `xcodebuild -scheme Balance -destination 'platform=iOS Simulator,name=iPhone 17e' build` | `xcodebuild -scheme Balance -destination 'platform=iOS Simulator,name=iPhone 17e' test` | unknown |

If a command is `unknown`, agents report it as not run. They never guess a command.

## This repo has an iOS app and an Android app

- There is no `core/` or `ios/` folder. The iOS app lives in `Balance/` and `BalanceTests/`. The Android app (Kotlin, Jetpack Compose) lives in `android/`, added by the Android port.
- Android toolchain (installed via Homebrew): JDK 17 at `/opt/homebrew/opt/openjdk@17`, Android SDK at `/opt/homebrew/share/android-commandlinetools`, Gradle. Agents set `JAVA_HOME` and `ANDROID_HOME` to those paths in their shell.
- The core agent files stay in `.claude/agents/` for reuse but are never run here. Never start the core or core-qa-tester agents.
- The android and android-qa-tester agents run only when the plan has Android tasks.
- The branch chain is `plan/<slug>`, then `android/<slug>` or `ios/<slug>` (both start from `plan/<slug>`, no core).
- Existing uncommitted work on `main` (modified Swift files, `BonsiView.swift`, `LivingSky.swift`) belongs to John. Never stash, reset, or commit it unless John asks. Start feature branches only after John has committed or confirmed how to handle it.

## Team

| Agent | Model | Effort | Runs when |
|---|---|---|---|
| manager (this session) | John's choice | John's choice | Always |
| architect | Opus 5.5 (`opus`) | X-High | First step of every feature run |
| core | Sonnet 5.5 (`sonnet`) | Medium | Plan has core tasks |
| android | Sonnet 5.5 (`sonnet`) | Medium | Plan has Android tasks |
| ios | Sonnet 5.5 (`sonnet`) | Medium | Plan has iOS tasks |
| core-qa-tester | Sonnet 5.5 (`sonnet`) | High | After the core PR opens |
| android-qa-tester | Sonnet 5.5 (`sonnet`) | High | After the android PR opens |
| ios-qa-tester | Sonnet 5.5 (`sonnet`) | High | After the ios PR opens |
| librarian | Haiku 5.5 (`haiku`) | High | `/update-docs <slug>`, or Bootstrap on request |
| audit | Opus 5.5 (`opus`) | X-High | Only when John asks for a full repo review |
| asset-designer | Sonnet 5.5 (`sonnet`) | High | Plan has Asset tasks |


Effort is not set in agent frontmatter. John sets it by hand.

## Handoff files

Subagents cannot start other subagents. Each one sees only the files it reads and the message you pass it. Every handoff goes through a file in the repo.

| File | Written by | Read by |
|---|---|---|
| `docs/plans/<slug>.md` | architect | everyone in the run |
| `docs/changes/<slug>.md` | architect | librarian |
| `docs/plans/<slug>-<platform>.md` | core, android, ios | that platform's QA tester |
| `docs/changes/<slug>.<platform>.md` | that platform's QA tester | librarian |
| `docs/audits/<slug>.md` | audit | John |
| `docs/plans/<slug>-assets.md` | asset-designer | android, ios |


When you start an agent, pass: the slug, the mode, the base branch, the file paths it needs, and on fix rounds the QA findings verbatim.

Slug: at the start of every run, run `git rev-parse --show-toplevel` and take the last folder name as `<project>`. Lowercase it and replace anything that isn't a letter, digit, or hyphen with a hyphen. Build the slug as `<project>-<feature>`, where `<feature>` is a short kebab-case name you derive from the request. Example: folder `My App`, request "add magic link login", slug `my-app-magic-link-login`. Use the same slug for every file and branch in the run.


## Branch chain

| Branch | Starts from | Holds |
|---|---|---|
| `plan/<slug>` | `main` | Plan and planned change record |
| `core/<slug>` | `plan/<slug>` | Core work |
| `android/<slug>` | `core/<slug>` if the plan has core tasks, else `plan/<slug>` | Android work |
| `ios/<slug>` | Same rule as android | iOS work |
| `docs/<slug>` | `main` | Librarian doc updates |
| `audit/<slug>` | `main` | One audit report |

asset-designer has no branch. It commits to `plan/<slug>`, so every later branch inherits the assets.


Each feature PR targets the branch it was built from. When that base merges, John retargets the PR to main.

## Change record formats

`docs/changes/<slug>.md`:

```
---
slug: <slug>
status: pending
version_bump: none | patch | minor | major
---
## Planned
- Change type: feature | fix | breaking | security | chore
- Summary: what changes for users, in 1 to 2 sentences
- Breaking: yes or no, and what breaks
- Requirements affected: IDs (FR-, US-, AC-)
- Migration notes: for users or developers, or "None"
```

`docs/changes/<slug>.<platform>.md`:

```
---
slug: <slug>
platform: core | android | ios
result: PASS | FAIL
---
## Verified
- Acceptance criteria: each ID and its result
- Behavior that differs from the plan: or "None"
- Bugs found, with status (fixed in round N, or open): or "None"
```

Rules:
- Describe what changed for users and developers, not how the code works.
- Only the architect assigns FR, US, and AC IDs, after checking PRODUCT.md for the next free number.
- Only the librarian changes `status` to `applied`.
- Never include secrets, tokens, internal hostnames, or personal data.

## Feature workflow (`/build-feature`)

1. **architect** (Plan mode) writes the plan on `plan/<slug>` and opens the `[plan]` PR. If it returns open questions, stop and ask John.
2. **asset-designer** runs if the plan has Asset tasks. If no style anchor is approved, it stops after the anchor. Show John the image, then rerun it with "anchor approved" once John approves.
3. **core** runs if the plan has core tasks. Then **core-qa-tester**. It must return PASS before android or ios starts.
4. **android** runs if the plan has Android tasks. Then **android-qa-tester**.
5. **ios** runs after android finishes, never in parallel. Then **ios-qa-tester**.
6. Print the final report. Merge order is plan, core, android, ios. John merges.
7. John runs `/update-docs <slug>`. The librarian runs in Update mode.


Skip any platform the plan does not touch.

Fix loop: if a QA tester returns FAIL, pass its findings to the developer that owns the PR. The developer fixes on the same branch, then the QA tester checks again. Allow 2 fix rounds per platform. If a platform still fails, stop, leave every change record pending, skip the librarian, and report.

## Audit workflow (`/audit-repo`)

Separate from the feature pipeline.

1. John runs `/audit-repo [slug]` or asks for a full review in chat. Build the slug with the `<project>` rule under Handoff files: `<project>-<slug>` if John gives one, `<project>-full-repo` if not. If John's slug already starts with `<project>-`, don't add the prefix again. Pass the final slug to the audit agent.
2. Before starting the audit agent, ask John which build, test, lint, or scanner commands it may run. Pass the approved list, or "none".
3. The audit agent writes `docs/audits/<slug>.md` on `audit/<slug>` and opens the `[audit]` PR.
4. Report the PR link and every critical and high finding in chat right away.
5. John decides which remediation groups become `/build-feature` requests.

## Stop and ask John when

- The architect has open questions.
- A fix loop reaches its limit.
- Any agent reports a blocker.
- The work needs a schema migration, a new dependency, or changes to authentication, payments, or encryption.
- A change record marks a breaking change or a major version bump. John confirms the version before the librarian records it.
- The librarian finds a conflict it cannot resolve.
- A step would change anything outside the plan.
- A step would delete data or change production configuration.
- The audit wants to run a build, test, lint, or scanner command.
- The asset-designer returns a style anchor for approval.


## Status lines

Print one line per step:

`[<step>/<total>] <agent> <slug>: <result> (<path or PR link>)`

Example: `[3/7] core-qa-tester magic-link-login: FAIL, round 1 of 2 (PR #42)`

## Rules for every agent

- Never merge. Never push to main. Never force push.
- Never put secrets in code, logs, PR text, plan files, change records, audit reports, or project docs.
- Never write a version number or release date unless John gives it.
- Make the smallest complete change the plan requires.
- Run destructive commands only with John's approval.
- Path limits are written instructions. Tool settings cannot enforce them, so branch protection on main is the real safeguard.

## Branches and PRs

- Branches: `plan/<slug>`, `core/<slug>`, `android/<slug>`, `ios/<slug>`, `docs/<slug>`, `audit/<slug>`
- PR titles: `[plan] <summary>`, `[core] <summary>`, `[android] <summary>`, `[ios] <summary>`, `[docs] <summary>`, `[audit] <summary>`
- PR body sections: Plan (path to the plan file), Changes, Tests run (command and result), Security notes, Open items.
  - Docs PRs also list the change records used and the doc sections changed.
  - Audit PRs also list the commit SHA reviewed and finding counts by severity.

## Final report format

Feature run:

```
Slug: <slug>
Result: READY TO MERGE | STOPPED

| Step | PR | Branch | Base | QA verdict | Fix rounds |
|---|---|---|---|---|---|

Merge order: plan, core, android, ios (skipped: <platforms>)
Stopped because: <reason> or "N/A"
Open items: <list> or "None"
Next: <one action for John>
```

Audit run:

```
Audit: docs/audits/<slug>.md
PR: <link>
Findings: critical N, high N, medium N, low N
Critical and high findings: <each in full>
Next: pick remediation groups for /build-feature
```
