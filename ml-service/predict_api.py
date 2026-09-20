"""
MachineSense ML Prediction Service
Exposes: POST /predict, GET /health, GET /model-info
"""
import os
import json
import joblib
import numpy as np
from flask import Flask, request, jsonify

MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

app = Flask(__name__)

_clf = None
_type_encoder = None
_ft_model = None
_ft_encoder = None
_features = None


def load_models():
    global _clf, _type_encoder, _ft_model, _ft_encoder, _features
    _clf = joblib.load(os.path.join(MODEL_DIR, "failure_model.pkl"))
    _type_encoder = joblib.load(os.path.join(MODEL_DIR, "type_encoder.pkl"))
    with open(os.path.join(MODEL_DIR, "feature_list.json")) as f:
        _features = json.load(f)
    ft_path = os.path.join(MODEL_DIR, "failure_type_model.pkl")
    if os.path.exists(ft_path):
        _ft_model = joblib.load(ft_path)
        _ft_encoder = joblib.load(os.path.join(MODEL_DIR, "failure_type_encoder.pkl"))


def health_score_from_probability(p):
    """Map failure probability (0-1) to a 0-100 health score (non-linear, feels natural)."""
    score = 100 * (1 - p) ** 1.3
    return int(round(max(0, min(100, score))))


def risk_level(p):
    if p < 0.15:
        return "HEALTHY"
    if p < 0.5:
        return "WARNING"
    return "CRITICAL"


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "UP", "modelLoaded": _clf is not None})


@app.route("/model-info", methods=["GET"])
def model_info():
    metrics_path = os.path.join(MODEL_DIR, "metrics.json")
    metrics = {}
    if os.path.exists(metrics_path):
        with open(metrics_path) as f:
            metrics = json.load(f)
    return jsonify({
        "features": _features,
        "metrics": metrics,
        "failureTypesSupported": _ft_model is not None,
        "failureTypes": list(_ft_encoder.classes_) if _ft_encoder is not None else [],
    })


@app.route("/predict", methods=["POST"])
def predict():
    if _clf is None:
        return jsonify({"error": "Model not loaded. Run train.py first."}), 503

    body = request.get_json(force=True)
    required = ["machineId", "machineType", "airTemperature", "processTemperature",
                "rotationalSpeed", "torque", "toolWear"]
    missing = [k for k in required if k not in body]
    if missing:
        return jsonify({"error": f"Missing fields: {missing}"}), 400

    try:
        type_encoded = _type_encoder.transform([body["machineType"]])[0]
    except ValueError:
        type_encoded = 0  # unseen type -> default to most common category

    row = np.array([[
        type_encoded,
        body["airTemperature"],
        body["processTemperature"],
        body["rotationalSpeed"],
        body["torque"],
        body["toolWear"],
    ]])

    proba = float(_clf.predict_proba(row)[0][1])
    score = health_score_from_probability(proba)
    risk = risk_level(proba)

    failure_type = None
    if _ft_model is not None and proba >= 0.5:
        pred_idx = _ft_model.predict(row)[0]
        failure_type = _ft_encoder.inverse_transform([pred_idx])[0]

    return jsonify({
        "machineId": body["machineId"],
        "failureProbability": round(proba, 4),
        "healthScore": score,
        "riskLevel": risk,
        "predictedFailureType": failure_type,
    })


load_models()

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)
