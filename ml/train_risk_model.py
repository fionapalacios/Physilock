"""
Trains the Module 2 Random Forest risk classifier on the synthetic dataset
(run generate_dataset.py first) and transpiles it to Java via m2cgen, so the
app can score real feature vectors on-device with zero ML runtime dependency
(no scikit-learn, no TensorFlow Lite -- just the generated arithmetic).

m2cgen doesn't have a Kotlin target, but Java sources compile and interop
seamlessly inside an Android/Kotlin Gradle module, so that's what this
generates -- see ml/README.md for the full explanation.

Feature order must exactly match generate_dataset.py's FEATURE_COLUMNS and
RiskScoringEngine.kt's input order:
    [dailyScreenTimeMin, avgSessionLengthMin, appLaunchFrequency,
     socialMediaFraction, bypassAttemptCount, doomscrollEpisodeCount]
"""
import os

import m2cgen as m2c
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split

FEATURE_COLUMNS = [
    "dailyScreenTimeMin",
    "avgSessionLengthMin",
    "appLaunchFrequency",
    "socialMediaFraction",
    "bypassAttemptCount",
    "doomscrollEpisodeCount",
]

DATA_PATH = "ml/data/synthetic_usage_risk.csv"
JAVA_OUT_PATH = "app/src/main/java/com/example/physi_lock/ml/RiskModel.java"
JAVA_PACKAGE = "com.example.physi_lock.ml"
JAVA_CLASS_NAME = "RiskModel"


def main():
    df = pd.read_csv(DATA_PATH)
    X = df[FEATURE_COLUMNS]
    y = df["riskLabel"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # Kept deliberately small: m2cgen inlines the whole forest as nested if/else
    # into ONE Java method (no per-tree helper methods), and the JVM caps a
    # single method at 64KB of bytecode. n_estimators=150/max_depth=8 generated
    # ~112,000 lines and would fail to compile. This size compiles cleanly --
    # see ml/README.md for the tradeoff and how to safely scale it back up.
    model = RandomForestClassifier(
        n_estimators=15,
        max_depth=5,
        min_samples_leaf=8,
        random_state=42,
        class_weight="balanced",
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    print(f"Test accuracy: {accuracy_score(y_test, y_pred):.4f}")
    print(classification_report(y_test, y_pred))
    print("Classes (output order of predict_proba):", list(model.classes_))

    java_code = m2c.export_to_java(
        model, class_name=JAVA_CLASS_NAME, package_name=JAVA_PACKAGE
    )
    os.makedirs(os.path.dirname(JAVA_OUT_PATH), exist_ok=True)
    with open(JAVA_OUT_PATH, "w") as f:
        f.write(java_code)
    print(f"Wrote transpiled model to {JAVA_OUT_PATH}")


if __name__ == "__main__":
    main()
