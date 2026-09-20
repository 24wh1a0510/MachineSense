# MachineSense — Intelligent Industrial Asset Monitoring

A production-style predictive-maintenance platform: virtual industrial machines stream live
sensor data, an ML model scores failure risk in real time, and the system automatically opens
maintenance tickets when a machine drifts into WARNING or CRITICAL territory.

```
Sensor Simulator → Spring Boot → Python ML Prediction → PostgreSQL → React Dashboard
```

## Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite, Recharts |
| Backend | Spring Boot 3 (Java 17), Spring Security + JWT |
| ML | Python, Scikit-learn, XGBoost, Flask |
| Database | PostgreSQL 16 |
| Simulator | Python |
| Containers | Docker + Kubernetes |

## Project structure

```
machinesense/
├── frontend/           React dashboard (Vite)
├── backend/             Spring Boot REST API
├── ml-service/           Model training + Flask prediction API
├── sensor-simulator/     Generates live sensor readings
├── database/             PostgreSQL schema + seed data
├── k8s/                  Kubernetes manifests
└── docker-compose.yml    Local multi-service orchestration
```

---

## 1. Run everything with Docker Compose (recommended)

Prerequisites: Docker + Docker Compose.

```bash
git clone <this-repo>
cd machinesense
docker compose up --build
```

This will:
1. Start PostgreSQL and load `database/schema.sql` (creates tables + seeds 5 demo machines and
   2 demo users).
2. Build the ML service image, **train the model during the image build**, and start the Flask
   prediction API on port 5000.
3. Build and start the Spring Boot backend on port 8080.
4. Build and start the React frontend on port 3000.
5. Start the sensor simulator, which begins posting readings for 5 virtual machines every 4
   seconds.

Open **http://localhost:3000** and sign in:

| Username | Password | Role |
|---|---|---|
| `admin` | `password123` | Admin |
| `tech1` | `password123` | Maintenance Technician |

Within a few seconds of the simulator running you'll see live health scores update on the
Factory Overview page, and maintenance tickets appear automatically as machines drift into
WARNING/CRITICAL risk.

To stop: `docker compose down` (add `-v` to also wipe the Postgres volume).

---

## 2. Run components individually (development mode)

### Database
```bash
docker run --name ms-postgres -e POSTGRES_DB=machinesense -e POSTGRES_USER=machinesense \
  -e POSTGRES_PASSWORD=machinesense -p 5432:5432 -d postgres:16-alpine
psql -h localhost -U machinesense -d machinesense -f database/schema.sql
```

### ML service
```bash
cd ml-service
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
python train.py          # trains model, saves to ml-service/models/
python predict_api.py    # serves on http://localhost:5000
```
To train on the **real AI4I 2020 dataset** instead of the synthetic fallback, download
`ai4i2020.csv` from the UCI repository and place it at `ml-service/data/ai4i2020.csv`, then
re-run `python train.py`.

### Backend
```bash
cd backend
mvn spring-boot:run
# or: mvn clean package && java -jar target/machinesense-backend.jar
```
Set `DB_HOST`, `ML_SERVICE_URL` etc. via environment variables if not using the defaults in
`application.yml`.

### Frontend
```bash
cd frontend
npm install
npm run dev   # http://localhost:3000, proxies API calls to http://localhost:8080
```

### Sensor simulator
```bash
cd sensor-simulator
pip install -r requirements.txt
python simulator.py --backend http://localhost:8080 --interval 4
```

---

## 3. Run on Kubernetes

Prerequisites: a running cluster (minikube, kind, etc.), `kubectl`, and images pushed to a
registry your cluster can pull from (or built directly into the cluster's Docker daemon, e.g.
`eval $(minikube docker-env)`).

```bash
# Build images (tag to match k8s manifests, or push to your registry and update the image: fields)
docker build -t machinesense/backend:latest ./backend
docker build -t machinesense/ml-service:latest ./ml-service
docker build -t machinesense/frontend:latest ./frontend
docker build -t machinesense/sensor-simulator:latest ./sensor-simulator

# Apply manifests
kubectl apply -f k8s/00-namespace.yaml
kubectl create configmap postgres-init-schema \
  --from-file=schema.sql=database/schema.sql -n machinesense --dry-run=client -o yaml \
  | kubectl apply -f -
kubectl apply -f k8s/
```

Check status:
```bash
kubectl get pods -n machinesense
kubectl get svc -n machinesense
```

Access the app via the `frontend` NodePort service (default `30080`) or through the provided
Ingress (`k8s/07-ingress.yaml`) if an ingress controller is installed.

The manifests include: Deployments with readiness/liveness probes for every service,
a Postgres StatefulSet-style Deployment backed by a PersistentVolumeClaim, ConfigMaps/Secrets
for configuration, and HorizontalPodAutoscalers for the backend and ML service.

---

## API reference (Spring Boot backend, base URL `http://localhost:8080`)

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` → `{ token, username, role, fullName }` |

All endpoints below (except `/api/auth/**`, `/api/sensors/ingest`, `/actuator/**`) require
`Authorization: Bearer <token>`.

### Machines
| Method | Path | Description |
|---|---|---|
| GET | `/api/machines` | List all machines |
| GET | `/api/machines/{id}` | Machine by ID |
| GET | `/api/machines/code/{code}` | Machine by code (e.g. `M-001`) |
| POST | `/api/machines/register` | Admin only — register a new machine |
| DELETE | `/api/machines/{id}/delete` | Admin only |

### Sensors
| Method | Path | Description |
|---|---|---|
| POST | `/api/sensors/ingest` | Public — simulator posts a reading here; triggers ML prediction + auto-ticketing |
| GET | `/api/sensors/machine/{id}?limit=50` | Recent readings for a machine |

### Predictions
| Method | Path | Description |
|---|---|---|
| GET | `/api/predictions/machine/{id}?limit=50` | Prediction history |
| GET | `/api/predictions/machine/{id}/latest` | Most recent prediction |

### Maintenance tickets
| Method | Path | Description |
|---|---|---|
| GET | `/api/tickets` | All tickets |
| GET | `/api/tickets/machine/{id}` | Tickets for a machine |
| GET | `/api/tickets/status/{status}` | Filter by status |
| GET | `/api/tickets/technician/{userId}` | Tickets assigned to a technician |
| PATCH | `/api/tickets/{id}` | `{ status?, assignedToId?, notes? }` — update status/assignment |
| GET | `/api/tickets/{id}/history` | Audit trail for a ticket |

### Dashboard
| Method | Path | Description |
|---|---|---|
| GET | `/api/dashboard/summary` | Totals, health counts, active tickets, overall factory health |

### ML service (`http://localhost:5000`, called internally by the backend)
| Method | Path | Description |
|---|---|---|
| POST | `/predict` | `{ machineId, machineType, airTemperature, processTemperature, rotationalSpeed, torque, toolWear }` → `{ failureProbability, healthScore, riskLevel, predictedFailureType }` |
| GET | `/health` | Liveness/model-loaded check |
| GET | `/model-info` | Feature list, training metrics, supported failure types |

---

## Machine learning notes

- Trained on the **AI4I 2020 Predictive Maintenance Dataset** schema (Air/Process temperature,
  Rotational speed, Torque, Tool wear, Type). If the real CSV isn't present, `train.py` falls
  back to a synthetic dataset generated with the same physically-motivated failure rules (tool
  wear, heat dissipation, power, overstrain, random failure) so the pipeline runs out of the box.
- **Failure probability** comes from an XGBoost binary classifier.
- **Health score (0–100)** is a non-linear transform of failure probability.
- **Risk level**: HEALTHY (<15% failure probability), WARNING (15–50%), CRITICAL (>50%).
- **Failure type** is predicted by a secondary RandomForest classifier, only surfaced when
  failure probability ≥ 50% — the system does not claim a failure type or exact failure time it
  cannot support.

## Testing

- **ML service**: `cd ml-service && python train.py` prints accuracy/precision/recall/F1/ROC-AUC
  on a held-out test split. `python -c "import predict_api"` and hit `/predict` with `curl` or
  the Flask test client to sanity-check responses.
- **Backend**: `mvn test` (add unit tests under `src/test/java` as needed for your grading
  rubric — the service layer is structured so each service can be tested in isolation with
  mocked repositories).
- **End-to-end**: run `docker compose up --build`, watch simulator logs
  (`docker compose logs -f sensor-simulator`) to confirm HTTP 200s, then confirm the dashboard
  updates live and tickets appear under **Maintenance Tickets** once a machine goes into
  WARNING/CRITICAL.

## Team split suggestion (4-member college team)

1. **Backend/DB** — Spring Boot APIs, security, PostgreSQL schema
2. **ML** — training pipeline, prediction API, model evaluation
3. **Frontend** — React dashboard, charts, UX polish
4. **DevOps** — Docker, Kubernetes, simulator, integration testing, README/demo script
