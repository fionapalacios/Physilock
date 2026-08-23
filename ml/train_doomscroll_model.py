"""
Trains the Module 2 Logistic Regression I (doomscroll) classifier and
transpiles it to Java via m2cgen -- same pattern as train_risk_model.py.

Deliberately trained on RAW (unscaled) features, not StandardScaler'd --
unlike the reference Colab notebook's LR models. With only 3 features of
modest scale differences, an unscaled sklearn LogisticRegression (lbfgs)
still separates the classes fine; skipping the scaler avoids having to
replicate scaler.mean_/scale_ arithmetic correctly on the Kotlin side,
which is a real bug surface (see ml/README.md) for a bootstrap model this
small. Revisit if this gets replaced with a model trained on real data.

Feature order must exactly match generate_doomscroll_dataset.py's
FEATURE_COLUMNS and DoomscrollDetector.kt's input order:
    [scrollSpeedPerMin, maxPauseGapSec, hourOfDay]
"""
import os

import m2cgen as m2c
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, roc_auc_score
from sklearn.model_selection import train_test_split

FEATURE_COLUMNS = ["scrollSpeedPerMin", "maxPauseGapSec", "hourOfDay"]

DATA_PATH = "ml/data/synthetic_doomscroll.csv"
JAVA_OUT_PATH = "app/src/main/java/com/example/physi_lock/ml/DoomscrollModel.java"
JAVA_PACKAGE = "com.example.physi_lock.ml"
JAVA_CLASS_NAME = "DoomscrollModel"


def main():
    df = pd.read_csv(DATA_PATH)
    X = df[FEATURE_COLUMNS]
    y = df["doomscrollLabel"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=7, stratify=y
    )

    model = LogisticRegression(max_iter=2000, class_weight="balanced", random_state=7)
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    y_proba = model.predict_proba(X_test)[:, 1]
    print(f"Test accuracy: {accuracy_score(y_test, y_pred):.4f}")
    print(f"AUC-ROC: {roc_auc_score(y_test, y_proba):.4f}")
    print(classification_report(y_test, y_pred))
    print("Classes (output order of predict_proba):", list(model.classes_))
    print("Coefficients:", dict(zip(FEATURE_COLUMNS, model.coef_[0])))
    print("Intercept:", model.intercept_[0])

    java_code = m2c.export_to_java(
        model, class_name=JAVA_CLASS_NAME, package_name=JAVA_PACKAGE
    )
    os.makedirs(os.path.dirname(JAVA_OUT_PATH), exist_ok=True)
    with open(JAVA_OUT_PATH, "w") as f:
        f.write(java_code)
    print(f"Wrote transpiled model to {JAVA_OUT_PATH}")


if __name__ == "__main__":
    main()
