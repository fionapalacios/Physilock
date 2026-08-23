"""
Trains the Module 2 Logistic Regression II (hourly excessive-usage) classifier
and transpiles it to Java via m2cgen -- same pattern as train_doomscroll_model.py,
including training on raw/unscaled features for the same reason (see ml/README.md).

Feature order must exactly match generate_excessive_usage_dataset.py's
FEATURE_COLUMNS and ExcessiveUsageDetector.kt's input order:
    [avgUsageThisHourMin, cumulativeUsageTodayMin, usagePrevHourMin, isWeekend]
"""
import os

import m2cgen as m2c
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, roc_auc_score
from sklearn.model_selection import train_test_split

FEATURE_COLUMNS = ["avgUsageThisHourMin", "cumulativeUsageTodayMin", "usagePrevHourMin", "isWeekend"]

DATA_PATH = "ml/data/synthetic_excessive_usage.csv"
JAVA_OUT_PATH = "app/src/main/java/com/example/physi_lock/ml/ExcessiveUsageModel.java"
JAVA_PACKAGE = "com.example.physi_lock.ml"
JAVA_CLASS_NAME = "ExcessiveUsageModel"


def main():
    df = pd.read_csv(DATA_PATH)
    X = df[FEATURE_COLUMNS]
    y = df["isExcessiveLabel"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=11, stratify=y
    )

    model = LogisticRegression(max_iter=2000, class_weight="balanced", random_state=11)
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
