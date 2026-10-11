import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Sidebar({ isOpen, onClose }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user?.name
    ? user.name
        .split(' ')
        .map((w) => w[0])
        .join('')
        .toUpperCase()
        .slice(0, 2)
    : '?';

  return (
    <>
      {isOpen && <div className="sidebar-overlay" onClick={onClose} style={{
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', zIndex: 99,
        display: window.innerWidth <= 1024 ? 'block' : 'none'
      }} />}
      <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <div className="sidebar-logo">
            <div className="sidebar-logo-icon">⚡</div>
            <span className="sidebar-logo-text">API Health</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-section-title">Overview</div>
          <NavLink to="/" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} onClick={onClose} end>
            <span className="nav-item-icon">📊</span>
            Dashboard
          </NavLink>

          <div className="nav-section-title">Monitoring</div>
          <NavLink to="/apis" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} onClick={onClose}>
            <span className="nav-item-icon">🔗</span>
            Monitored APIs
          </NavLink>

          <div className="nav-section-title">Observability</div>
          <NavLink to="/alerts" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} onClick={onClose}>
            <span className="nav-item-icon">🔔</span>
            Alerts
          </NavLink>

          <div className="nav-section-title">System</div>
          <NavLink to="/cache" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} onClick={onClose}>
            <span className="nav-item-icon">💾</span>
            Cache Stats
          </NavLink>
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">{initials}</div>
            <div className="user-details">
              <div className="user-name">{user?.name || 'User'}</div>
              <div className="user-email">{user?.email || ''}</div>
            </div>
            <button className="logout-btn" onClick={handleLogout} title="Sign out">
              ⏻
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}
