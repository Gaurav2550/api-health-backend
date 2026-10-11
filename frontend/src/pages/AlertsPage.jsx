import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import { useToast } from '../context/ToastContext';

function SeverityBadge({ severity }) {
  const sev = (severity || 'low').toLowerCase();
  return (
    <span className={`severity-badge ${sev}`}>
      {sev}
    </span>
  );
}

function StatusTag({ status }) {
  const s = (status || '').toLowerCase();
  const styles = {
    active: { bg: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', border: 'rgba(239, 68, 68, 0.3)' },
    acknowledged: { bg: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b', border: 'rgba(245, 158, 11, 0.3)' },
    resolved: { bg: 'rgba(16, 185, 129, 0.15)', color: '#10b981', border: 'rgba(16, 185, 129, 0.3)' },
  };
  const current = styles[s] || { bg: 'rgba(148, 163, 184, 0.15)', color: '#94a3b8', border: 'rgba(148, 163, 184, 0.3)' };

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        padding: '3px 10px',
        borderRadius: '9999px',
        fontSize: '11px',
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.04em',
        background: current.bg,
        color: current.color,
        border: `1px solid ${current.border}`,
      }}
    >
      <span
        style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          backgroundColor: current.color,
        }}
      />
      {status}
    </span>
  );
}

export default function AlertsPage() {
  const { addToast } = useToast();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [severityFilter, setSeverityFilter] = useState('ALL');
  const [actionInProgress, setActionInProgress] = useState(null);

  const fetchAlerts = useCallback(async () => {
    try {
      const res = await api.getAlerts(0, 100);
      const data = res.data?.content || res.data || res.content || res || [];
      setAlerts(Array.isArray(data) ? data : []);
    } catch (err) {
      addToast(err.message || 'Failed to load alerts', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  const handleAcknowledge = async (id) => {
    setActionInProgress(id);
    try {
      await api.acknowledgeAlert(id);
      addToast('Alert marked as acknowledged', 'success');
      await fetchAlerts();
    } catch (err) {
      addToast(err.message || 'Failed to acknowledge alert', 'error');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleResolve = async (id) => {
    setActionInProgress(id);
    try {
      await api.resolveAlert(id);
      addToast('Alert resolved successfully', 'success');
      await fetchAlerts();
    } catch (err) {
      addToast(err.message || 'Failed to resolve alert', 'error');
    } finally {
      setActionInProgress(null);
    }
  };

  const filteredAlerts = alerts.filter((alert) => {
    if (statusFilter !== 'ALL' && alert.status !== statusFilter) return false;
    if (severityFilter !== 'ALL' && alert.severity !== severityFilter) return false;
    return true;
  });

  const activeCount = alerts.filter((a) => a.status === 'ACTIVE').length;
  const acknowledgedCount = alerts.filter((a) => a.status === 'ACKNOWLEDGED').length;
  const resolvedCount = alerts.filter((a) => a.status === 'RESOLVED').length;

  const formatDate = (iso) => {
    if (!iso) return '-';
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title">System Alerts</h1>
          <p className="page-subtitle">Real-time incident response and automated alert management</p>
        </div>
        <button className="btn btn-secondary" onClick={() => { setLoading(true); fetchAlerts(); }}>
          <span>🔄</span> Refresh Alerts
        </button>
      </div>

      {/* Summary KPI Cards */}
      <div className="stats-grid">
        <div className="glass-card stat-card blue">
          <div className="stat-card-header">
            <span className="stat-card-label">Total Alerts</span>
            <div className="stat-card-icon">🔔</div>
          </div>
          <div className="stat-card-value">{alerts.length}</div>
          <div className="stat-card-detail">All logged events</div>
        </div>

        <div className="glass-card stat-card red">
          <div className="stat-card-header">
            <span className="stat-card-label">Active Incidents</span>
            <div className="stat-card-icon">🚨</div>
          </div>
          <div className="stat-card-value">{activeCount}</div>
          <div className="stat-card-detail">Require immediate attention</div>
        </div>

        <div className="glass-card stat-card purple">
          <div className="stat-card-header">
            <span className="stat-card-label">Acknowledged</span>
            <div className="stat-card-icon">⏳</div>
          </div>
          <div className="stat-card-value">{acknowledgedCount}</div>
          <div className="stat-card-detail">Under investigation</div>
        </div>

        <div className="glass-card stat-card green">
          <div className="stat-card-header">
            <span className="stat-card-label">Resolved</span>
            <div className="stat-card-icon">✅</div>
          </div>
          <div className="stat-card-value">{resolvedCount}</div>
          <div className="stat-card-detail">Mitigated and closed</div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div
        className="glass-card"
        style={{
          padding: '16px 20px',
          marginBottom: 20,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <span style={{ fontSize: 13, color: 'var(--text-tertiary)', marginRight: 4 }}>Status:</span>
          {['ALL', 'ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className="btn btn-sm"
              style={{
                background: statusFilter === status ? 'var(--gradient-primary)' : 'var(--glass-bg)',
                color: statusFilter === status ? '#ffffff' : 'var(--text-secondary)',
                border: `1px solid ${statusFilter === status ? 'transparent' : 'var(--glass-border)'}`,
              }}
            >
              {status}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 13, color: 'var(--text-tertiary)' }}>Severity:</span>
          <select
            className="form-select"
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            style={{ width: 'auto', padding: '6px 12px', fontSize: 13 }}
          >
            <option value="ALL">All Severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>
      </div>

      {/* Alert Feed */}
      {loading ? (
        <div className="loading-container">
          <div className="spinner" />
          <span className="loading-text">Loading alerts...</span>
        </div>
      ) : filteredAlerts.length === 0 ? (
        <div className="glass-card">
          <div className="empty-state">
            <div className="empty-state-icon">🛡️</div>
            <h3 className="empty-state-title">No alerts found</h3>
            <p className="empty-state-desc">
              {statusFilter !== 'ALL' || severityFilter !== 'ALL'
                ? 'No alerts match the chosen filter criteria.'
                : 'All monitored APIs are operating normally. No alerts have been triggered.'}
            </p>
          </div>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 14 }}>
          {filteredAlerts.map((alert) => (
            <div
              key={alert.id}
              className="glass-card"
              style={{
                padding: '20px 24px',
                borderLeft: `4px solid ${
                  alert.severity === 'CRITICAL'
                    ? 'var(--severity-critical)'
                    : alert.severity === 'HIGH'
                    ? 'var(--severity-high)'
                    : alert.severity === 'MEDIUM'
                    ? 'var(--severity-medium)'
                    : 'var(--severity-low)'
                }`,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12, marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                  <SeverityBadge severity={alert.severity} />
                  <StatusTag status={alert.status} />
                  <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)' }}>
                    Type: {alert.alertType || 'HEALTH_FAILURE'}
                  </span>
                </div>
                <div style={{ fontSize: 12, color: 'var(--text-tertiary)' }}>
                  {formatDate(alert.createdAt)}
                </div>
              </div>

              <div style={{ marginBottom: 14 }}>
                <div style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 6 }}>
                  {alert.apiName ? (
                    <span>
                      {alert.apiName} <span style={{ fontSize: 13, fontWeight: 400, color: 'var(--text-tertiary)' }}>({alert.apiUrl})</span>
                    </span>
                  ) : (
                    'System Alert'
                  )}
                </div>
                <div
                  style={{
                    fontSize: 14,
                    color: 'var(--text-secondary)',
                    background: 'rgba(0, 0, 0, 0.25)',
                    padding: '10px 14px',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--glass-border)',
                    fontFamily: 'monospace',
                  }}
                >
                  {alert.message}
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
                {alert.status === 'ACTIVE' && (
                  <button
                    className="btn btn-secondary btn-sm"
                    disabled={actionInProgress === alert.id}
                    onClick={() => handleAcknowledge(alert.id)}
                  >
                    {actionInProgress === alert.id ? 'Saving...' : 'Acknowledge'}
                  </button>
                )}
                {alert.status !== 'RESOLVED' && (
                  <button
                    className="btn btn-primary btn-sm"
                    disabled={actionInProgress === alert.id}
                    onClick={() => handleResolve(alert.id)}
                  >
                    {actionInProgress === alert.id ? 'Resolving...' : 'Mark as Resolved'}
                  </button>
                )}
                {alert.status === 'RESOLVED' && (
                  <span style={{ fontSize: 12, color: 'var(--status-up)', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    ✓ Resolved at {formatDate(alert.resolvedAt)}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
