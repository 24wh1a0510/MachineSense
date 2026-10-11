"""
Train two models on the AI4I 2020 dataset:
  1. Binary classifier -> failure probability (XGBoost)
  2. Multi-class classifier -> failure type, trained only on failed samples
     (Scikit-learn RandomForest, used only when failure_probability is high)

Run:  python train.py
Outputs saved to ml-service/models/
"""
import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (accuracy_score, precision_score, recall_score,
                              f1_score, roc_auc_score, classification_report)
from xgboost import XGBClassifier

from data_loader import load_dataset, failure_type_label

MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")
os.makedirs(MODEL_DIR, exist_ok=True)

FEATURES = [
    "type_encoded", "air_temperature", "process_temperature",
    "rotational_speed", "torque", "tool_wear",
]


def build_features(df):
    df = df.copy()
    type_encoder = LabelEncoder()
    df["type_encoded"] = type_encoder.fit_transform(df["Type"])
    df["air_temperature"] = df["Air temperature [K]"]
    df["process_temperature"] = df["Process temperature [K]"]
    df["rotational_speed"] = df["Rotational speed [rpm]"]
    df["torque"] = df["Torque [Nm]"]
    df["tool_wear"] = df["Tool wear [min]"]
    df["FailureType"] = df.apply(failure_type_label, axis=1)
    return df, type_encoder


def main():
    print("Loading AI4I 2020 dataset (real file if present, else synthetic fallback)...")
    df = load_dataset()
    df, type_encoder = build_features(df)

    X = df[FEATURES]
    y = df["Machine failure"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y)

    print("Training XGBoost failure-probability classifier...")
    clf = XGBClassifier(
        n_estimators=300, max_depth=5, learning_rate=0.05,
        subsample=0.9, colsample_bytree=0.9,
        eval_metric="logloss", random_state=42,
        scale_pos_weight=(len(y_train) - y_train.sum()) / max(y_train.sum(), 1),
    )
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)
    y_proba = clf.predict_proba(X_test)[:, 1]
    metrics = {
        "accuracy": accuracy_score(y_test, y_pred),
        "precision": precision_score(y_test, y_pred, zero_division=0),
        "recall": recall_score(y_test, y_pred, zero_division=0),
        "f1": f1_score(y_test, y_pred, zero_division=0),
        "roc_auc": roc_auc_score(y_test, y_proba),
    }
    print("Failure classifier metrics:", json.dumps(metrics, indent=2))
    print(classification_report(y_test, y_pred))

    # Failure-type classifier (only on samples that actually failed)
    failed = df[df["Machine failure"] == 1]
    failure_type_model = None
    ft_encoder = None
    if failed["FailureType"].nunique() > 1:
        print("Training failure-type classifier on failed samples...")
        ft_encoder = LabelEncoder()
        y_ft = ft_encoder.fit_transform(failed["FailureType"])
        X_ft = failed[FEATURES]
        failure_type_model = RandomForestClassifier(
            n_estimators=200, max_depth=8, random_state=42, class_weight="balanced")
        failure_type_model.fit(X_ft, y_ft)
        print("Failure types learned:", list(ft_encoder.classes_))

    joblib.dump(clf, os.path.join(MODEL_DIR, "failure_model.pkl"))
    joblib.dump(type_encoder, os.path.join(MODEL_DIR, "type_encoder.pkl"))
    if failure_type_model is not None:
        joblib.dump(failure_type_model, os.path.join(MODEL_DIR, "failure_type_model.pkl"))
        joblib.dump(ft_encoder, os.path.join(MODEL_DIR, "failure_type_encoder.pkl"))

    with open(os.path.join(MODEL_DIR, "metrics.json"), "w") as f:
        json.dump(metrics, f, indent=2)

    with open(os.path.join(MODEL_DIR, "feature_list.json"), "w") as f:
        json.dump(FEATURES, f)

    print(f"Models saved to {MODEL_DIR}")


if __name__ == "__main__":
    main()
