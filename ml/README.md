# Module 2: AI-Based Behavior Analysis — Random Forest risk scoring

## Pipeline

```
generate_dataset.py  -->  ml/data/synthetic_usage_risk.csv
                                    |
                                    v
train_risk_model.py  -->  app/src/main/java/com/example/physi_lock/ml/RiskModel.java
                                    |
                                    v
        RiskScoringEngine.kt calls RiskModel.score(input) at runtime,
        fed by RiskFeatureExtractor.kt reading real Room DB data
```

## Setup

```
py -3.14 -m venv ml/.venv
ml/.venv/Scripts/python.exe -m pip install -r ml/requirements.txt
ml/.venv/Scripts/python.exe ml/generate_dataset.py
ml/.venv/Scripts/python.exe ml/train_risk_model.py
```

The last step overwrites `app/src/main/java/com/example/physi_lock/ml/RiskModel.java` —
rerun `gradlew compileDebugKotlin` afterward to confirm it still compiles.

## Why the training data is synthetic

Nothing in this app has ever recorded a ground-truth risk label for a usage session —
there's no survey, no user self-report, nothing. A hybrid dataset in the literal sense
(real usage rows + real risk labels) isn't possible yet. `generate_dataset.py` instead
generates synthetic feature vectors and assigns each a Low/Moderate/High label via a
weighted heuristic (`heuristic_risk_score`), with noise so the classes aren't perfectly
separable. The trained model is meant to score **real** on-device feature vectors at
**inference** time (`RiskFeatureExtractor.kt`) — synthetic for training, real for scoring.

## Why the output is Java, not Kotlin

m2cgen has no Kotlin target. It does have a Java target, and Java sources compile and
interop with Kotlin with zero friction in an Android Gradle module — `RiskScoringEngine.kt`
just calls `RiskModel.score(...)` like any other JVM class. This still satisfies "zero
external ML dependency" (no scikit-learn, no TensorFlow Lite bundled into the app — the
generated file is plain arithmetic), it's just not literally Kotlin syntax.

## Why the forest is small (15 trees, max_depth=5)

m2cgen inlines an entire Random Forest as one giant nested if/else tree **in a single
Java method** — it does not split trees into helper methods. The JVM caps a single
method at 64KB of bytecode. A first attempt at 150 trees/depth 8 generated a ~112,000
line file that would have failed to compile (`code too large`). 15 trees/depth 5
generates ~1,900 lines and compiles and dexes cleanly (`gradlew assembleDebug` verified).

If you need to scale the model back up, increase `n_estimators`/`max_depth` gradually
and rerun `gradlew compileDebugJavaWithJavac` after each retrain to catch the limit
before it becomes a runtime dex failure. Splitting the ensemble into several classes/
methods manually is possible but wasn't necessary at this size — 66.8% held-out
accuracy on the balanced synthetic set was judged good enough for a bootstrap model
that gets replaced once real labeled data exists.

## Feature order (must match in all three places)

`generate_dataset.py`'s `FEATURE_COLUMNS`, `RiskScoringEngine.kt`'s `doubleArrayOf(...)`,
and `RiskFeatureExtractor.kt`'s `RiskFeatures` fields must all agree on this order:

1. `dailyScreenTimeMin` — real, from `AppUsageLogDao.getTotalDurationByDateOnce`
2. `avgSessionLengthMin` — real, derived (`dailyScreenTimeMin / sessionCount`)
3. `appLaunchFrequency` — real, `AppUsageLogDao.getSessionCountByDate` (one row per app-open session)
4. `socialMediaFraction` — real, `AppUsageLogDao.getDurationByCategoryAndDate(SOCIAL_MEDIA, ...)` / total
5. `bypassAttemptCount` — real, `MotionInterventionLogDao.countBypassAttemptsAfter` (see below)
6. `doomscrollEpisodeCount` — **NOT real yet, hardcoded to 0** (see below)

## Known gap: doomscrollEpisodeCount is still a placeholder

The manuscript's 6th Random Forest feature is a *count of doomscroll episodes detected
by Logistic Regression I* — but LR-I (the doomscroll classifier) hasn't been built yet.
`RiskFeatureExtractor.extractTodayFeatures()` hardcodes this to `0.0` until it exists.
The raw material LR-I will need is already being collected for real, though:
`AppUsageLog.scrollEventCount` (increments on every `TYPE_VIEW_SCROLLED` event) and
`AppUsageLog.maxScrollGapMs` (longest gap between scroll events in a session, a proxy
for "pause patterns") — see `AppMonitorService.logScrollEvent`. Time-of-day (LR-I's
third input) is trivially available from `AppUsageLog.sessionStartTime`. Building LR-I
itself — training it, deciding what counts as an "episode" from a stream of sessions,
and wiring its output into `doomscrollEpisodeCount` — is separate follow-up work.

## What "bypass attempt" means

A `MotionInterventionLog` row with `userResponse = "DISMISSED"` — the user was shown a
lock challenge (`LockActivity`) and left without completing it. `LockActivity.onStop()`
logs this once per challenge shown (back button, home button, recents swipe-away, and
screen-off all route through `onStop()`, guarded so a screen-off/on cycle mid-challenge
doesn't double-log). This does **not** block back-press or otherwise change the lock
screen's behavior — it only adds logging on top of what was already possible.

## Retraining safely

Always run `generate_dataset.py` before `train_risk_model.py` if you change anything in
the feature/label generation logic — the trained model only knows what's in the CSV.
After any retrain, re-verify:
1. `RiskModel.score`'s printed `Classes (output order of predict_proba)` still matches
   `RiskScoringEngine.kt`'s `pHigh/pLow/pModerate` index assumptions (alphabetical:
   HIGH, LOW, MODERATE — but re-check, don't assume).
2. `gradlew assembleDebug` still succeeds (method-size limit, see above).
