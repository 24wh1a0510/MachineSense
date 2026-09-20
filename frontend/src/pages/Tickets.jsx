import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../api/AuthContext.jsx';

const STATUS_OPTIONS = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED'];

export default function Tickets() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [machines, setMachines] = useState({});
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    const [t, m] = await Promise.all([api.tickets(), api.machines()]);
    setTickets(t.data);
    setMachines(Object.fromEntries(m.data.map((mm) => [mm.id, mm])));
    setLoading(false);
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, 8000);
    return () => clearInterval(interval);
  }, [load]);

  const updateStatus = async (ticketId, status) => {
    await api.updateTicket(ticketId, { status });
    load();
  };

  if (loading) return <div className="spinner" />;

  const filtered = filter === 'ALL' ? tickets : tickets.filter((t) => t.status === filter);

  return (
    <div>
      <div className="topbar" style={{ marginBottom: 18 }}>
        <div className="section-title" style={{ margin: 0 }}>
          {user?.role === 'TECHNICIAN' ? 'Technician Dashboard — Maintenance Tickets' : 'Maintenance Tickets'}
        </div>
        <select value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">All statuses</option>
          {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
        </select>
      </div>

      <div className="card">
        {filtered.length === 0 ? (
          <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>No tickets match this filter.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Machine</th>
                <th>Title</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Opened</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((t) => (
                <tr key={t.id}>
                  <td className="mono">{machines[t.machine?.id]?.machineCode || t.machine?.machineCode || '—'}</td>
                  <td>{t.title}</td>
                  <td><span className={`badge priority-${t.priority}`}>{t.priority}</span></td>
                  <td><span className={`badge status-${t.status}`}>{t.status.replace('_', ' ')}</span></td>
                  <td className="mono">{new Date(t.createdAt).toLocaleString()}</td>
                  <td>
                    <select
                      value={t.status}
                      onChange={(e) => updateStatus(t.id, e.target.value)}
                    >
                      {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
