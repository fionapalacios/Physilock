# Physi-Lock: Project Documentation

## Project Overview
Physi-Lock is an AI-Based Mobile Locking System designed to systematically regulate and mitigate excessive smartphone use through on-device hybrid AI analytics combined with friction-based physical unlock challenges.

## Architecture Summary

### On-Device Edge Computing Model
- **Privacy First:** All data persists locally; no cloud servers required
- **Offline Functional:** Complete system operates without internet connectivity
- **User Control:** Anonymized research data export optional

### Core Components

#### 1. **Database Layer (Room + SQLite)**
- **AppUsageLog:** Session-level app usage tracking (screen time, scroll events)
- **UserConfiguration:** User preferences and thresholds
- **MotionInterventionLog:** Lock event audit trail with AI risk scores
- **AppLockRule:** Application restriction policies
- **UsageSession:** Legacy usage session tracking

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
- **Logistic Regression I:** Doomscrolling detection
- **Logistic Regression II:** Hourly excessive usage prediction
- **m2cgen Transpilation:** Pure Kotlin code, zero external ML dependencies

#### 5. **UI Framework (Jetpack Compose)**
- **LockScreen:** Real-time shake progress visualization
- **Dashboard:** Daily stats, risk trends, intervention history
- **Settings:** User configuration and personalization
- **Notifications:** Non-intrusive alerts and recommendations

---

## Development Lifecycle

### 8-Sprint Scrum Framework

| Sprint | Focus | Duration | Key Deliverables |
|--------|-------|----------|-----------------|
| 1 | Setup & DB Config | Week 1 | Database schema, DAOs, Permissions, AccessibilityService |
| 2 | UI & Navigation | Week 2 | Dashboard, navigation, onboarding, settings |
| 3 | Data Collection | Week 3 | Foreground tracking, usage aggregation, scroll detection |
| 4 | Risk Scoring AI | Week 4 | Random Forest, m2cgen transpilation, inference service |
| 5 | Behavioral Classifiers | Week 5 | Doomscroll & prediction models, notifications |
| 6 | Motion-Lock Feature | Week 6 | Accelerometer, motion thresholds, lock overlay, shake calibration |
| 7 | Smart Interventions | Week 7 | Context awareness, adaptive thresholds, personalization |
| 8 | Integration & QA | Week 8 | E2E testing, performance profiling, ISO/IEC 25010 validation |

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

### 2. **Risk Scoring Pipeline (Sprint 4+)**
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

### 3. **Motion-to-Unlock Challenge (Sprint 6)**
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

### Target Metrics (Sprint 8)
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

### On-Device Storage Only
- No mandatory cloud sync
- All tracking data local to device
- No external API calls for core functionality
- Optional anonymized research export (user control)

### Data Retention
- 90-day auto-cleanup for historical logs
- User can manually wipe all data
- No backup to cloud (unless opted-in for research)

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
- ✅ All 61 sprint tasks completed
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
- Documentation: See plan.md and sprints in session state
- Database Inspector: Android Studio → Database Inspector tool
