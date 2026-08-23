# Physi-Lock: Project Documentation

## Project Overview
Physi-Lock is an AI-Based Mobile Locking System designed to systematically regulate and mitigate excessive smartphone use through on-device hybrid AI analytics combined with friction-based physical unlock challenges.

## Architecture Summary

### Hybrid Model
Physi-Lock splits work between the device and the cloud along a privacy/latency line:
- **On-Device:** usage tracking, AI/ML inference (risk scoring, doomscroll & hourly-excessive detection), and motion-lock logic — all privacy- and latency-sensitive, and all functional with no network connection.
- **Cloud (Firebase — Auth + Firestore):** accounts & authentication, Admin-facing data, and aggregate/anonymized analytics + research data export.
- **User Control:** Anonymized research data export is opt-in.

### Actors
- **User** — drives the entire on-device product experience: tracking, AI analytics, motion-lock challenges, personalization.
- **Admin** — a thin governance layer, not a monitoring/processing engine: manages user accounts, curates the app/category master list used for tracking & risk scoring, configures global default settings for new accounts, and owns aggregate/anonymized analytics + research data export.

### Core Components

#### 1. **Database Layer (Room + SQLite — on-device)**
- **AppUsageLog:** Session-level app usage tracking (screen time, scroll events)
- **UserConfiguration:** User preferences and thresholds
- **MotionInterventionLog:** Lock event audit trail with AI risk scores
- **AppLockRule:** Application restriction policies
- **UsageSession:** Legacy usage session tracking
- **AppCategory:** Admin-curated master list of app categories (Social Media/Entertainment/Games/Productivity/Other) used for tracking & risk scoring
- **DefaultSettings:** Admin-configured global defaults applied to new accounts (users can still override in their own Settings)
- **CachedAccount:** Local offline-login cache (salted password hash + profile snapshot), written after every successful online login/registration — see Authentication & Accounts below

#### 2. **Sensor Integration (SensorManager)**
- **Accelerometer (TYPE_ACCELEROMETER):** Raw motion vectors (x, y, z)
- **Motion Threshold Logic:** Distinguishes intentional movement from stationary noise
- **Dynamic Calibration:** Shake difficulty adapts to risk score

#### 3. **System-Level Monitoring (AccessibilityService)**
- **AppMonitorService:** Detects foreground app changes via TYPE_WINDOW_STATE_CHANGED
- **Real-Time Logging:** Session data persisted asynchronously to Room
- **Grace Period:** 8-second unlock cooldown prevents aggressive re-locking

#### 4. **AI/ML Pipeline (On-Device Inference)**
- **Random Forest Classifier:** Risk scoring (Low/Moderate/High)
- **Logistic Regression I:** Doomscrolling detection — 3 raw inputs (scroll speed, pause patterns, time of day). The behavioral risk score is **not** a 4th input blended into this classifier; instead, risk score selects which sensitivity threshold set ("recipe") the 3 inputs are evaluated against — it picks the recipe, it isn't an ingredient.
- **Logistic Regression II:** Hourly excessive usage prediction
- **m2cgen Transpilation:** Pure Kotlin code, zero external ML dependencies

#### 5. **UI Framework (Jetpack Compose)**
- **LockScreen:** Real-time shake progress visualization
- **Dashboard:** Daily stats, risk trends, intervention history
- **Settings:** User configuration and personalization
- **Notifications:** Non-intrusive alerts and recommendations
- **AuthScreen:** Login/registration
- **AdminHomeScreen:** Admin governance dashboard (Accounts, Categories, Defaults, Analytics tabs)

#### 6. **Authentication & Accounts**
- **Primary path (live): Firebase Authentication + Firestore**, behind the `AccountRepository` abstraction so the UI layer doesn't touch Firebase directly.
  - **Email/password:** Firebase verifies the credential directly and issues its own signed ID token (JWT) — no OAuth 2.0 exchange involved. Profile data (username, fullName, email, occupation, role, createdAt, isActive) lives in Firestore at `users/{uid}`.
  - **Google Sign-In:** built on **OAuth 2.0 / OpenID Connect** — the device obtains a Google-issued token via the Google Identity/Credential Manager SDK, and Firebase verifies that token before minting a Firebase session on top of it. This is the one login path in the system that's actually OAuth 2.0-based; the primary username/password path is not.
- **Offline fallback: `HybridAccountRepository`** wraps the Firebase repository and adds a Room-backed cache (`CachedAccount`) for when Firebase Auth/Firestore is unreachable:
  - Every successful online login or registration writes a salted-hash snapshot of the account (via `PasswordHasher`, PBKDF2WithHmacSHA256) to the local cache.
  - If a later login call throws (no network, Firestore/Auth unreachable), it falls back to verifying the identifier/password against that local snapshot instead of failing outright.
  - **Scope of the fallback is login only** — registration and profile edits still require Firebase, since there's no server to reserve a username/email or reconcile a conflicting edit against while offline. Google Sign-In is also online-only (Credential Manager needs the network).
  - The password hash used for the offline cache is a separate, device-local credential — it is never sent to Firebase and isn't the same secret Firebase Auth verifies.

---

## Development Lifecycle

### Agile Framework (Meet → Plan → Design → Develop → Test → Evaluate)

Development is organized into self-contained **modules** — one per feature/sub-feature — rather than fixed-duration sprints. Each module moves through the full Meet/Plan/Design/Develop/Test/Evaluate cycle independently; "Status" below reflects where a module currently sits in that cycle, not a calendar week.

| Module | Feature Group | Key Deliverables | Status |
|--------|---------------|-------------------|--------|
| 0. Foundation | — (cross-cutting) | Database schema, DAOs, permissions, AccessibilityService, navigation shell, onboarding | Established |
| 1. Core Monitoring & Usage Awareness | Core Monitoring/Usage Awareness | Foreground tracking, usage aggregation, scroll detection, dashboard/reports UI | Most advanced |
| 2. AI-Based Behavior Analysis | AI-Based Behavior Analysis | Random Forest risk scoring, m2cgen transpilation, doomscroll & hourly-excessive classifiers | Partial — Random Forest live (2026-08-23), classifiers not started |
| 3. Motion-Responsive Locking | Motion-Responsive Locking | Accelerometer integration, motion thresholds, lock overlay, shake calibration | Most advanced |
| 4. Smart Intervention System | Smart Intervention System | Context-aware nudges, adaptive thresholds, break reminders | Partial |
| 5. Mental Health & Awareness | Mental Health & Awareness | Reflection prompts, wellbeing check-ins | Not started |
| 6. Personalization & User Control | Personalization & User Control | Usage profiles, custom lock rules, usage limits | Partial |
| 7. Context-Aware AI | Context-Aware AI | Location-based rules, contextual doomscroll detection | Not started |
| 8. Integration & QA | — (cross-cutting) | E2E testing, performance profiling, ISO/IEC 25010 validation | Ongoing |

---

## Technology Stack

### Android Framework
- **Language:** Kotlin 1.9+
- **Minimum SDK:** 26 (Android 8.0)
- **Target SDK:** 36 (Android 15)
- **Compose Version:** Latest via BOM

### Core Dependencies
- **Room:** `androidx.room:room-ktx:2.8.0`
- **Coroutines:** `kotlinx-coroutines-android:1.7.3`
- **Compose:** Via BOM
- **Navigation:** `androidx.navigation:navigation-compose:2.7.7`
- **Analytics:** `MPAndroidChart:v3.1.0`
- **Lifecycle:** `androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0`

### Build Configuration
- **Build Tool:** Gradle with Kotlin DSL
- **Annotation Processor:** KSP (Kotlin Symbol Processing) for Room
- **Kotlin Compose Plugin:** Enabled for Jetpack Compose

---

## Database Schema

### Entities

#### AppUsageLog
```
id (PK, auto-increment)
packageName (indexed)
appName
sessionStartTime
sessionEndTime
foregroundDurationMs
scrollEventCount
dateKey (indexed with packageName)
```

#### UserConfiguration
```
configId (PK, default=1)
userMode: STUDENT_MODE | WORK_MODE
dailyScreenTimeThresholdMs
hourlyExcessiveUsageThresholdMs
doomscrollingDetectionEnabled
motionLockSensitivity: LOW | MEDIUM | HIGH
lastUpdatedTime
```

#### MotionInterventionLog
```
interventionId (PK, auto-increment)
packageName (indexed)
triggerType: MOTION_LOCK | DOOMSCROLL_ALERT | HOURLY_PREDICTION
interventionTimestamp (indexed)
userResponse: UNLOCKED | DISMISSED | TIMEOUT
accelerometerVariance
riskScore (0.0–1.0)
```

#### AppLockRule
```
packageName (PK)
isLocked
lockType: ADAPTIVE | CUSTOM | GOALS
dailyLimitMs (nullable)
```

#### UsageSession
```
id (PK, auto-increment)
packageName
appName
startTime
endTime
durationMs
dateKey
```

---

## Android Manifest Permissions

### Required Permissions
- **PACKAGE_USAGE_STATS:** Access usage statistics via UsageStatsManager
- **SYSTEM_ALERT_WINDOW:** Draw overlay lock UI
- **READ_LOGS:** Extended usage tracking (API ≥ 29)
- **BIND_ACCESSIBILITY_SERVICE:** Register AccessibilityService

### System-Level Services
- **AppMonitorService (AccessibilityService):** Persistent foreground app monitoring

---

## Key Workflows

### 1. **Foreground App Monitoring**
```
AccessibilityService detects TYPE_WINDOW_STATE_CHANGED
  ↓
Extract packageName, timestamp
  ↓
Log previous session to AppUsageLog (if changed)
  ↓
Initialize new session for current app
  ↓
Check against AppLockRule (if locked, launch LockActivity)
```

### 2. **Risk Scoring Pipeline (Module 2: AI-Based Behavior Analysis)**
```
Every 5 minutes:
  ↓
Collect recent AppUsageLog entries
  ↓
Normalize features (app category, hour, duration, unlock freq)
  ↓
Call RiskScoringEngine.scoreRisk(features)
  ↓
Store riskScore in MotionInterventionLog
  ↓
Stream to UI for dashboard display
```

### 3. **Motion-to-Unlock Challenge (Module 3: Motion-Responsive Locking)**
```
User tries to access locked app
  ↓
AccessibilityService detects app launch
  ↓
LockActivity deployed with LockScreen overlay
  ↓
SensorManager registers accelerometer listener
  ↓
User shakes device aggressively
  ↓
Acceleration variance exceeds threshold (configurable per risk)
  ↓
ShakeDetector counts shakes (required count: 10 default, scales with risk)
  ↓
On completion: triggerGlobalUnlock() called
  ↓
8-second grace period before re-locking enabled
```

---

## Performance & Battery Optimization

### Target Metrics (Module 8: Integration & QA)
- **CPU Usage:** <5% during sensor monitoring, <1% during AI inference
- **Battery Drain:** <2% per 24 hours from Physi-Lock alone
- **Memory Footprint:** <100 MB RAM during normal operation
- **Database Query Time:** <200ms for daily aggregations

### Optimization Strategies
- Aggressive sensor unregistration on screen-off
- Batch database writes with transactions
- Defer non-critical analytics to off-peak hours
- WorkManager for background tasks instead of frequent alarms

---

## Privacy & Data Security

### Hybrid Storage, On-Device by Default
- Sensitive usage/behavioral tracking data (AppUsageLog, MotionInterventionLog, UserConfiguration) stays on-device; no external API calls for the core monitoring/AI/motion-lock loop
- Accounts, Admin-facing data, and aggregate/anonymized analytics are the only data that reach the cloud (Firebase Auth + Firestore, planned)
- Optional anonymized research export (user control) via the Admin dashboard's Export Research Data function

### Data Retention
- 90-day auto-cleanup for historical usage logs (on-device)
- User can manually wipe all local data
- No cloud backup of raw usage/behavioral data (unless opted-in for research export)

---

## Testing Strategy

### Unit Tests
- Entity validation
- DAO operations
- AI model inference
- Timestamp calculations

### Integration Tests
- Database persistence
- AccessibilityService event handling
- Sensor listener registration/cleanup
- UI state transitions

### End-to-End Tests
- Launch restricted app → motion-lock triggers → shake to unlock
- Risk scoring affects difficulty correctly
- Settings persist across app restarts
- Device rotation preserves state

### Performance Testing
- CPU/memory profiling during 1-hour continuous monitoring
- Battery drain measurement over 24 hours
- Database query performance under load

---

## Deployment & Research Evaluation

### Pre-Deployment Checklist
- ✅ All module deliverables completed
- ✅ ISO/IEC 25010 quality validation passed
- ✅ E2E testing on SDK 26–36 successful
- ✅ Performance targets met
- ✅ Privacy safeguards verified
- ✅ Documentation complete

### Research Evaluation
- Anonymized usage data export available
- Researcher guides prepared
- Beta testing infrastructure ready
- Onboarding flow polished

---

## Future Enhancements

1. **Alternative Unlock Methods:** CAPTCHA, math problems, PIN challenges
2. **Location-Based Locking:** Custom rules by WiFi network or geofence
3. **Social Features:** Friend accountability, shared goals (optional cloud)
4. **Wearable Integration:** Apple Watch, Wear OS motion detection
   5. **Advanced ML:** Federated learning for cohort-level insights without cloud

---

## Contact & Support

For technical issues or questions about Physi-Lock development:
- Repository: joys-R-Us/physi-lock-capstone-project
- Documentation: See the Development Lifecycle module table above
- Database Inspector: Android Studio → Database Inspector tool
