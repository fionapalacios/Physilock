"""
Generates a synthetic training set for Module 2's Logistic Regression I
(doomscroll classifier). Same bootstrap situation as the Random Forest
(see generate_dataset.py) -- no ground-truth doomscroll labels exist
anywhere, real or synthetic, so this assigns labels via a weighted
heuristic with noise.

IMPORTANT -- exactly 3 raw inputs, per the confirmed design (project memory
"module2-doomscroll-design"): scroll speed, pause patterns, time of day.
The behavioral risk score is explicitly NOT a 4th input here -- it's applied
at inference time as a threshold selector (see DoomscrollDetector.kt), not
trained into the model. Don't add it as a column in this dataset.

Feature order must exactly match DoomscrollDetector.kt's input order:
    [scrollSpeedPerMin, maxPauseGapSec, hourOfDay]
"""
import os

import numpy as np
import pandas as pd

FEATURE_COLUMNS = ["scrollSpeedPerMin", "maxPauseGapSec", "hourOfDay"]

N_SAMPLES = 6000
SEED = 7

REFERENCE_HIGH = {
    "scrollSpeedPerMin": 120.0,
    "maxPauseGapSec": 60.0,  # ceiling used for the *inverted* pause contribution
}

WEIGHTS = {
    "scrollSpeed": 0.45,
    "pauseInverted": 0.30,
    "nightProximity": 0.25,
}


def generate_features(rng: np.random.Generator, n: int) -> pd.DataFrame:
    scroll_speed = rng.uniform(0, 120, size=n)  # scroll events per minute
    max_pause_gap = rng.uniform(0, 180, size=n)  # seconds
    hour_of_day = rng.uniform(0, 24, size=n)

    return pd.DataFrame({
        "scrollSpeedPerMin": scroll_speed,
        "maxPauseGapSec": max_pause_gap,
        "hourOfDay": hour_of_day,
    })


def night_proximity(hour: np.ndarray) -> np.ndarray:
    # Peaks (1.0) at 2am, troughs (0.0) at 2pm -- a smooth day/night curve
    # instead of a hard late-night cutoff. Used only to build the label,
    # not fed to the model (which sees raw hourOfDay -- see module docstring).
    return 0.5 * (1 + np.cos(2 * np.pi * (hour - 2) / 24))


def heuristic_doomscroll_score(df: pd.DataFrame) -> np.ndarray:
    scroll_component = (df["scrollSpeedPerMin"] / REFERENCE_HIGH["scrollSpeedPerMin"]).clip(upper=1.0)
    # Inverted: a LOW pause gap (near-continuous scrolling, no reading stop)
    # contributes MORE to the doomscroll score.
    pause_component = 1.0 - (df["maxPauseGapSec"] / REFERENCE_HIGH["maxPauseGapSec"]).clip(upper=1.0)
    night_component = night_proximity(df["hourOfDay"].to_numpy())

    return (
        WEIGHTS["scrollSpeed"] * scroll_component
        + WEIGHTS["pauseInverted"] * pause_component
        + WEIGHTS["nightProximity"] * night_component
    )


def main():
    rng = np.random.default_rng(SEED)
    features = generate_features(rng, N_SAMPLES)
    score = heuristic_doomscroll_score(features)
    noise = rng.normal(loc=0.0, scale=0.07, size=len(features))
    noisy_score = np.clip(score + noise, 0.0, 1.0)

    # Median split -- guarantees a balanced binary label regardless of the
    # heuristic score's actual distribution shape (same reasoning as the
    # Random Forest's tertile split in generate_dataset.py).
    median = np.median(noisy_score)
    label = (noisy_score >= median).astype(int)

    features["doomscrollScore"] = noisy_score
    features["doomscrollLabel"] = label

    out_path = "ml/data/synthetic_doomscroll.csv"
    os.makedirs("ml/data", exist_ok=True)
    features.to_csv(out_path, index=False)
    print(f"Wrote {len(features)} rows to {out_path}")
    print(features["doomscrollLabel"].value_counts())


if __name__ == "__main__":
    main()
