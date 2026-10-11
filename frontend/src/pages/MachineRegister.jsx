import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client.js';

export default function MachineRegister() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ machineCode: '', name: '', machineType: 'M', location: '', installedAt: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await api.registerMachine(form);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to register machine.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: 480 }}>
      <div className="section-title">Register New Machine</div>
      <form className="card" onSubmit={submit}>
        <div className="field">
          <label>Machine Code (e.g. M-006)</label>
          <input value={form.machineCode} onChange={update('machineCode')} required />
        </div>
        <div className="field">
          <label>Name</label>
          <input value={form.name} onChange={update('name')} required placeholder="e.g. Injection Molder" />
        </div>
        <div className="field">
          <label>Type</label>
          <select value={form.machineType} onChange={update('machineType')}>
            <option value="L">L — Low duty</option>
            <option value="M">M — Medium duty</option>
            <option value="H">H — High duty</option>
          </select>
        </div>
        <div className="field">
          <label>Location</label>
          <input value={form.location} onChange={update('location')} placeholder="e.g. Bay C" />
        </div>
        <div className="field">
          <label>Installed On</label>
          <input type="date" value={form.installedAt} onChange={update('installedAt')} />
        </div>

        {error && <div className="error-msg">{error}</div>}

        <button className="btn primary" disabled={saving} style={{ marginTop: 8 }}>
          {saving ? 'Saving…' : 'Register Machine'}
        </button>
      </form>
    </div>
  );
}
