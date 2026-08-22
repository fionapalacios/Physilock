# Physi-Lock Presentation / Script Guide

## 1) Opening line
**Say:**  
"Physi-Lock is a hybrid AI-based mobile locking system that helps reduce excessive smartphone use by combining on-device app monitoring and usage analytics, a physical shake-to-unlock challenge, and cloud-backed accounts with Admin governance."

## 2) How to frame the project
**Say:**  
"This presentation covers where Physi-Lock stands today across its development modules, and what the full MVP looks like once every module is built and working."

**Simple distinction**
- **Current build:** the modules already working — see the module status breakdown below for what's done
- **Full MVP:** the overall system with all modules built and working together — Foundation, Core Monitoring, AI-Based Behavior Analysis, Motion-Responsive Locking, Smart Intervention System, Mental Health & Awareness, Personalization & User Control, Context-Aware AI, and Integration & QA

---

## 3) Slide-by-slide presentation flow

### Slide 1 — Title
**Title:** Physi-Lock: AI-Based Mobile Locking System  
**Say:**  
"We built Physi-Lock to help users control screen time by making access to distracting apps require physical effort and by tracking usage locally on the device."

### Slide 2 — Problem
**Title:** Why this matters  
**Say:**  
"Most phone use is frictionless, so people keep opening apps without thinking. Physi-Lock adds intentional friction when usage becomes excessive."

### Slide 3 — Current module status
**Title:** What is working now  
**Say:**  
"Development is organized into modules — one per feature area — rather than a single prototype-vs-final split. Foundation, Core Monitoring, and Motion-Responsive Locking are the most advanced; Smart Intervention System and Personalization are partially built; AI-Based Behavior Analysis, Mental Health & Awareness, and Context-Aware AI haven't been started yet."

**Point out the current working pieces**
- Foundation: landing, auth, home, database layer, permissions, AccessibilityService
- Core Monitoring & Usage Awareness: home dashboard, reports, app monitoring, local database storage
- Motion-Responsive Locking: app lock rules, lock overlay, shake-to-unlock challenge
- Smart Intervention System / Personalization (partial): move challenges, settings, user mode
- Admin governance layer: account management, app-category curation, default settings, usage analytics & research export

### Slide 4 — Full MVP vision
**Title:** What the complete MVP will become  
**Say:**  
"The full MVP is the overall system with every module built and working together — not just the AI piece. The current priority is Module 2, AI-Based Behavior Analysis: Random Forest risk scoring, doomscrolling detection, and hourly excessive-usage prediction. Once that's in, the remaining modules — Smart Interventions, Mental Health & Awareness, Personalization, and Context-Aware AI — round out the full system."

**Remaining modules for the full MVP**
- AI-Based Behavior Analysis (current priority): Random Forest risk scoring, doomscroll & hourly-excessive classifiers
- Smart Intervention System: context-aware nudges, adaptive thresholds, break reminders
- Mental Health & Awareness: reflection prompts, wellbeing check-ins
- Personalization & User Control: usage profiles, custom lock rules, usage limits
- Context-Aware AI: location-based rules, contextual doomscroll detection
- Integration & QA: end-to-end testing, performance profiling, ISO/IEC 25010 validation

### Slide 5 — Frontend
**Title:** User-facing screens  
**Say:**  
"On the frontend, Physi-Lock uses Jetpack Compose to deliver the landing page, auth screen, onboarding flow, home dashboard, weekly reports, move challenge screen, settings, app lock rules, and the lock challenge screen."

### Slide 6 — Backend: hybrid on-device + cloud
**Title:** System and data layer  
**Say:**  
"The backend is hybrid. Usage tracking, AI inference, and motion-lock logic stay fully on-device for privacy and latency — Room, local DAOs, foreground app monitoring, usage statistics, and sensor-based shaking logic. Accounts, Admin-facing data, and aggregate/anonymized analytics are the cloud side, planned on Firebase Auth and Firestore. Right now, accounts run on a local Room simulation standing in for that Firebase layer until the migration happens."

### Slide 7 — APIs and Android services
**Title:** What APIs are used  
**Say:**  
"The system relies on Android platform APIs such as AccessibilityService, UsageStatsManager, SensorManager, PackageManager, and the overlay and settings intents needed for permissions and enforcement."

### Slide 8 — Demo flow
**Title:** How the app behaves  
**Say:**  
"A user opens the app, grants permissions, sets app lock rules, and then when a locked app is opened, Physi-Lock detects it and shows the shake challenge. Once the user completes the shake requirement, access is restored."

### Slide 9 — Value
**Title:** Why this design is useful  
**Say:**  
"Physi-Lock is privacy-first because sensitive usage and behavioral data stay on-device — the cloud is only used for accounts and anonymized aggregate analytics — and it is behavior-based because it does not just block apps — it encourages a physical action before unlocking."

### Slide 10 — Closing
**Title:** Summary  
**Say:**  
"In short, Physi-Lock is a hybrid, AI-ready digital wellness system that combines on-device monitoring, app restriction, and motion-based unlocks with cloud-backed accounts and Admin governance to reduce excessive phone use."

---

## 4) Current feature list

### Frontend
- `LandingScreen` — branded product intro and get-started CTA
- `AuthScreen` — sign in / register UI
- `HomeScreen` — today’s screen time, lock sensitivity, locked apps summary
- `ReportsScreen` — weekly usage, top apps, insights
- `MoveScreen` — challenge streak and move/unlock concept
- `SettingsScreen` — user mode, screen-time limit, doomscroll toggle, motion sensitivity
- `AppLockRulesScreen` — choose installed apps to lock
- `LockScreen` — shake-to-unlock progress UI
- `OnboardingScreen` — permission setup flow (available; not the current main entry flow)
- `BottomNavBar` and `NavGraph` — app navigation
- `AdminHomeScreen` — Admin governance dashboard (Accounts, Categories, Defaults, Analytics tabs)

### Backend / local logic
- `AppMonitorService` — AccessibilityService foreground app tracking
- `PhysiLockDatabase` — Room database singleton
- `AppUsageLog` / `AppUsageLogDao` — session usage logging
- `UserConfiguration` / `UserConfigurationDao` — preferences and thresholds
- `MotionInterventionLog` / `MotionInterventionLogDao` — lock event history
- `AppLockRule` / `AppLockRuleDao` — per-app lock policies
- `Account` / `AccountDao` — user & admin accounts (local simulation, pending Firebase Auth migration)
- `AppCategory` / `AppCategoryDao` — Admin-curated app category master list
- `DefaultSettings` / `DefaultSettingsDao` — Admin-configured defaults for new accounts
- `UsageStatsRepository` — reads usage stats from Android
- `UsageSync` — syncs usage into local storage
- `ShakeDetector` — accelerometer-based shake challenge
- ViewModels — `HomeViewModel`, `ReportsViewModel`, `SettingsViewModel`, `AppLockViewModel`, `MoveViewModel`, `AuthViewModel`, `AdminViewModel`

### Android APIs / services used
- `AccessibilityService`
- `AccessibilityManager`
- `UsageStatsManager`
- `AppOpsManager`
- `SensorManager`
- `PackageManager`
- `Settings.ACTION_USAGE_ACCESS_SETTINGS`
- `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
- `Settings.ACTION_ACCESSIBILITY_SETTINGS`
- `Room`
- `Coroutines` and `Flow`
- `Jetpack Compose`
- `Navigation Compose`
- `Lifecycle ViewModel`

---

## 5) What to say about backend/API usage
**Say:**  
"The core product loop — usage tracking, AI inference, motion-lock — has no external REST API or cloud dependency; that's Android services, sensor APIs, and a Room/SQLite database. Accounts and Admin governance are the cloud side, planned on Firebase Auth and Firestore; today that layer is a local Room simulation standing in until the migration happens."

**If asked what APIs are used, say:**
- Android system APIs for monitoring and permissions
- Room DAO APIs for persistence
- Kotlin Coroutines and Flow for asynchronous data handling
- Compose and Navigation for the UI layer
- Firebase Authentication (planned) — email/password plus optional Google Sign-In (OAuth 2.0/OpenID Connect)

---

## 6) Short technical summary
**Say:**  
"The current build proves the main product loop on-device, adds an Admin governance layer, and prepares the app for the Module 2 AI pipeline — real trained Random Forest and logistic regression models, not placeholders — plus the remaining modules that make up the full MVP."
