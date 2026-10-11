# MachineSense — Intelligent Industrial Asset Monitoring

> A production-grade predictive maintenance platform. Industrial machines stream live sensor data, an ML model scores failure risk in real time, and the system automatically opens maintenance tickets and sends phone alerts when a machine drifts into WARNING or CRITICAL territory.

```
IoT Sensors / Simulator → Spring Boot API → Python ML Service → PostgreSQL → React Dashboard → Telegram Alerts
```

---

## The Problem

Unplanned industrial downtime costs manufacturers an average of **$260,000 per hour**. Traditional maintenance is either reactive (fix after failure) or schedule-based (service every N months regardless of condition). Both are wasteful or dangerous.

**MachineSense enables predictive maintenance** — the system detects degradation before failure occurs, automatically raises a ticket, and instantly notifies the maintenance team on their phones.

---

## Stack

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | React 18 + Vite + Recharts | Live dashboard, ticket management |
| Backend | Spring Boot 3, Java 17, Spring Security + JWT | REST API, business logic, security |
| ML Service | Python, XGBoost, Scikit-learn, Flask | Failure prediction engine |
| Database | PostgreSQL 16 | Persistent storage for all data |
| Alerts | Telegram Bot API | Instant phone notifications |
| Simulator | Python | Mimics IoT sensors on 5 machines |
| Containers | Docker + Docker Compose | Local orchestration |
| Cloud | Kubernetes | Production deployment manifests |

---

## Architecture

```
┌─────────────────┐     POST /api/sensors/ingest      ┌──────────────────────┐
│  Sensor Layer   │ ─────────────────────────────────▶ │   Spring Boot API    │
│  (simulator or  │                                    │                      │
│  real hardware) │                                    │  • JWT Auth          │
└─────────────────┘                                    │  • Sensor Ingestion  │
                                                       │  • ML Client         │
┌─────────────────┐     GET /api/...                   │  • Ticket Service    │
│ React Dashboard │ ◀───────────────────────────────── │  • Telegram Alerts   │
│                 │     polls every 5 seconds          └──────────┬───────────┘
│ • Factory View  │                                               │
│ • Live Charts   │                                    ┌──────────▼───────────┐
│ • Tickets       │                                    │   ML Service (Flask) │
│ • Demo Controls │                                    │                      │
└─────────────────┘                                    │  • XGBoost           │
                                                       │  • Random Forest     │
┌─────────────────┐                                    │  • Health Score      │
│    Telegram     │ ◀── alert on WARNING/CRITICAL       └──────────┬───────────┘
│  (phone alert)  │                                               │
└─────────────────┘                                    ┌──────────▼───────────┐
                                                       │  PostgreSQL Database  │
                                                       │                      │
                                                       │  • machines          │
                                                       │  • sensor_readings   │
                                                       │  • predictions       │
                                                       │  • maintenance_tickets│
                                                       │  • users             │
                                                       └──────────────────────┘
```

---

## Project Structure

```
machinesense/
├── backend/                        Spring Boot REST API (Java 17)
│   ├── src/main/java/com/machinesense/
│   │   ├── controller/             HTTP endpoints (Auth, Sensors, Tickets, Dashboard, Demo)
│   │   ├── service/                Business logic (Ingestion, ML Client, Telegram, Tickets)
│   │   ├── entity/                 JPA database entities
│   │   ├── repository/             Spring Data JPA repositories
│   │   ├── dto/                    Request/Response data classes
│   │   ├── config/                 Security, CORS configuration
│   │   └── security/               JWT filter, UserDetailsService
│   └── src/main/resources/
│       └── application.yml         Configuration (DB, JWT, ML URL, Telegram)
│
├── frontend/                       React 18 + Vite dashboard
│   └── src/
│       ├── pages/                  Dashboard, MachineDetail, Tickets, Login, Register
│       ├── components/             MachineCard, Layout, statusUtils
│       └── api/                    Axios client, AuthContext
│
├── ml-service/                     Python ML prediction engine
│   ├── train.py                    Model training (XGBoost + Random Forest)
│   ├── predict_api.py              Flask prediction API
│   ├── data_loader.py              Loads AI4I dataset or generates synthetic fallback
│   └── models/                     Saved trained models
│
├── sensor-simulator/               Python IoT sensor simulator
│   └── simulator.py                Posts readings for 5 machines every 4 seconds
│
├── database/
│   └── schema.sql                  PostgreSQL tables + seed data (5 machines, 2 users)
│
├── k8s/                            Kubernetes manifests
│   ├── 00-namespace.yaml
│   ├── 01-postgres-configmap-secret.yaml
│   ├── 02-postgres.yaml
│   ├── 03-ml-service.yaml
│   ├── 04-backend.yaml
│   ├── 05-frontend.yaml
│   └── 07-ingress.yaml
│
└── docker-compose.yml              Full local orchestration
```

---

## Quick Start — Docker Compose (Recommended)

**Prerequisites:** Docker Desktop installed and running.

```bash
git clone <this-repo>
cd machinesense
docker compose up --build
```

Open **http://localhost:3000** and sign in:

| Username | Password | Role |
|---|---|---|
| `admin` | `password123` | Admin — full access |
| `tech1` | `password123` | Maintenance Technician |

Within 30 seconds you will see live health scores updating on the Factory Overview page. Maintenance tickets appear automatically as machines drift into WARNING or CRITICAL risk.

**To stop:**
```bash
docker compose down          # stops containers, keeps database
docker compose down -v       # stops containers + wipes database
```

---

## Telegram Phone Alerts (Optional but Recommended for Demo)

MachineSense can send instant phone alerts via a Telegram bot whenever a machine hits WARNING or CRITICAL risk.

### Setup (5 minutes)

1. Open Telegram → search **@BotFather** → send `/newbot`
2. Follow prompts → BotFather gives you a **bot token** (format: `1234567890:AAxxxxxxx`)
3. Search for your new bot in Telegram → press **Start**
4. Visit this URL in your browser (replace YOUR_TOKEN):
   ```
   https://api.telegram.org/botYOUR_TOKEN/getUpdates
   ```
5. Copy the `chat.id` number from the response — that is your **chat ID**

### Configure

Create a `.env` file in the project root:

```env
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_CHAT_ID=your_chat_id_here
```

Then start with:
```bash
docker compose up -d
```

Docker Compose automatically reads the `.env` file. No rebuild needed.

### What the alert looks like on your phone

```
🟡 MachineSense Alert 🟡

🏭 Machine: CNC Machine (M-001)
📊 Risk level: WARNING
❤️ Health score: 62/100
📈 Failure probability: 35.0%
⚠️ Predicted failure: Heat Dissipation

👷 A maintenance ticket has been automatically created.
Please inspect the machine immediately.
```

---

## Demo Controls

The dashboard includes built-in demo controls for presentations:

- **📱 Send Telegram Alert** — sends a test alert to your phone instantly, no degradation needed
- **⚡ Simulate Degradation** (on any machine detail page) — injects a high-risk sensor reading, triggers the full pipeline: ML prediction → ticket creation → Telegram alert → live chart update

---

## Machine Learning

### Dataset
Trained on the **AI4I 2020 Predictive Maintenance Dataset** from the UCI Machine Learning Repository — collected from real CNC machines.

Features used: Air Temperature, Process Temperature, Rotational Speed, Torque, Tool Wear, Machine Type.

If the dataset CSV is not present at `ml-service/data/ai4i2020.csv`, `train.py` automatically generates a synthetic fallback dataset using the same physically-motivated failure rules.

### Models

| Model | Algorithm | Output |
|---|---|---|
| Failure classifier | XGBoost | Failure probability (0.0 – 1.0) |
| Failure type classifier | Random Forest | Failure type (Heat Dissipation, Tool Wear, Power, Overstrain, Random) |

### Risk Levels

| Risk Level | Failure Probability | Action |
|---|---|---|
| HEALTHY | < 15% | No action — continue monitoring |
| WARNING | 15% – 50% | Schedule inspection within current shift |
| CRITICAL | > 50% | Stop machine, dispatch technician immediately |

### Health Score
Non-linear transform of failure probability to a 0–100 scale. 100 = perfect health, 0 = imminent failure.

---

## Data Flow — Step by Step

```
1. Sensor simulator posts a reading every 4 seconds:
   POST /api/sensors/ingest
   { machineCode, machineType, airTemperature, processTemperature,
     rotationalSpeed, torque, toolWear }

2. Backend saves the reading to sensor_readings table

3. Backend calls ML service:
   POST http://ml-service:5000/predict
   ← { failureProbability, healthScore, riskLevel, predictedFailureType }

4. Backend saves prediction to predictions table

5. If riskLevel is WARNING or CRITICAL and no open ticket exists:
   → Creates maintenance ticket in maintenance_tickets table
   → Sends Telegram alert to maintenance team's phones

6. React dashboard polls /api/machines, /api/predictions, /api/sensors every 5 seconds
   → Machine cards update live with new health scores and risk status
```

---

## API Reference

Base URL: `http://localhost:8080`

All endpoints except `/api/auth/**`, `/api/sensors/ingest`, and `/actuator/**` require:
```
Authorization: Bearer <jwt_token>
```

### Authentication
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` | `{ token, username, role, fullName }` |

### Machines
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/machines` | Any | List all machines |
| GET | `/api/machines/{id}` | Any | Machine by ID |
| POST | `/api/machines/register` | Admin | Register a new machine |
| DELETE | `/api/machines/{id}/delete` | Admin | Remove a machine |

### Sensors
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/sensors/ingest` | Public | Post a sensor reading — triggers full pipeline |
| GET | `/api/sensors/machine/{id}?limit=50` | Any | Recent readings for a machine |

### Predictions
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/predictions/machine/{id}/latest` | Any | Most recent prediction |
| GET | `/api/predictions/machine/{id}?limit=50` | Any | Prediction history |

### Maintenance Tickets
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/tickets` | Any | All tickets |
| GET | `/api/tickets/machine/{id}` | Any | Tickets for a machine |
| GET | `/api/tickets/status/{status}` | Any | Filter by OPEN / ASSIGNED / IN_PROGRESS / COMPLETED |
| PATCH | `/api/tickets/{id}` | Any | Update status, assign technician, add notes |
| GET | `/api/tickets/{id}/history` | Any | Full audit trail |

### Dashboard
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/dashboard/summary` | Any | Total machines, health counts, active tickets, factory health % |

### Demo
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/demo/trigger-alert/{machineId}` | Any | Send a test Telegram alert for demo purposes |

### ML Service (internal, port 5000)
| Method | Path | Description |
|---|---|---|
| POST | `/predict` | Run prediction on sensor values |
| GET | `/health` | Liveness check |
| GET | `/model-info` | Model features, metrics, failure types |

---

## Running Services Individually (Development)

### Database
```bash
docker run --name ms-postgres \
  -e POSTGRES_DB=machinesense \
  -e POSTGRES_USER=machinesense \
  -e POSTGRES_PASSWORD=machinesense \
  -p 5432:5432 -d postgres:16-alpine

psql -h localhost -U machinesense -d machinesense -f database/schema.sql
```

### ML Service
```bash
cd ml-service
python -m venv venv && source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
python train.py         # trains and saves models to ml-service/models/
python predict_api.py   # starts Flask API on http://localhost:5000
```

### Backend
```bash
cd backend
mvn spring-boot:run
# Needs DB_HOST, ML_SERVICE_URL env vars if not using defaults in application.yml
```

### Frontend
```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

### Sensor Simulator
```bash
cd sensor-simulator
pip install -r requirements.txt
python simulator.py --backend http://localhost:8080 --interval 4
```

---

## Kubernetes Deployment

Prerequisites: A running cluster (minikube, kind, EKS, GKE, etc.) with `kubectl` configured.

```bash
# Build and tag images
docker build -t machinesense/backend:latest ./backend
docker build -t machinesense/ml-service:latest ./ml-service
docker build -t machinesense/frontend:latest ./frontend
docker build -t machinesense/sensor-simulator:latest ./sensor-simulator

# Apply manifests
kubectl apply -f k8s/00-namespace.yaml
kubectl create configmap postgres-init-schema \
  --from-file=schema.sql=database/schema.sql \
  -n machinesense --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/

# Check status
kubectl get pods -n machinesense
kubectl get svc -n machinesense
```

### What the Kubernetes manifests include

| Manifest | What it does |
|---|---|
| `00-namespace.yaml` | Isolates all resources in `machinesense` namespace |
| `01-postgres-configmap-secret.yaml` | DB credentials as Kubernetes Secrets |
| `02-postgres.yaml` | Postgres Deployment + PersistentVolumeClaim |
| `03-ml-service.yaml` | ML Flask API Deployment + ClusterIP Service |
| `04-backend.yaml` | Spring Boot Deployment + Service + HPA |
| `05-frontend.yaml` | React frontend Deployment + NodePort Service |
| `07-ingress.yaml` | Ingress routing for external access |

All Deployments include **readiness and liveness probes** so Kubernetes knows when each service is actually ready and automatically restarts unhealthy pods.

---

## Default Credentials

| Service | Username | Password |
|---|---|---|
| Dashboard (Admin) | `admin` | `password123` |
| Dashboard (Technician) | `tech1` | `password123` |
| PostgreSQL | `machinesense` | `machinesense` |

---

## Team

| Role | Responsibility |
|---|---|
| Backend & Database | Spring Boot API, PostgreSQL schema, JWT security, sensor ingestion pipeline, Telegram alerts |
| Machine Learning | Model training (XGBoost + Random Forest), Flask prediction API, dataset processing |
| Frontend | React dashboard, live charts, ticket management UI, dark industrial design |
| DevOps & Simulator | Docker, Kubernetes, sensor simulator, integration, deployment |
