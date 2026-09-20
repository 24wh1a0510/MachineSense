import { useNavigate } from 'react-router-dom';
import { riskClass, healthColor, riskLabel } from './statusUtils.js';

export default function MachineCard({ machine, prediction, latestReading }) {
  const navigate = useNavigate();
  const score = prediction?.healthScore ?? null;
  const risk = prediction?.riskLevel ?? null;

  return (
    <div className="machine-card" onClick={() => navigate(`/machines/${machine.id}`)} style={{ cursor: 'pointer' }}>
      <div className="machine-card-top">
        <div>
          <div className="machine-code">{machine.machineCode}</div>
          <div className="machine-name">{machine.name}</div>
        </div>
        <span className={`status-pill ${riskClass(risk)}`}>
          <span className="status-dot" />
          {riskLabel(risk)}
        </span>
      </div>

      <div className="health-bar-track">
        <div
          className="health-bar-fill"
          style={{ width: `${score ?? 0}%`, background: healthColor(score) }}
        />
      </div>

      <div className="metric-row">
        <div><span className="metric-label">Health</span><span className="metric-value">{score != null ? `${score}%` : '—'}</span></div>
        <div><span className="metric-label">Failure Risk</span><span className="metric-value">{prediction ? `${Math.round(prediction.failureProbability * 100)}%` : '—'}</span></div>
        <div><span className="metric-label">Temp</span><span className="metric-value">{latestReading ? `${latestReading.processTemperature?.toFixed(0)}°C` : '—'}</span></div>
        <div><span className="metric-label">RPM</span><span className="metric-value">{latestReading ? Math.round(latestReading.rotationalSpeed) : '—'}</span></div>
      </div>
    </div>
  );
}
