# Module 2: AI-Based Behavior Analysis

All three Module 2 models live here now: the Random Forest risk classifier,
Logistic Regression I (doomscroll detection), and Logistic Regression II
(hourly excessive-usage prediction). All three follow the same shape: a
Python script generates a synthetic bootstrap dataset, another trains a
model and transpiles it to Java via m2cgen, and a Kotlin wrapper calls the
generated Java class with real on-device feature values at inference time.

## Pipeline — Random Forest (risk scoring)

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

## Pipeline — Logistic Regression I (doomscroll detection)

```
generate_doomscroll_dataset.py  -->  ml/data/synthetic_doomscroll.csv
                                    |
                                    v
train_doomscroll_model.py  -->  app/.../ml/DoomscrollModel.java
                                    |
                                    v
        DoomscrollDetector.kt calls DoomscrollModel.score(input) at runtime,
        fed by live in-memory session state in AppMonitorService
        (NOT Room-queried — see "Where the 3 inputs come from" below)
```

**Design constraint that shapes this model** (project memory "module2-
doomscroll-design"): the behavioral risk score (from the Random Forest above)
is explicitly **not** a 4th input feature here. It's applied at inference
time in `DoomscrollDetector.kt` as a threshold selector — a high-risk user
gets flagged by a weaker doomscroll signal than a low-risk user would need.
`generate_doomscroll_dataset.py`/`train_doomscroll_model.py` only ever see
the 3 raw inputs (scroll speed, pause pattern, time of day); don't add risk
score as a training column if this gets retrained.

**Where the 3 inputs come from**: `AppMonitorService` already tracks live,
in-progress session state for the currently-foregrounded app
(`currentSessionScrollCount`, `currentSessionMaxScrollGapMs`,
`currentSessionStartTime`) to populate `AppUsageLog` when a session ends.
`checkDoomscrolling()` reads that same live state mid-session (scroll speed =
count / elapsed minutes, pause = max gap in seconds, time of day = current
hour) rather than querying Room, since doomscrolling needs to be caught while
it's happening, not after the session's already over.

**m2cgen's Java export differs by model type**: `RiskModel.score()` (Random
Forest) returns a `double[]` of class probabilities. `DoomscrollModel.score()`
(Logistic Regression) returns a single raw **logit** (pre-sigmoid linear
score) — `DoomscrollDetector.kt` applies the sigmoid itself. Check the actual
generated file's signature after any retrain rather than assuming either shape.

**Trained on raw, unscaled features** — unlike the reference Colab notebook's
Logistic Regression models (which use `StandardScaler` + a `scaler_params.json`
replicated on-device). With only 3 features of modest scale, unscaled
`LogisticRegression` (lbfgs) still separates the classes fine, and skipping
the scaler avoids a real bug surface: replicating `mean_`/`scale_` arithmetic
correctly by hand on the Kotlin side. Revisit if this gets retrained on real
(not synthetic) data where scaling might matter more.

**Known simplification — hour-of-day wraparound**: `hourOfDay` is a single
raw 0–23 value, not cyclically encoded (no sin/cos). A plain linear model
can't represent "11pm and 1am are close together" — it learns a rough
monotonic trend instead (this trained model's `hourOfDay` coefficient is
negative, i.e. smaller raw hour → higher doomscroll probability, which
correctly favors early-morning hours but under-flags the late-evening hours
that should also score high). Good enough for a bootstrap model; revisit with
cyclical encoding if this becomes the deployed model trained on real data.

## Pipeline — Logistic Regression II (hourly excessive-usage prediction)

```
generate_excessive_usage_dataset.py  -->  ml/data/synthetic_excessive_usage.csv
                                    |
                                    v
train_excessive_usage_model.py  -->  app/.../ml/ExcessiveUsageModel.java
                                    |
                                    v
        ExcessiveUsageDetector.kt calls ExcessiveUsageModel.score(input),
        fed by ExcessiveUsageFeatureExtractor.kt reading real Room DB data
```

The manuscript's Data Dictionary has an `EXCESSIVE_USAGE_PREDICTION` table, but
it only specifies the model's **output** log schema (`hour`,
`predicted_usage_minutes`, `excessive_probability`, `is_excessive`) — same
relationship as `MotionInterventionLog` (output) vs. `RiskFeatures` (input)
for the Random Forest. It does not enumerate input features anywhere in that
appendix. The 4-feature input set below was proposed and confirmed
2026-08-23 (see project memory "project-next-session-tasks"), not
transcribed from the manuscript the way the Random Forest's feature list was:

1. `avgUsageThisHourMin` — average usage during this hour-of-day, over the past 30 days
2. `cumulativeUsageTodayMin` — total usage so far today
3. `usagePrevHourMin` — usage in the immediately preceding hour
4. `isWeekend`

Label (`isExcessiveLabel`): whether the hour's actual usage crosses the
already-existing `UserConfiguration.hourlyExcessiveUsageThresholdMs`
(60 min default) — real, no new threshold invented.

Output is logged to a new `ExcessiveUsagePredictionLog` Room entity (mirrors
the manuscript's table almost field-for-field), one row per hour, via
`AppMonitorService.checkExcessiveUsagePrediction()` on a 15-minute loop. The
resulting notification is gated behind the existing **Overuse Alerts**
Settings toggle (`UserConfiguration.overuseAlertsEnabled`) rather than a new
dedicated toggle — it's still fundamentally an overuse alert, just
predictive instead of reactive-on-today's-total.

Unlike `DoomscrollDetector`, there's no risk-level threshold "recipe" here —
that pattern was specifically confirmed for the doomscroll classifier only;
this one uses a plain fixed 0.5 probability threshold.

## Setup

```
py -3.14 -m venv ml/.venv
ml/.venv/Scripts/python.exe -m pip install -r ml/requirements.txt
ml/.venv/Scripts/python.exe ml/generate_dataset.py
ml/.venv/Scripts/python.exe ml/train_risk_model.py
ml/.venv/Scripts/python.exe ml/generate_doomscroll_dataset.py
ml/.venv/Scripts/python.exe ml/train_doomscroll_model.py
ml/.venv/Scripts/python.exe ml/generate_excessive_usage_dataset.py
ml/.venv/Scripts/python.exe ml/train_excessive_usage_model.py
```

The training scripts overwrite `app/src/main/java/com/example/physi_lock/ml/RiskModel.java`,
`DoomscrollModel.java`, and `ExcessiveUsageModel.java` respectively — rerun
`gradlew assembleDebug` afterward to confirm all three still compile *and*
dex (see the method-size note below — only relevant to the Random Forest).

## Open question: manuscript discrepancy on Logistic Regression I's inputs

The manuscript's `DOOMSCROLLING_DETECTION` Data Dictionary table implies
different inputs than what got built — `scrolling_duration_minutes` and
`scroll_pause_frequency` (a count), not scroll speed (events/min) + max
single pause gap, and no `hour`/time-of-day column at all. This may come
from a different manuscript section than whatever narrative chapter
originally confirmed the "3 raw inputs" design (project memory
"module2-doomscroll-design") — not necessarily a contradiction, but
unresolved. Flagged 2026-08-23, revisit later per user's request.

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
