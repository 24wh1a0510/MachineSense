import { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { api } from '../api/client.js';
import { riskClass, healthColor, riskLabel } from '../components/statusUtils.js';

export default function MachineDetail() {
  const { id } = useParams();
  const [machine, setMachine] = useState(null);
  const [readings, setReadings] = useState([]);
  const [predictions, setPredictions] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    const [m, r, p, t] = await Promise.all([
      api.machine(id),
      api.sensorHistory(id, 40),
      api.predictionHistory(id, 40),
      api.ticketsByMachine(id),
    ]);
    setMachine(m.data);
    setReadings([...r.data].reverse());
    setPredictions([...p.data].reverse());
    setTickets(t.data);
    setLoading(false);
  }, [id]);

  useEffect(() => {
    load();
    const interval = setInterval(load, 6000);
    return () => clearInterval(interval);
  }, [load]);

  if (loading || !machine) return <div className="spinner" />;

  const latestReading = readings[readings.length - 1];
  const latestPrediction = predictions[predictions.length - 1];

  const chartData = readings.map((r, i) => ({
    idx: i,
    temp: r.processTemperature,
    torque: r.torque,
    rpm: r.rotationalSpeed,
  }));

  const healthChartData = predictions.map((p, i) => ({
    idx: i,
    score: p.healthScore,
    prob: Math.round(p.failureProbability * 100),
  }));

  return (
    <div>
      <Link to="/" className="back-link">← Back to Factory Overview</Link>

      <div className="detail-header">
        <div>
          <div className="page-title">{machine.name} <span className="mono" style={{ color: 'var(--text-muted)', fontSize: 15 }}>{machine.machineCode}</span></div>
          <div className="page-sub">{machine.location} · Type {machine.machineType} · Installed {machine.installedAt || '—'}</div>
        </div>
        <span className={`status-pill ${riskClass(latestPrediction?.riskLevel)}`} style={{ fontSize: 13, padding: '8px 16px' }}>
          <span className="status-dot" />
          {riskLabel(latestPrediction?.riskLevel)}
        </span>
      </div>

      <div className="stat-grid" style={{ gridTemplateColumns: 'repeat(5, 1fr)' }}>
        <MiniStat label="Health Score" value={latestPrediction ? `${latestPrediction.healthScore}%` : '—'} color={healthColor(latestPrediction?.healthScore)} />
        <MiniStat label="Failure Risk" value={latestPrediction ? `${Math.round(latestPrediction.failureProbability * 100)}%` : '—'} />
        <MiniStat label="Temperature" value={latestReading ? `${latestReading.processTemperature?.toFixed(1)}°C` : '—'} />
        <MiniStat label="RPM" value={latestReading ? Math.round(latestReading.rotationalSpeed) : '—'} />
        <MiniStat label="Torque" value={latestReading ? `${latestReading.torque?.toFixed(1)} Nm` : '—'} />
      </div>

      <div className="two-col">
        <div>
          <div className="card chart-card">
            <div className="section-title">Live Sensor Trend — Temperature & Torque</div>
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={chartData}>
                <CartesianGrid stroke="#1b2128" vertical={false} />
                <XAxis dataKey="idx" hide />
                <YAxis stroke="#5c6772" fontSize={11} />
                <Tooltip contentStyle={{ background: '#171c22', border: '1px solid #232a32', fontSize: 12 }} />
                <Line type="monotone" dataKey="temp" stroke="#f2a341" strokeWidth={2} dot={false} name="Process Temp (°C)" />
                <Line type="monotone" dataKey="torque" stroke="#4fd1c5" strokeWidth={2} dot={false} name="Torque (Nm)" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="card chart-card">
            <div className="section-title">Health Score & Failure Probability History</div>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={healthChartData}>
                <CartesianGrid stroke="#1b2128" vertical={false} />
                <XAxis dataKey="idx" hide />
                <YAxis stroke="#5c6772" fontSize={11} domain={[0, 100]} />
                <Tooltip contentStyle={{ background: '#171c22', border: '1px solid #232a32', fontSize: 12 }} />
                <Line type="monotone" dataKey="score" stroke="#3ecf8e" strokeWidth={2} dot={false} name="Health Score" />
                <Line type="monotone" dataKey="prob" stroke="#f2545b" strokeWidth={2} dot={false} name="Failure Probability %" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <div className="section-title">Maintenance History</div>
            {tickets.length === 0 ? (
              <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>No maintenance tickets recorded for this machine yet.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr><th>Title</th><th>Priority</th><th>Status</th><th>Opened</th></tr>
                </thead>
                <tbody>
                  {tickets.map((t) => (
                    <tr key={t.id}>
                      <td>{t.title}</td>
                      <td><span className={`badge priority-${t.priority}`}>{t.priority}</span></td>
                      <td><span className={`badge status-${t.status}`}>{t.status.replace('_', ' ')}</span></td>
                      <td className="mono">{new Date(t.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <div>
          <div className="card">
            <div className="section-title">Prediction Detail</div>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
              <Row label="Predicted Failure Type" value={latestPrediction?.predictedFailureType || 'None detected'} />
              <Row label="Risk Level" value={riskLabel(latestPrediction?.riskLevel)} />
              <Row label="Last Updated" value={latestPrediction ? new Date(latestPrediction.createdAt).toLocaleTimeString() : '—'} />
            </div>

            <div className="section-title" style={{ marginTop: 20 }}>Recommended Action</div>
            <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
              {recommendedAction(latestPrediction)}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function recommendedAction(prediction) {
  if (!prediction) return 'Awaiting first sensor reading.';
  if (prediction.riskLevel === 'CRITICAL') {
    return 'Stop the machine at the next safe opportunity and dispatch a technician immediately. A maintenance ticket has been auto-generated.';
  }
  if (prediction.riskLevel === 'WARNING') {
    return 'Schedule a preventive maintenance inspection within the next shift. Monitor trend closely.';
  }
  return 'No action required. Continue routine monitoring.';
}

function MiniStat({ label, value, color }) {
  return (
    <div className="stat-card mini-stat">
      <div className="val" style={{ color: color || 'var(--text-primary)' }}>{value}</div>
      <div className="lab">{label}</div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0' }}>
      <span>{label}</span>
      <span className="mono" style={{ color: 'var(--text-primary)' }}>{value}</span>
    </div>
  );
}
