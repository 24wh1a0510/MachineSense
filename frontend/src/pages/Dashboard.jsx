import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client.js';
import MachineCard from '../components/MachineCard.jsx';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [machines, setMachines] = useState([]);
  const [predictions, setPredictions] = useState({});
  const [readings, setReadings] = useState({});
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const [summaryRes, machinesRes] = await Promise.all([
        api.dashboardSummary(),
        api.machines(),
      ]);
      setSummary(summaryRes.data);
      setMachines(machinesRes.data);

      const predPairs = await Promise.all(
        machinesRes.data.map(async (m) => {
          try {
            const r = await api.latestPrediction(m.id);
            return [m.id, r.data];
          } catch {
            return [m.id, null];
          }
        })
      );
      setPredictions(Object.fromEntries(predPairs));

      const readingPairs = await Promise.all(
        machinesRes.data.map(async (m) => {
          try {
            const r = await api.sensorHistory(m.id, 1);
            return [m.id, r.data[0] || null];
          } catch {
            return [m.id, null];
          }
        })
      );
      setReadings(Object.fromEntries(readingPairs));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, 8000);
    return () => clearInterval(interval);
  }, [load]);

  if (loading) return <div className="spinner" />;

  return (
    <div>
      <div className="stat-grid">
        <Stat label="Total Machines" value={summary?.totalMachines ?? 0} />
        <Stat label="Healthy" value={summary?.healthyMachines ?? 0} tone="healthy" />
        <Stat label="Warning" value={summary?.warningMachines ?? 0} tone="warning" />
        <Stat label="Critical" value={summary?.criticalMachines ?? 0} tone="critical" />
        <Stat label="Active Tickets" value={summary?.activeMaintenanceTickets ?? 0} />
        <Stat label="Factory Health" value={`${summary?.overallFactoryHealth ?? 100}%`} tone="healthy" />
      </div>

      <div className="section-title">Factory Overview</div>
      <div className="machine-grid">
        {machines.map((m) => (
          <MachineCard
            key={m.id}
            machine={m}
            prediction={predictions[m.id]}
            latestReading={readings[m.id]}
          />
        ))}
      </div>
    </div>
  );
}

function Stat({ label, value, tone }) {
  return (
    <div className="stat-card">
      <div className="stat-label">{label}</div>
      <div className={`stat-value ${tone || ''}`}>{value}</div>
    </div>
  );
}
