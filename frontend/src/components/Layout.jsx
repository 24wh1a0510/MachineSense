import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../api/AuthContext.jsx';

export default function Layout() {
  const { user, logout } = useAuth();

  const initials = (user?.fullName || user?.username || '?')
    .split(' ')
    .map((s) => s[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">MS</div>
          <div>
            <div className="brand-name">MachineSense</div>
            <div className="brand-sub">Asset Intelligence</div>
          </div>
        </div>

        <NavLink to="/" end className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
          Factory Overview
        </NavLink>
        <NavLink to="/tickets" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
          Maintenance Tickets
        </NavLink>
        {user?.role === 'ADMIN' && (
          <NavLink to="/register" className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
            Register Machine
          </NavLink>
        )}
      </aside>

      <div className="main-area">
        <div className="topbar">
          <div>
            <div className="page-title">Industrial Asset Monitoring</div>
            <div className="page-sub">Live sensor telemetry, ML failure risk and maintenance workflow</div>
          </div>
          <div className="user-chip">
            <div className="user-avatar">{initials}</div>
            <div>
              <div style={{ fontWeight: 600 }}>{user?.fullName || user?.username}</div>
              <div style={{ color: 'var(--text-muted)', fontSize: 11 }}>{user?.role}</div>
            </div>
            <button className="logout-btn" onClick={logout}>Sign out</button>
          </div>
        </div>
        <Outlet />
      </div>
    </div>
  );
}
