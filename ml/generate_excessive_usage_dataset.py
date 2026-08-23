"""
Generates a synthetic training set for Module 2's Logistic Regression II
(hourly excessive-usage prediction). Same bootstrap situation as the other
two Module 2 models -- no ground-truth labels exist, so this assigns them
via a weighted heuristic with noise.

The manuscript's Data Dictionary (EXCESSIVE_USAGE_PREDICTION table) only
specifies the *output* log schema (hour, predicted_usage_minutes,
excessive_probability, is_excessive) -- it doesn't enumerate model inputs.
This feature set was proposed and confirmed 2026-08-23 (see project memory
"project-next-session-tasks"), not transcribed from the manuscript like the
Random Forest's feature list was.

Feature order must exactly match ExcessiveUsageDetector.kt's input order:
    [avgUsageThisHourMin, cumulativeUsageTodayMin, usagePrevHourMin, isWeekend]
"""
import os

import numpy as np
import pandas as pd

FEATURE_COLUMNS = [
    "avgUsageThisHourMin",
    "cumulativeUsageTodayMin",
    "usagePrevHourMin",
    "isWeekend",
]

N_SAMPLES = 6000
SEED = 11

REFERENCE_HIGH = {
    "avgUsageThisHourMin": 60.0,  # a full hour is the ceiling for a single-hour bucket
    "cumulativeUsageTodayMin": 900.0,  # 15h
    "usagePrevHourMin": 60.0,
}

WEIGHTS = {
    "avgUsageThisHour": 0.40,
    "usagePrevHour": 0.30,
    "cumulativeToday": 0.20,
    "isWeekend": 0.10,
}


def generate_features(rng: np.random.Generator, n: int) -> pd.DataFrame:
    avg_usage_this_hour = rng.uniform(0, 60, size=n)
    cumulative_today = rng.uniform(0, 900, size=n)
    usage_prev_hour = rng.uniform(0, 60, size=n)
    is_weekend = rng.integers(0, 2, size=n).astype(float)

    return pd.DataFrame({
        "avgUsageThisHourMin": avg_usage_this_hour,
        "cumulativeUsageTodayMin": cumulative_today,
        "usagePrevHourMin": usage_prev_hour,
        "isWeekend": is_weekend,
    })


def heuristic_excessive_score(df: pd.DataFrame) -> np.ndarray:
    avg_hour_component = (df["avgUsageThisHourMin"] / REFERENCE_HIGH["avgUsageThisHourMin"]).clip(upper=1.0)
    prev_hour_component = (df["usagePrevHourMin"] / REFERENCE_HIGH["usagePrevHourMin"]).clip(upper=1.0)
    cumulative_component = (df["cumulativeUsageTodayMin"] / REFERENCE_HIGH["cumulativeUsageTodayMin"]).clip(upper=1.0)
    weekend_component = df["isWeekend"]

    return (
        WEIGHTS["avgUsageThisHour"] * avg_hour_component
        + WEIGHTS["usagePrevHour"] * prev_hour_component
        + WEIGHTS["cumulativeToday"] * cumulative_component
        + WEIGHTS["isWeekend"] * weekend_component
    )


def main():
    rng = np.random.default_rng(SEED)
    features = generate_features(rng, N_SAMPLES)
    score = heuristic_excessive_score(features)
    noise = rng.normal(loc=0.0, scale=0.07, size=len(features))
    noisy_score = np.clip(score + noise, 0.0, 1.0)

    median = np.median(noisy_score)
    label = (noisy_score >= median).astype(int)

    features["excessiveScore"] = noisy_score
    features["isExcessiveLabel"] = label

    out_path = "ml/data/synthetic_excessive_usage.csv"
    os.makedirs("ml/data", exist_ok=True)
    features.to_csv(out_path, index=False)
    print(f"Wrote {len(features)} rows to {out_path}")
    print(features["isExcessiveLabel"].value_counts())


if __name__ == "__main__":
    main()
