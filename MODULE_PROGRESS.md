# Physi-Lock Module Progress Checker

Tracks progress against the module-based development lifecycle in `PROJECT_DOCUMENTATION.md` (see its "Development Lifecycle" table for the canonical module list). Check items off as they're built; update the module-level status line whenever a module's checklist changes meaningfully. Last synced: 2026-08-21.

**Note on scope:** the per-module checklists below map the manuscript's 21 User + 6 Admin functions (see FDD memory) onto modules by function name/semantics. That mapping is my best-effort inference, not a confirmed 1:1 copy of the manuscript's 13-submodule breakdown — cross-check against the manuscript FDD if exact submodule wording matters for grading/documentation.

---

## Summary

| Status | Modules |
|--------|---------|
| ✅ **Done** | Admin Governance Layer |
| 🟢 **Most Advanced** (built, may need polish) | 0. Foundation, 1. Core Monitoring & Usage Awareness, 3. Motion-Responsive Locking |
| 🟡 **Partial / In Progress** | 4. Smart Intervention System, 6. Personalization & User Control, 8. Integration & QA (ongoing by nature) |
| 🔴 **Not Started** | 2. AI-Based Behavior Analysis, 5. Mental Health & Awareness, 7. Context-Aware AI |

---

## UI Status (screen-by-screen cut)

Same underlying progress as the module checklists below, viewed by screen instead of by feature module — useful for "what does the app actually look like right now" questions. Last checked: 2026-08-03.

**Built**
- [x] `LandingScreen` — hero section, module list (text-based — flagged for visual redesign, backlog #6 in [[project_next_session_tasks]]), CTA
- [x] `AuthScreen` — Login/Register toggle, all fields, error display, "Continue with Google" button (2026-08-02, UI-only)
- [x] `OnboardingScreen` — YPT-style "Continue X/N" permission flow, wired into the real nav flow
- [x] `HomeScreen` (`ui/home/HomeScreen.kt`) — today's screen time (with real configured daily limit + progress ring), lock-sensitivity ring, locked-apps summary. Redesigned 2026-08-11, see below.
- [x] `ReportsScreen` (`ui/reports/ReportsScreen.kt`) — real weekly bar chart (dynamic scale, over-limit coloring), stat cards, top apps, rule-based insights. Redesigned 2026-08-11, see below.
- [x] `MoveScreen` — shake-challenge streak/move-to-unlock
- [x] `SettingsScreen` — user mode, screen-time limit, doomscroll toggle, motion sensitivity, break reminder toggle+interval, account editor (username/fullName/occupation, email read-only), Log Out button
- [x] `AppLockRulesScreen` — pick installed apps to lock
- [x] `LockScreen` / `LockActivity` (`ui/lock/`) — shake-to-unlock progress overlay
- [x] `BottomNavBar` / `NavGraph` — bottom nav + routing
- [x] `AdminHomeScreen` + 4 tab sections (Accounts/Categories/Defaults/Analytics)

**File structure & naming alignment (2026-08-11):** two things folded into one session — (1) this repo's own internal folder organization was inconsistent (`HomeScreen.kt`/`ReportsScreen.kt` sat in a leftover `ui/screens/` folder while their ViewModels already lived in `ui/home/`/`ui/reports/`; `LockActivity.kt`/`LockScreen.kt` sat loose directly in `ui/` with no feature folder) — fixed by moving both screens into their ViewModels' folders and creating `ui/lock/` for the lock files, updating all imports and `AndroidManifest.xml`. (2) A teammate's separate UI repo (`github.com/fionapalacios/Physilock`, real work is on branch `sprint-2-ui-navigation` — `main` is just an empty scaffold) was cloned and compared. Package name differs (`com.prototype.physi_lock` vs this repo's `com.example.physi_lock`) and a real token collision was found: both theme files define a color named `DeepOlive`, but with *different hex values* (`#3D4928` here vs `#536136` there, which is what this repo separately calls `MutedText`) — a genuine naming collision, not just a style difference.

Decision: for the three screens with a functional overlap, take the teammate's visual design but keep this repo's real data layer (Firebase/Room/`UsageStatsManager`) — teammate's versions are UI-only mockups with **no backend**, in two cases entirely hardcoded fake data (teammate's `DashboardScreen.kt` ships a fixed list of fabricated notifications and a fake "AI" predictive-overuse claim; `ReportsScreen.kt` is 100% invented stats, no ViewModel at all). Per-screen outcome:
- **Home** — redesigned using teammate's visual language (progress rings via a new `ui/components/CircularProgressRing.kt`, restyled cards) wired to real `HomeViewModel` data. Added a real `dailyLimitMinutes` StateFlow (was previously hardcoded "240m"). Dropped the fabricated break-reminder banner, "predictive overuse AI" banner, and static goals row — none were backed by real data on either side.
- **Reports** — redesigned the same way, wired to real `ReportsViewModel` data (added a real `dailyLimitMinutes` StateFlow there too, used for over-limit bar coloring). Kept the honest "Insights" label rather than teammate's "AI-POWERED" badge, since these are rule-based, not ML. Skipped teammate's category-breakdown card and week/month toggle — no real per-category usage aggregation exists yet to back it (candidate for later, once Module 2 or a category-aware usage query exists).
- **Settings** — teammate's `SettingsScreen.kt`/`StudentModeScreen.kt`/`WorkModeScreen.kt` turned out to be empty (0-byte) stub files, never actually written. Kept this repo's existing `SettingsScreen` as-is; revisit once the teammate's branch has real content.
- **Move, Onboarding/permissions, Landing** — kept this repo's existing versions (user's explicit choice); teammate has visually different `MoveScreen`/`GrantPermissionsScreen`/`GetStartedScreen` equivalents not adopted.

New shared theme tokens added to `Color.kt` to support the above and future ports from teammate's branch: `BackgroundLight`, `SecondarySage`, `TertiaryTan`, `ErrorRed`. (Teammate's `PrimaryDark`/`PrimaryGreen`/`AccentLavender` already had equivalents here — `DeepOlive`/`SageAccent`/`Orchid` — reused rather than duplicated.)

**Deferred to next session** (files identified, not yet ported — see [[project_next_session_tasks]]): teammate's branch also has real, non-overlapping screens that fill several items in the "Not built" list below — split `LoginScreen`/`CreateAccountScreen`/`ForgotPasswordScreen`/`VerifyEmailScreen` (auth), `FocusModeScreen`/`EndFocusSessionSheet` (Focus Mode), `MotionChallengeScreen`/`ChallengeActiveScreen`, `UsageGoalsScreen`, and shared components `AuthComponents`/`DashboardCard`/`NotificationsPanel`. None of these conflict with existing screens, but the auth screens call a `data.AuthRepository` that doesn't exist here (this repo uses `AccountRepository`/`FirebaseAccountRepository`) — porting them will need real rewiring, not just a file copy.

**Corrections (2026-08-10):** two "not built" items below were stale — verified against current code, both already exist:
- **Logout** — `SettingsScreen`'s `AccountSummaryCard` already had a Log Out button, wired `MainActivity` → `NavGraph` → `SettingsScreen`, since before this entry was last written. It just didn't actually call Firebase — it only reset local UI state (`currentAccount = null`, navigate to `AUTH`), leaving the Firebase Auth session alive underneath. Fixed today: added `logout()` to `AccountRepository`/`FirebaseAccountRepository` (`auth.signOut()`) and `HybridAccountRepository` (delegates — purely local, no offline handling needed), `AuthViewModel.logout(context)` calls it plus best-effort clears Credential Manager's cached Google account state, and `MainActivity`'s `onLogout` now calls it before resetting UI state.
- **Occupation field** — already present in `SettingsScreen`'s account editor (`OutlinedTextField` for occupation, editable). Not a gap.

**Not built** (confirmed via codebase search 2026-08-02 — no matching files/composables exist)
- [ ] Forgot-password flow — "Forgot password?" link exists, wired to a no-op; no reset-link → verification-code → new-password screens. Teammate's `ForgotPasswordScreen.kt` exists and could fill this — see "Deferred to next session" above.
- [ ] Email verification screen — no UI to enter/confirm a verification link/code (nor the local-simulation backend for it). Teammate's `VerifyEmailScreen.kt` exists — see "Deferred to next session" above.
- [ ] Mode-picker screen (Student/Work, chosen post-onboarding per [[project_auth_screens]]'s confirmed flow) — `SettingsScreen` has a mode toggle, but the dedicated post-login picker screen doesn't exist. Teammate's `StudentModeScreen.kt`/`WorkModeScreen.kt` are empty stubs, so no source to port from yet.
- [ ] Notifications UI — listed as a UI component in `PROJECT_DOCUMENTATION.md`/`PRESENTATION_SCRIPT_GUIDE.md`, but no such screen/composable exists. Teammate's `NotificationsPanel.kt` exists — see "Deferred to next session" above.
- [ ] Focus Mode, Reflection Prompts, Context Alerts screens (Modules 5/6/7). Teammate's `FocusModeScreen.kt`/`EndFocusSessionSheet.kt` cover Focus Mode — see "Deferred to next session" above. (Break Reminders no longer belongs on this list — see Module 4 below.)
- [ ] Real risk-score display (currently a placeholder value in `HomeScreen`, blocked on Module 2)

---

## ✅ Admin Governance Layer (Done — 2026-08-02)

Not one of modules 0–8 (those map to User-facing feature groups); Admin is a separate cross-cutting governance layer per [[project_physi_lock_overview]].

- [x] Manage User Accounts — list/activate/deactivate/delete (`AdminAccountsSection`, `AdminViewModel`)
- [x] Curate App Categories — per-installed-app category master list (`AdminCategoriesSection`, `AppCategory`)
- [x] Configure Default Settings — global defaults for new accounts (`AdminDefaultsSection`, `DefaultSettings`)
- [x] View Usage Analytics — aggregate stat cards + top apps (`AdminAnalyticsSection`)
- [x] Export Research Data — plain-text share-sheet export
- [x] Authenticate Users — resolved as architectural framing only (Admin owns/governs the auth subsystem; no separate approval-gate code needed, see [[project_physi_lock_overview]])

**Known gap (2026-08-09): no error-state handling.** `AdminViewModel` swallows failures silently instead of surfacing them — `loadAnalytics()` catches Firestore/Room read errors and falls back to `0`/`emptyList()` rather than an error state, and `accountsFlow()` closes the Firestore listener on error with nothing shown to the UI (list just goes stale). None of `AdminAccountsSection`/`AdminAnalyticsSection` have a "couldn't load, tap to retry" state, and toggle/delete/category-save/settings-save actions have no failure feedback (fire-and-forget `viewModelScope.launch`, no try/catch, no snackbar on failure). Admin also has no offline fallback at all — unlike User login (`HybridAccountRepository`, see Module 0 below), Admin's accounts/categories/defaults/analytics are 100% live-Firestore/Room-dependent with nothing cached for when the connection drops. Figma Make prompts for the Admin screens already spec the intended error/empty/offline UI (retry buttons, inline "couldn't save" tags, an offline banner) — this entry tracks that the Kotlin side hasn't been built to back it yet.

---

## 🟢 Module 0: Foundation

- [x] Database schema, DAOs, Room singleton
- [x] Permissions declared in manifest
- [x] AccessibilityService (`AppMonitorService`)
- [x] Navigation shell (`NavGraph`, `BottomNavBar`)
- [x] Register Account / Log In (`HybridAccountRepository` wrapping `FirebaseAccountRepository` — Firebase Auth + Firestore `users/{uid}` as primary, replacing the old local Room simulation)
- [x] Grant Permissions — YPT-style "Continue X/N" flow in `OnboardingScreen.kt`, wired into `MainActivity`'s navigation (2026-08-02): regular Users land there right after login/register, Admins skip straight to `ADMIN_HOME`
- [x] Manage Account Settings (username/fullName/occupation editable in `SettingsScreen`; email intentionally read-only — see caveat below)
- [x] Log Out — button already existed in `SettingsScreen`'s `AccountSummaryCard`; fixed 2026-08-10 to actually call `AccountRepository.logout()` (→ `auth.signOut()`) instead of only resetting local UI state. See runtime-verification entry below.
- [x] Firebase Auth migration (2026-08-07) — `Account`/`AccountDao` Room entity retired entirely; `FirebaseAccountRepository` (`data/auth/`) handles register/login/updateAccount against Firebase Auth + Firestore. **Correction (2026-08-09): a real Firebase project does exist** — `app/google-services.json` is present (project `physi-lock`, placed 2026-08-07) and processes cleanly (`default_web_client_id` resource confirmed generated in `app/build/generated/res/processDebugGoogleServices/`, including a web OAuth client for Google Sign-In). The earlier "no Firebase project exists yet" note was stale. **Still unverified: actual runtime behavior against that project.** Nobody has run the app against it yet, so unconfirmed: whether the Email/Password provider is enabled in the Firebase console, whether Firestore is created with usable security rules (default rules deny all reads/writes), and whether the demo Admin account was ever manually created (Auth user + Firestore doc with `role: ADMIN`) as this section previously instructed.
- [x] Google Sign-In (OAuth 2.0/OIDC) — Credential Manager + `GoogleIdTokenCredential` flow wired end-to-end in `AuthViewModel.signInWithGoogle`/`MainActivity` (2026-08-07). Same runtime-unverified status as above, plus one Google-Sign-In-specific risk: it needs the app's SHA-1 certificate fingerprint registered against the `physi-lock` Firebase project, or it fails at runtime with a DEVELOPER_ERROR even though the code/build config is otherwise correct — unconfirmed whether that's been done.
- [x] Offline login fallback (2026-08-09) — `HybridAccountRepository` (`data/auth/`) wraps `FirebaseAccountRepository`; every successful online login/register writes a salted-hash snapshot (`PasswordHasher`, PBKDF2WithHmacSHA256) to a new local Room cache (`CachedAccount`/`CachedAccountDao`, DB version 5→6). If a login call throws (Firebase/Firestore unreachable), it falls back to verifying against that cache. Scope is login-only — registration and profile updates still require Firebase; Google Sign-In is unaffected (still online-only). `AuthViewModel` now extends `AndroidViewModel` to reach the DB. **Verified:** `gradlew compileDebugKotlin` passes. **Not yet verified:** no runtime test of the actual offline path (e.g., killing network mid-session in an emulator).
- [x] **Runtime-verified 2026-08-09 (partial):** user ran the app and confirmed Google Sign-In works end-to-end — account visible in both Firebase Auth console and Firestore `users` collection. Email/password registration errored, root-caused and fixed same day: `FirebaseAccountRepository.register()` was querying Firestore (username uniqueness check) *before* `auth.createUserWithEmailAndPassword`, i.e. while still unauthenticated — fails under any Firestore rules that scope reads to signed-in users. Fixed by reordering: Auth now runs first, Firestore check second, with a rollback (`authResult.user?.delete()`) if the username turns out taken. Added `firestore.rules` at repo root (not auto-deployed — must be pasted into the Firebase Console manually, no CLI configured in this repo) with the corresponding access model: any signed-in user can read/query `users`, but can only write their own doc (Admin gets an update/delete bypass for account management). **Gap closed same day:** added a public `usernames/{username} → { email }` lookup collection (`firestore.rules`) so login-by-username keeps working under the auth-scoped rules. Deliberately minimal — the doc holds only `email`, and rules only permit exact-ID `get` (never `list`/enumeration), so it can't be used to browse all usernames, only resolve one you already know. `resolveEmail()` now does a `usernamesCollection.document(identifier).get()` (public, pre-auth-safe) instead of the old `whereEqualTo` query (which required auth and would've failed pre-login). Write-side: `register()`, `updateAccount()` (on username rename — deletes the old mapping, writes the new one), and `signInWithGoogleIdToken()`'s first-time provisioning all keep this collection in sync, best-effort (a failed lookup write doesn't fail the whole registration/update — it only degrades that account's future username-login, email login is unaffected). Rules scope create/update/delete to `request.auth.token.email` matching the mapping's own email, so you can't overwrite or delete someone else's username mapping. **Not yet runtime-verified** — same as the rest of this Firebase work, needs an actual login-by-username test once the updated rules are pasted into the console.
- [x] **Runtime-verified 2026-08-10:** user pasted the updated rules and confirmed login-by-username works. Also confirmed the offline-fallback design is behaving exactly as intended: registering a *new* account with no internet correctly fails (expected — registration has no offline path, needs Firebase to reserve the username/email); registering that same account with internet then worked; logging into that freshly-registered account with no internet then succeeded (the `HybridAccountRepository` cache written at registration time backing the offline login as designed). **Log Out fix same day** — see Foundation checklist above; not yet runtime-verified (need to confirm `auth.currentUser` is actually null after tapping it, e.g. via Logcat or by checking the Firebase Auth console's "last sign-in" doesn't show the app still active).

## 🟢 Module 1: Core Monitoring & Usage Awareness

- [x] View Usage Tracker (`HomeScreen`, `HomeViewModel`)
- [x] View Screen Time Breakdown (`ReportsScreen`, `ReportsViewModel`)
- [x] Foreground app tracking, usage aggregation, scroll-event field (counting logic itself deferred to Module 2)
- [ ] View Usage Patterns (`ReportsViewModel`'s "insights" are simple rule-based observations, explicitly not pattern-recognition)
- [ ] Receive Overuse Alerts (no notification/alert mechanism built yet)

**Bug found + fixed (2026-08-10): displayed screen time didn't match real device usage.** Root cause: `HomeViewModel`'s "today's screen time" and `ReportsViewModel`'s weekly chart/top-apps were entirely sourced from `AppUsageLog` — rows `AppMonitorService` (the AccessibilityService) only writes when it observes a foreground-app *switch* while actively running. That has no way to reflect usage from before the service started watching, so it structurally couldn't match the phone's real total; it'd read low or zero. The fix: `UsageStatsRepository` (already built, already gated by the "Usage Access" permission already requested in onboarding, but never actually called for this) wraps Android's real `UsageStatsManager` — the same source Digital Wellbeing/Settings' screen-time page reads. `HomeViewModel` now sums `getTodayUsage()` on every `ON_RESUME`. `ReportsViewModel` now queries one day at a time via the new `getUsageForRange(startMillis, endMillis)` (folds 7 single-day queries into the weekly chart + a client-side top-apps aggregate) rather than a single 7-day range query, since `UsageStatsManager`'s multi-day bucket-aggregation behavior isn't reliable enough to trust — see kdoc on that method. `AppUsageLog`/`AppMonitorService` are untouched and still needed for real-time, event-driven behavior (lock-triggering, per-session data, future scroll-event/doomscroll counting) — this fix only changes where the *displayed totals* come from. Also found and left alone: `UsageSync.kt`'s `syncTodayUsage()` was a half-built earlier attempt at this exact fix — dead code (never called), and even if wired up it writes to the legacy `UsageSession` table, not the one either ViewModel reads. Verified with `gradlew compileDebugKotlin`; not yet runtime-tested against real device usage.

## 🟢 Module 3: Motion-Responsive Locking

- [x] Perform Move-to-Unlock (`MoveScreen`, `ShakeDetector`, `LockScreen`)
- [x] Customize Lock Rules — per-app lock on/off (`AppLockRulesScreen`)
- [x] Motion Lock Sensitivity setting wired to shake challenge (`SettingsScreen`)
- [ ] Trigger Adaptive Lock — `AppLockRule.lockType` has an `ADAPTIVE` option in the schema, but difficulty doesn't yet scale off a real risk score (blocked on Module 2)

## 🟡 Module 4: Smart Intervention System (Partial)

- [x] Set Usage Limits — daily screen-time threshold slider (`SettingsScreen`, `AdminDefaultsSection`); not the goal-based/YPT-style limits from [[project_inspiration_apps]]
- [ ] Complete Activity Challenges (only the shake challenge exists; no separate "activity challenge" mechanic)
- [x] Receive Break Reminders (2026-08-21) — user-configurable continuous-foreground-usage timer (`UserConfiguration.breakReminderEnabled`/`breakReminderIntervalMs`, default on/30 min; toggle+slider in `SettingsScreen`), tracked in `AppMonitorService` (accumulates across accessibility events, resets on a >2 min idle gap — a proposed default, not manuscript-specified), posts a repeating Android notification via a new `break_reminder_channel` (`IMPORTANCE_HIGH`, heads-up banner). Deliberately independent of doomscroll pattern-detection (separate, still-unbuilt Module 2 classifier) — modeled on TikTok's own plain continuous-session break reminder instead. **Runtime-verified 2026-08-21** on a real device (vivo V2109, Android 13) with a temporary 1-min test interval — notification fired correctly with real (touch-driven) app switching, including the `IMPORTANCE_HIGH` heads-up banner (confirmed by manually bumping the already-created channel to "Urgent" in system Settings, since Android won't let an app change an existing channel's importance in code once created — a fresh install gets `IMPORTANCE_HIGH` automatically without this manual step). Two device-level gotchas found along the way, worth remembering for testing any other background-service feature on this same device: (1) no local emulator/AVD is configured on this machine — testing used the physical device over wireless ADB instead; (2) this vivo (Funtouch OS) build blocks `adb logcat` entirely, even for a manually shell-injected test log, so log-based debugging doesn't work here — dumpsys (`dumpsys notification`, `dumpsys accessibility`) is the only viable adb-based introspection; (3) vivo's per-app "Smart control" battery/autostart mode silently suppressed the background accessibility service's notifications even with the standard Android notification permission granted — had to be switched to explicit "No restrictions" in vivo's battery settings before anything fired. Also confirmed `adb shell am start`-simulated app switches do **not** reliably trigger the same accessibility events as real touch navigation on this device (the pre-existing App Lock feature also failed to trigger under simulated switches) — real on-device interaction is required to test any accessibility-event-driven feature here, adb alone isn't sufficient.
- [ ] Receive Overuse Alerts (not built)

## 🔴 Module 2: AI-Based Behavior Analysis (Not Started — paused 2026-08-02)

- [ ] Random Forest risk scoring — feature list confirmed from manuscript, dataset source + Python env still undecided (see [[project_next_session_tasks]])
- [ ] Logistic Regression I — doomscroll detection
- [ ] Logistic Regression II — hourly excessive-usage prediction
- [ ] m2cgen transpilation to Kotlin
- [ ] View Risk Score (UI currently shows `UserConfiguration.riskSensitivity`, an explicit placeholder mirroring motion sensitivity, not a computed score)
- [ ] Receive Overuse Predictions

## 🟡 Module 6: Personalization & User Control (Partial)

- [x] Select Usage Profile — Student/Work mode toggle (`SettingsScreen`)
- [x] Customize Lock Rules — covered under Module 3 above (shared deliverable)
- [ ] Enable Focus Mode (not built — candidate feature per [[project_inspiration_apps]])

## 🔴 Module 5: Mental Health & Awareness (Not Started)

- [ ] Answer Reflection Prompts

## 🔴 Module 7: Context-Aware AI (Not Started)

- [ ] Receive Context Alerts (location/context-based rules)

## 🟡 Module 8: Integration & QA (Ongoing — cross-cutting)

- [x] Manual build verification (`gradlew compileDebugKotlin`, `gradlew assembleDebug`) after each code change this session
- [ ] Unit tests (entities, DAOs, AI inference, timestamp calculations)
- [ ] Integration tests (DB persistence, AccessibilityService, sensor lifecycle, UI state)
- [ ] End-to-end tests (lock → shake → unlock, risk-scoring difficulty, settings persistence, rotation)
- [ ] Performance/battery profiling against `PROJECT_DOCUMENTATION.md`'s target metrics
- [ ] ISO/IEC 25010 validation pass
