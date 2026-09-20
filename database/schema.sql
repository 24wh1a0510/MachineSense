-- MachineSense Database Schema (PostgreSQL)

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','TECHNICIAN')),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS machines (
    id SERIAL PRIMARY KEY,
    machine_code VARCHAR(20) UNIQUE NOT NULL,       -- e.g. M-001
    name VARCHAR(100) NOT NULL,                      -- e.g. CNC Machine
    machine_type VARCHAR(50) NOT NULL,               -- L/M/H per AI4I "Type"
    location VARCHAR(100),
    installed_at DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','MAINTENANCE','DECOMMISSIONED')),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sensor_readings (
    id BIGSERIAL PRIMARY KEY,
    machine_id INT NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    air_temperature DOUBLE PRECISION,
    process_temperature DOUBLE PRECISION,
    rotational_speed DOUBLE PRECISION,
    torque DOUBLE PRECISION,
    tool_wear DOUBLE PRECISION,
    vibration DOUBLE PRECISION,
    current DOUBLE PRECISION,
    pressure DOUBLE PRECISION,
    recorded_at TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_sensor_machine_time ON sensor_readings(machine_id, recorded_at DESC);

CREATE TABLE IF NOT EXISTS predictions (
    id BIGSERIAL PRIMARY KEY,
    machine_id INT NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    sensor_reading_id BIGINT REFERENCES sensor_readings(id) ON DELETE SET NULL,
    failure_probability DOUBLE PRECISION NOT NULL,
    health_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('HEALTHY','WARNING','CRITICAL')),
    predicted_failure_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_predictions_machine_time ON predictions(machine_id, created_at DESC);

CREATE TABLE IF NOT EXISTS maintenance_tickets (
    id SERIAL PRIMARY KEY,
    machine_id INT NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    prediction_id BIGINT REFERENCES predictions(id) ON DELETE SET NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    priority VARCHAR(20) DEFAULT 'MEDIUM' CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status VARCHAR(20) DEFAULT 'OPEN' CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','COMPLETED')),
    assigned_to INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS maintenance_history (
    id SERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES maintenance_tickets(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,
    notes TEXT,
    performed_by INT REFERENCES users(id),
    performed_at TIMESTAMP DEFAULT now()
);

-- Seed users (password = "password123" bcrypt hash below)
INSERT INTO users (username, password, full_name, role) VALUES
('admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5aBHNa4qJTPKKf1ZnH0KGiC0dV/9C', 'System Admin', 'ADMIN'),
('tech1', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5aBHNa4qJTPKKf1ZnH0KGiC0dV/9C', 'John Technician', 'TECHNICIAN')
ON CONFLICT DO NOTHING;

-- Seed machines
INSERT INTO machines (machine_code, name, machine_type, location, installed_at) VALUES
('M-001', 'CNC Machine', 'M', 'Bay A', '2022-01-15'),
('M-002', 'Hydraulic Press', 'H', 'Bay A', '2021-06-10'),
('M-003', 'Conveyor Motor', 'L', 'Bay B', '2023-03-20'),
('M-004', 'Industrial Lathe', 'M', 'Bay B', '2020-11-05'),
('M-005', 'Compressor Unit', 'H', 'Bay C', '2022-08-01')
ON CONFLICT DO NOTHING;
