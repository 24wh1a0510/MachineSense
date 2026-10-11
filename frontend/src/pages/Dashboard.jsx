import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client.js';
import MachineCard from '../components/MachineCard.jsx';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [machines, setMachines] = useState([]);
  const [predictions, setPredictions] = useState({});
  const [readings, setReadings] = useState({});
  const [loading, setLoading] = useState(true);
  const [alertStatus, setAlertStatus] = useState(null); // 'sending' | 'sent' | 'error'
  const [alertMachineId, setAlertMachineId] = useState(null);

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
    } catch (err) {
      console.error('Dashboard load error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, 5000);
    return () => clearInterval(interval);
  }, [load]);

  const handleSimulateAlert = async (machineId) => {
    setAlertMachineId(machineId);
    setAlertStatus('sending');
    try {
      await api.triggerDemoAlert(machineId);
      setAlertStatus('sent');
      setTimeout(() => setAlertStatus(null), 4000);
    } catch (err) {
      console.error('Alert trigger error:', err);
      setAlertStatus('error');
      setTimeout(() => setAlertStatus(null), 4000);
    }
  };

  if (loading) return <div className="spinner" />;

  // Pick any WARNING/CRITICAL machine for demo, fallback to first machine
  const demoMachine = machines.find(m => {
    const p = predictions[m.id];
    return p && (p.riskLevel === 'WARNING' || p.riskLevel === 'CRITICAL');
  }) || machines[0];

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

      {/* Demo Controls Bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        margin: '16px 0',
        padding: '12px 16px',
        background: 'rgba(255,255,255,0.04)',
        borderRadius: '8px',
        border: '1px solid rgba(255,255,255,0.08)',
        flexWrap: 'wrap'
      }}>
        <span style={{ color: 'rgba(255,255,255,0.5)', fontSize: '12px', fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase' }}>
          Demo Controls
        </span>

        {demoMachine && (
          <button
            onClick={() => handleSimulateAlert(demoMachine.id)}
            disabled={alertStatus === 'sending'}
            style={{
              padding: '8px 16px',
              background: alertStatus === 'sent' ? '#16a34a' : alertStatus === 'error' ? '#dc2626' : '#f59e0b',
              color: '#000',
              border: 'none',
              borderRadius: '6px',
              fontWeight: 700,
              fontSize: '13px',
              cursor: alertStatus === 'sending' ? 'wait' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all 0.2s',
            }}
          >
            {alertStatus === 'sending' && '⏳'}
            {alertStatus === 'sent' && '✅'}
            {alertStatus === 'error' && '❌'}
            {!alertStatus && '📱'}
            {alertStatus === 'sending' ? 'Sending...' : alertStatus === 'sent' ? 'Alert Sent!' : alertStatus === 'error' ? 'Check Telegram Config' : 'Send Telegram Alert'}
          </button>
        )}

        {alertStatus === 'sent' && (
          <span style={{ color: '#4ade80', fontSize: '13px' }}>
            Check your phone — Telegram message delivered to your device!
          </span>
        )}
        {alertStatus === 'error' && (
          <span style={{ color: '#f87171', fontSize: '13px' }}>
            Configure TELEGRAM_BOT_TOKEN + TELEGRAM_CHAT_ID to enable alerts.
          </span>
        )}
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
