import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from 'recharts';
import api from '../services/api';

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

function AvailabilityRing({ value }) {
  const radius = 58;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;
  const color = value >= 99 ? '#10b981' : value >= 95 ? '#f59e0b' : '#ef4444';

  return (
    <div className="availability-ring-container">
      <div className="availability-ring">
        <svg width="140" height="140" viewBox="0 0 140 140">
          <circle className="availability-ring-bg" cx="70" cy="70" r={radius} />
          <circle
            className="availability-ring-fill"
            cx="70" cy="70" r={radius}
            stroke={color}
            strokeDasharray={circumference}
            strokeDashoffset={offset}
          />
        </svg>
        <div className="availability-ring-text">
          <span className="availability-ring-value" style={{ color }}>{value.toFixed(1)}%</span>
          <span className="availability-ring-label">Uptime</span>
        </div>
      </div>
    </div>
  );
}

const customTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: '#1a2332', border: '1px solid rgba(255,255,255,0.1)',
      borderRadius: 8, padding: '10px 14px', fontSize: 12,
    }}>
      <div style={{ color: '#94a3b8', marginBottom: 4 }}>{label}</div>
      {payload.map((p, i) => (
        <div key={i} style={{ color: p.color, fontWeight: 600 }}>
          {p.name}: {typeof p.value === 'number' ? p.value.toFixed(1) : p.value}ms
        </div>
      ))}
    </div>
  );
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState(null);
  const [summaries, setSummaries] = useState([]);
  const [slowest, setSlowest] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [dashRes, sumRes, slowRes] = await Promise.all([
        api.getDashboard(),
        api.getApiSummaries(),
        api.getSlowestApis(5),
      ]);
      setDashboard(dashRes.data || dashRes);
      setSummaries((sumRes.data || sumRes) || []);
      setSlowest((slowRes.data || slowRes) || []);
    } catch (err) {
      console.error('Dashboard load error:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner" />
        <span className="loading-text">Loading dashboard...</span>
      </div>
    );
  }

  const d = dashboard || {};

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <p className="page-subtitle">Real-time health overview of your monitored APIs</p>
      </div>

      {/* Stat Cards */}
      <div className="stats-grid">
        <div className="stat-card glass-card blue">
          <div className="stat-card-header">
            <div className="stat-card-icon">🔗</div>
            <span className="stat-card-label">Total APIs</span>
          </div>
          <div className="stat-card-value">{d.totalApis || 0}</div>
          <div className="stat-card-detail">{d.activeApis || 0} active</div>
        </div>

        <div className="stat-card glass-card green">
          <div className="stat-card-header">
            <div className="stat-card-icon">✓</div>
            <span className="stat-card-label">Healthy</span>
          </div>
          <div className="stat-card-value">{d.apisUp || 0}</div>
          <div className="stat-card-detail">APIs responding normally</div>
        </div>

        <div className="stat-card glass-card red">
          <div className="stat-card-header">
            <div className="stat-card-icon">✕</div>
            <span className="stat-card-label">Down</span>
          </div>
          <div className="stat-card-value">{d.apisDown || 0}</div>
          <div className="stat-card-detail">{d.activeAlerts || 0} active alerts</div>
        </div>

        <div className="stat-card glass-card purple">
          <div className="stat-card-header">
            <div className="stat-card-icon">⏱</div>
            <span className="stat-card-label">Checks (24h)</span>
          </div>
          <div className="stat-card-value">{d.totalChecksLast24h || 0}</div>
          <div className="stat-card-detail">{d.apisSlow || 0} slow</div>
        </div>
      </div>

      <div className="content-grid">
        {/* Availability Ring */}
        <div className="glass-card section-card">
          <div className="section-card-header">
            <span className="section-card-title">Overall Availability</span>
          </div>
          <AvailabilityRing value={d.overallAvailability || 0} />
        </div>

        {/* Slowest APIs */}
        <div className="glass-card section-card">
          <div className="section-card-header">
            <span className="section-card-title">Slowest APIs</span>
          </div>
          <div className="section-card-body">
            {slowest.length === 0 ? (
              <div className="empty-state" style={{ padding: 20 }}>
                <p className="empty-state-desc">No latency data available yet</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {slowest.map((s, i) => (
                  <div
                    key={s.apiId}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 12,
                      padding: '10px 0',
                      borderBottom: i < slowest.length - 1 ? '1px solid rgba(255,255,255,0.04)' : 'none',
                      cursor: 'pointer',
                    }}
                    onClick={() => navigate(`/apis/${s.apiId}`)}
                  >
                    <span style={{
                      width: 24, height: 24, borderRadius: 6,
                      background: 'rgba(239,68,68,0.12)', color: '#ef4444',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 11, fontWeight: 700, flexShrink: 0,
                    }}>
                      {i + 1}
                    </span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {s.apiName || s.name}
                      </div>
                    </div>
                    <span style={{ fontSize: 14, fontWeight: 700, color: '#ef4444' }}>
                      {(s.averageLatencyMs || 0).toFixed(0)}ms
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* API Status Table */}
      <div className="glass-card section-card" style={{ marginTop: 20 }}>
        <div className="section-card-header">
          <span className="section-card-title">API Status Overview</span>
          <button className="btn btn-ghost btn-sm" onClick={() => navigate('/apis')}>View All →</button>
        </div>
        {summaries.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🔗</div>
            <h3 className="empty-state-title">No APIs registered</h3>
            <p className="empty-state-desc">Start by adding your first API endpoint to monitor.</p>
            <button className="btn btn-primary" onClick={() => navigate('/apis')}>+ Add API</button>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Status</th>
                  <th>Avg Latency</th>
                  <th>Availability</th>
                </tr>
              </thead>
              <tbody>
                {summaries.slice(0, 10).map((s) => (
                  <tr key={s.apiId} onClick={() => navigate(`/apis/${s.apiId}`)} style={{ cursor: 'pointer' }}>
                    <td className="primary">{s.apiName}</td>
                    <td><StatusBadge status={s.lastStatus} /></td>
                    <td>{(s.averageLatencyMs || 0).toFixed(0)} ms</td>
                    <td>
                      <span style={{ color: s.availabilityPercentage >= 99 ? '#10b981' : s.availabilityPercentage >= 95 ? '#f59e0b' : '#ef4444', fontWeight: 600 }}>
                        {(s.availabilityPercentage || 0).toFixed(1)}%
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
