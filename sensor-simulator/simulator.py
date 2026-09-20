"""
MachineSense Real-Time Sensor Simulator

Generates realistic sensor readings for several virtual industrial machines
and posts them to the Spring Boot backend, which forwards each reading to
the ML service for a live prediction, stores everything in PostgreSQL and
(when risk is high) opens a maintenance ticket automatically.

Each machine drifts between a HEALTHY baseline and an occasional
"degrading" episode (rising temperature / vibration / torque / current,
abnormal RPM) to make the live dashboard feel genuinely alive.

Usage:
    python simulator.py --backend http://localhost:8080 --interval 4
"""
import argparse
import random
import time
import sys
import requests

MACHINES = [
    {"code": "M-001", "type": "M"},
    {"code": "M-002", "type": "H"},
    {"code": "M-003", "type": "L"},
    {"code": "M-004", "type": "M"},
    {"code": "M-005", "type": "H"},
]


class MachineState:
    """Tracks a machine's drift so readings evolve smoothly over time."""

    def __init__(self, code, mtype):
        self.code = code
        self.type = mtype
        self.degrading = False
        self.degrade_ticks_left = 0
        self.air_temp = random.uniform(295, 301)
        self.tool_wear = random.uniform(0, 40)

    def maybe_start_degradation(self):
        if not self.degrading and random.random() < 0.03:
            self.degrading = True
            self.degrade_ticks_left = random.randint(8, 20)

    def tick(self):
        self.maybe_start_degradation()

        self.air_temp += random.uniform(-0.3, 0.3)
        process_temp = self.air_temp + random.uniform(8, 12)
        rot_speed = random.gauss(1500, 120)
        torque = random.gauss(40, 8)
        vibration = random.uniform(0.2, 0.8)
        current = random.uniform(4, 8)
        pressure = random.uniform(2, 6)
        self.tool_wear += random.uniform(0.1, 0.6)

        if self.degrading:
            severity = 1 - (self.degrade_ticks_left / 20)
            process_temp += severity * random.uniform(10, 25)
            rot_speed -= severity * random.uniform(100, 400)
            torque += severity * random.uniform(15, 35)
            vibration += severity * random.uniform(1.5, 4)
            current += severity * random.uniform(3, 10)
            self.tool_wear += severity * random.uniform(1, 3)

            self.degrade_ticks_left -= 1
            if self.degrade_ticks_left <= 0:
                self.degrading = False
                self.tool_wear = max(0, self.tool_wear - random.uniform(150, 200))  # tool replaced

        if self.tool_wear > 253:
            self.tool_wear = random.uniform(0, 20)  # tool replaced during maintenance

        return {
            "machineCode": self.code,
            "machineType": self.type,
            "airTemperature": round(self.air_temp, 2),
            "processTemperature": round(process_temp, 2),
            "rotationalSpeed": round(max(rot_speed, 200), 1),
            "torque": round(max(torque, 1), 2),
            "toolWear": round(self.tool_wear, 1),
            "vibration": round(max(vibration, 0), 3),
            "current": round(max(current, 0), 2),
            "pressure": round(max(pressure, 0), 2),
        }


def run(backend_url, interval, api_token=None):
    states = [MachineState(m["code"], m["type"]) for m in MACHINES]
    session = requests.Session()
    headers = {"Content-Type": "application/json"}
    if api_token:
        headers["Authorization"] = f"Bearer {api_token}"

    print(f"Starting simulator -> {backend_url}/api/sensors/ingest every {interval}s "
          f"for {len(states)} machines. Ctrl+C to stop.")

    while True:
        for state in states:
            reading = state.tick()
            try:
                resp = session.post(
                    f"{backend_url}/api/sensors/ingest",
                    json=reading, headers=headers, timeout=5,
                )
                tag = "DEGRADING" if state.degrading else "normal"
                print(f"[{state.code}] ({tag}) -> HTTP {resp.status_code}")
            except requests.RequestException as e:
                print(f"[{state.code}] send failed: {e}", file=sys.stderr)
        time.sleep(interval)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--backend", default="http://localhost:8080")
    parser.add_argument("--interval", type=float, default=4.0)
    parser.add_argument("--token", default=None, help="Optional bearer token for a service account")
    args = parser.parse_args()
    run(args.backend, args.interval, args.token)
