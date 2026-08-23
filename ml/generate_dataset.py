"""
Generates a synthetic training set for Module 2's Random Forest risk classifier.

Why synthetic: nothing in the app has ever recorded a ground-truth risk label (real
or otherwise) for a usage session, so there's no labeled data to train on yet. This
generates realistic-looking feature vectors and assigns each one a Low/Moderate/High
label via a weighted heuristic (see `heuristic_risk_score` below), with noise so the
classes aren't perfectly separable. The trained model is meant to be applied to real
on-device feature vectors at inference time (see RiskFeatureExtractor.kt on the
Kotlin side) -- this script only produces the training data.

Feature order matters -- it must exactly match RiskScoringEngine.kt's input order:
    [dailyScreenTimeMin, avgSessionLengthMin, appLaunchFrequency,
     socialMediaFraction, bypassAttemptCount, doomscrollEpisodeCount]
"""
import numpy as np
import pandas as pd

FEATURE_COLUMNS = [
    "dailyScreenTimeMin",
    "avgSessionLengthMin",
    "appLaunchFrequency",
    "socialMediaFraction",
    "bypassAttemptCount",
    "doomscrollEpisodeCount",
]

N_SAMPLES = 6000
SEED = 42

# Reference "high" values used to normalize each raw feature into [0, 1] before
# weighting -- these are rough manuscript-scale ceilings, not measured constants.
REFERENCE_HIGH = {
    "dailyScreenTimeMin": 960.0,   # 16h
    "avgSessionLengthMin": 60.0,
    "appLaunchFrequency": 150.0,
    "bypassAttemptCount": 20.0,
    "doomscrollEpisodeCount": 15.0,
}

WEIGHTS = {
    "dailyScreenTimeMin": 0.25,
    "avgSessionLengthMin": 0.10,
    "appLaunchFrequency": 0.15,
    "socialMediaFraction": 0.20,
    "bypassAttemptCount": 0.15,
    "doomscrollEpisodeCount": 0.15,
}


def generate_features(rng: np.random.Generator, n: int) -> pd.DataFrame:
    # Uniform-ish over each feature's plausible range, not centered on typical/light
    # usage -- the point of this synthetic set is even coverage across the Low/
    # Moderate/High spectrum so the classifier sees real examples of all three,
    # not a realistic population skew (that's what real on-device data will look
    # like once enough of it exists; this is a bootstrap, not a population model).
    daily_screen_time = rng.uniform(10, 900, size=n)
    avg_session_length = rng.uniform(1, 80, size=n)
    app_launch_frequency = rng.uniform(1, 180, size=n)
    social_media_fraction = rng.uniform(0.0, 1.0, size=n)
    bypass_attempts = rng.uniform(0, 25, size=n)
    doomscroll_episodes = rng.uniform(0, 20, size=n)

    return pd.DataFrame({
        "dailyScreenTimeMin": daily_screen_time,
        "avgSessionLengthMin": avg_session_length,
        "appLaunchFrequency": app_launch_frequency,
        "socialMediaFraction": social_media_fraction,
        "bypassAttemptCount": bypass_attempts,
        "doomscrollEpisodeCount": doomscroll_episodes,
    })


def heuristic_risk_score(df: pd.DataFrame, rng: np.random.Generator) -> np.ndarray:
    normalized = pd.DataFrame({
        "dailyScreenTimeMin": df["dailyScreenTimeMin"] / REFERENCE_HIGH["dailyScreenTimeMin"],
        "avgSessionLengthMin": df["avgSessionLengthMin"] / REFERENCE_HIGH["avgSessionLengthMin"],
        "appLaunchFrequency": df["appLaunchFrequency"] / REFERENCE_HIGH["appLaunchFrequency"],
        "socialMediaFraction": df["socialMediaFraction"],
        "bypassAttemptCount": df["bypassAttemptCount"] / REFERENCE_HIGH["bypassAttemptCount"],
        "doomscrollEpisodeCount": df["doomscrollEpisodeCount"] / REFERENCE_HIGH["doomscrollEpisodeCount"],
    }).clip(upper=1.0)

    score = sum(normalized[col] * WEIGHTS[col] for col in FEATURE_COLUMNS)
    noise = rng.normal(loc=0.0, scale=0.06, size=len(df))
    return np.clip(score + noise, 0.0, 1.0)


def label_from_score(score: np.ndarray) -> np.ndarray:
    # Tertile cutoffs rather than fixed 0.35/0.65 thresholds: summing several
    # independent-ish uniform features naturally concentrates the weighted score
    # near its mean (a sum-of-uniforms/CLT effect), so a fixed-threshold split
    # would starve LOW/HIGH of examples. Splitting on the actual score
    # distribution's own tertiles keeps the three classes roughly balanced,
    # which matters more for training a useful classifier than matching an
    # assumed "most people are moderate risk" population shape.
    low_cut, high_cut = np.quantile(score, [1 / 3, 2 / 3])
    labels = np.full(len(score), "MODERATE", dtype=object)
    labels[score < low_cut] = "LOW"
    labels[score >= high_cut] = "HIGH"
    return labels


def main():
    rng = np.random.default_rng(SEED)
    features = generate_features(rng, N_SAMPLES)
    scores = heuristic_risk_score(features, rng)
    features["riskScore"] = scores
    features["riskLabel"] = label_from_score(scores)

    out_path = "ml/data/synthetic_usage_risk.csv"
    import os
    os.makedirs("ml/data", exist_ok=True)
    features.to_csv(out_path, index=False)
    print(f"Wrote {len(features)} rows to {out_path}")
    print(features["riskLabel"].value_counts())


if __name__ == "__main__":
    main()
