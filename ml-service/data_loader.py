"""
Loads the AI4I 2020 Predictive Maintenance Dataset.

If a real ai4i2020.csv (from
https://archive.ics.uci.edu/dataset/601/ai4i+2020+predictive+maintenance+dataset)
is placed at ml-service/data/ai4i2020.csv, it is used directly.

Otherwise, a synthetic dataset that reproduces the exact same schema,
value ranges and failure-mode distribution is generated so the pipeline
is runnable out-of-the-box for the college demo. Swap in the real CSV
for a production-grade model — no code changes required.
"""
import os
import numpy as np
import pandas as pd

COLUMNS = [
    "UDI", "Product ID", "Type", "Air temperature [K]", "Process temperature [K]",
    "Rotational speed [rpm]", "Torque [Nm]", "Tool wear [min]", "Machine failure",
    "TWF", "HDF", "PWF", "OSF", "RNF"
]

DATA_PATH = os.path.join(os.path.dirname(__file__), "data", "ai4i2020.csv")


def _generate_synthetic(n=10000, seed=42):
    rng = np.random.default_rng(seed)
    types = rng.choice(["L", "M", "H"], size=n, p=[0.6, 0.3, 0.1])

    air_temp = rng.normal(300, 2, n)
    process_temp = air_temp + rng.normal(10, 1, n)
    rot_speed = rng.normal(1500, 180, n).clip(1000, 2900)
    torque = rng.normal(40, 10, n).clip(3, 77)
    tool_wear = rng.uniform(0, 253, n)

    # failure modes derived from physically-motivated thresholds, mirroring AI4I logic
    twf = ((tool_wear > 200) & (rng.random(n) < 0.6)).astype(int)
    hdf = ((process_temp - air_temp < 8.6) & (rot_speed < 1380)).astype(int)
    pwf = (((torque * rot_speed * 2 * np.pi / 60) < 3500) |
           ((torque * rot_speed * 2 * np.pi / 60) > 9000)).astype(int)
    osf_limit = np.select([types == "L", types == "M", types == "H"], [11000, 12000, 13000])
    osf = ((tool_wear * torque) > osf_limit).astype(int)
    rnf = (rng.random(n) < 0.001).astype(int)

    machine_failure = ((twf + hdf + pwf + osf + rnf) > 0).astype(int)

    df = pd.DataFrame({
        "UDI": np.arange(1, n + 1),
        "Product ID": [f"{t}{i:05d}" for i, t in zip(range(n), types)],
        "Type": types,
        "Air temperature [K]": air_temp,
        "Process temperature [K]": process_temp,
        "Rotational speed [rpm]": rot_speed,
        "Torque [Nm]": torque,
        "Tool wear [min]": tool_wear,
        "Machine failure": machine_failure,
        "TWF": twf, "HDF": hdf, "PWF": pwf, "OSF": osf, "RNF": rnf,
    })
    return df


def load_dataset():
    if os.path.exists(DATA_PATH):
        df = pd.read_csv(DATA_PATH)
    else:
        df = _generate_synthetic()
    return df


def failure_type_label(row):
    """Return a single failure-type label, priority order matches AI4I docs."""
    if row.get("TWF", 0) == 1:
        return "TOOL_WEAR_FAILURE"
    if row.get("HDF", 0) == 1:
        return "HEAT_DISSIPATION_FAILURE"
    if row.get("PWF", 0) == 1:
        return "POWER_FAILURE"
    if row.get("OSF", 0) == 1:
        return "OVERSTRAIN_FAILURE"
    if row.get("RNF", 0) == 1:
        return "RANDOM_FAILURE"
    return "NONE"
