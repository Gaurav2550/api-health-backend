import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useToast } from '../context/ToastContext';

function StatusBadge({ status }) {
  const s = (status || '').toLowerCase();
  const label = s === 'up' ? 'Healthy' : s === 'down' ? 'Down' : s === 'slow' ? 'Slow' : 'Unknown';
  const cls = s === 'up' ? 'up' : s === 'down' ? 'down' : s === 'slow' ? 'slow' : 'unknown';
  return (
    <span className={`status-badge ${cls}`}>
      <span className="status-dot" />
      {label}
    </span>
  );
}

function AddApiModal({ onClose, onCreated }) {
  const { addToast } = useToast();
  const [form, setForm] = useState({
    name: '',
    url: '',
    httpMethod: 'GET',
    checkIntervalSeconds: 60,
    timeoutMillis: 5000,
    latencyThresholdMillis: 400,
    expectedStatusCodes: '200',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (field, value) => {
    setForm((f) => ({ ...f, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const payload = {
        ...form,
        checkIntervalSeconds: parseInt(form.checkIntervalSeconds),
        timeoutMillis: parseInt(form.timeoutMillis),
        latencyThresholdMillis: parseInt(form.latencyThresholdMillis),
      };
      await api.createApi(payload);
      addToast('API registered successfully', 'success');
      onCreated();
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">Register New API</h2>
          <button className="btn-icon" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="auth-error"><span>⚠</span> {error}</div>}

            <div className="form-group">
              <label className="form-label">API Name *</label>
              <input className="form-input" placeholder="e.g. GitHub Status API" value={form.name} onChange={(e) => handleChange('name', e.target.value)} required />
            </div>

            <div className="form-group">
              <label className="form-label">Endpoint URL *</label>
              <input className="form-input" placeholder="https://api.example.com/health" value={form.url} onChange={(e) => handleChange('url', e.target.value)} required />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">HTTP Method</label>
                <select className="form-select" value={form.httpMethod} onChange={(e) => handleChange('httpMethod', e.target.value)}>
                  <option value="GET">GET</option>
                  <option value="POST">POST</option>
                  <option value="PUT">PUT</option>
                  <option value="HEAD">HEAD</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Expected Status</label>
                <input className="form-input" placeholder="200" value={form.expectedStatusCodes} onChange={(e) => handleChange('expectedStatusCodes', e.target.value)} />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Check Interval (sec)</label>
                <input className="form-input" type="number" min="10" max="3600" value={form.checkIntervalSeconds} onChange={(e) => handleChange('checkIntervalSeconds', e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Timeout (ms)</label>
                <input className="form-input" type="number" min="1000" max="30000" value={form.timeoutMillis} onChange={(e) => handleChange('timeoutMillis', e.target.value)} />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Latency Threshold (ms)</label>
              <input className="form-input" type="number" min="50" max="60000" value={form.latencyThresholdMillis} onChange={(e) => handleChange('latencyThresholdMillis', e.target.value)} />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Registering...' : '+ Register API'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function ApisPage() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [apis, setApis] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    loadApis();
  }, []);

  const loadApis = async () => {
    try {
      const res = await api.getApis(0, 50);
      const data = res.data || res;
      setApis(data.content || data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (e, id) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to delete this API?')) return;
    try {
      await api.deleteApi(id);
      addToast('API deleted', 'success');
      loadApis();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const handleCheck = async (e, id) => {
    e.stopPropagation();
    try {
      await api.triggerCheck(id);
      addToast('Health check triggered', 'success');
      setTimeout(loadApis, 2000);
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const timeAgo = (iso) => {
    if (!iso) return 'Never';
    const diff = Date.now() - new Date(iso).getTime();
    if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`;
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return `${Math.floor(diff / 86400000)}d ago`;
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner" />
        <span className="loading-text">Loading APIs...</span>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 className="page-title">Monitored APIs</h1>
          <p className="page-subtitle">Manage and monitor your API endpoints</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Add API</button>
      </div>

      {apis.length === 0 ? (
        <div className="glass-card">
          <div className="empty-state">
            <div className="empty-state-icon">🔗</div>
            <h3 className="empty-state-title">No APIs registered yet</h3>
            <p className="empty-state-desc">Register your first API endpoint to begin monitoring its health, latency, and availability.</p>
            <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Register First API</button>
          </div>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 12 }}>
          {apis.map((a) => (
            <div key={a.id} className="glass-card" style={{ padding: '16px 20px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 16 }}
              onClick={() => navigate(`/apis/${a.id}`)}
            >
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
                  <span style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)' }}>{a.name}</span>
                  <StatusBadge status={a.lastStatus} />
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16, fontSize: 12, color: 'var(--text-tertiary)' }}>
                  <span>{a.httpMethod} {a.url}</span>
                  <span>•</span>
                  <span>Every {a.checkIntervalSeconds}s</span>
                  <span>•</span>
                  <span>Checked {timeAgo(a.lastCheckedAt)}</span>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                <button className="btn btn-ghost btn-sm" onClick={(e) => handleCheck(e, a.id)} title="Trigger check">▶</button>
                <button className="btn btn-danger btn-sm" onClick={(e) => handleDelete(e, a.id)} title="Delete">🗑</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && <AddApiModal onClose={() => setShowModal(false)} onCreated={loadApis} />}
    </div>
  );
}
