import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid
} from 'recharts';
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

function ImpactBadge({ impact }) {
  const imp = (impact || 'low').toLowerCase();
  const colors = {
    high: { bg: 'rgba(239, 68, 68, 0.15)', text: '#ef4444' },
    medium: { bg: 'rgba(245, 158, 11, 0.15)', text: '#f59e0b' },
    low: { bg: 'rgba(59, 130, 246, 0.15)', text: '#3b82f6' },
  };
  const c = colors[imp] || colors.low;
  return (
    <span
      style={{
        padding: '2px 8px',
        borderRadius: 9999,
        fontSize: 10,
        fontWeight: 700,
        textTransform: 'uppercase',
        background: c.bg,
        color: c.text,
      }}
    >
      {impact} Impact
    </span>
  );
}

export default function ApiDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [apiData, setApiData] = useState(null);
  const [trends, setTrends] = useState([]);
  const [checks, setChecks] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);
  const [windowHours, setWindowHours] = useState(24);

  const loadAllData = useCallback(async () => {
    try {
      const [apiRes, trendsRes, checksRes, recsRes] = await Promise.all([
        api.getApiById(id).catch(() => null),
        api.getLatencyTrends(id, windowHours).catch(() => null),
        api.getCheckHistory(id, 0, 30).catch(() => null),
        api.getRecommendations(id).catch(() => null),
      ]);

      if (apiRes) setApiData(apiRes.data || apiRes);
      if (trendsRes) {
        const trendList = trendsRes.data || trendsRes || [];
        setTrends(
          trendList.map((t) => ({
            ...t,
            formattedTime: t.timestamp ? new Date(t.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
          }))
        );
      }
      if (checksRes) {
        const checkList = checksRes.data?.content || checksRes.data || checksRes.content || checksRes || [];
        setChecks(Array.isArray(checkList) ? checkList : []);
      }
      if (recsRes) {
        const recList = recsRes.data || recsRes || [];
        setRecommendations(Array.isArray(recList) ? recList : []);
      }
    } catch (err) {
      addToast(err.message || 'Failed to load API details', 'error');
    } finally {
      setLoading(false);
    }
  }, [id, windowHours, addToast]);

  useEffect(() => {
    loadAllData();
  }, [loadAllData]);

  const handleTriggerCheck = async () => {
    setChecking(true);
    try {
      const res = await api.triggerCheck(id);
      addToast('Health check triggered successfully', 'success');
      await loadAllData();
    } catch (err) {
      addToast(err.message || 'Health check failed', 'error');
    } finally {
      setChecking(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner" />
        <span className="loading-text">Loading API Telemetry...</span>
      </div>
    );
  }

  if (!apiData) {
    return (
      <div className="glass-card">
        <div className="empty-state">
          <div className="empty-state-icon">⚠</div>
          <h3 className="empty-state-title">API not found</h3>
          <p className="empty-state-desc">The requested API could not be retrieved.</p>
          <button className="btn btn-primary" onClick={() => navigate('/apis')}>
            ← Back to APIs
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Navigation & Header */}
      <div style={{ marginBottom: 20 }}>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate('/apis')}
          style={{ marginBottom: 12, paddingLeft: 0 }}
        >
          ← Back to Monitored APIs
        </button>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 6 }}>
              <h1 className="page-title" style={{ margin: 0 }}>{apiData.name}</h1>
              <StatusBadge status={apiData.lastStatus} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, fontSize: 13, color: 'var(--text-tertiary)' }}>
              <span style={{ padding: '2px 8px', borderRadius: 4, background: 'rgba(255,255,255,0.08)', fontWeight: 600, color: 'var(--text-primary)' }}>
                {apiData.httpMethod}
              </span>
              <a href={apiData.url} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent-blue)', textDecoration: 'underline' }}>
                {apiData.url}
              </a>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            <button className="btn btn-secondary" onClick={loadAllData} title="Refresh">
              🔄 Refresh
            </button>
            <button className="btn btn-primary" onClick={handleTriggerCheck} disabled={checking}>
              {checking ? 'Checking...' : '▶ Run Health Check'}
            </button>
          </div>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="stats-grid" style={{ marginBottom: 24 }}>
        <div className="glass-card stat-card blue">
          <div className="stat-card-header">
            <span className="stat-card-label">Latency Threshold</span>
            <div className="stat-card-icon">⚡</div>
          </div>
          <div className="stat-card-value">{apiData.latencyThresholdMillis} ms</div>
          <div className="stat-card-detail">SLA warning cutoff</div>
        </div>

        <div className="glass-card stat-card green">
          <div className="stat-card-header">
            <span className="stat-card-label">Check Interval</span>
            <div className="stat-card-icon">⏱</div>
          </div>
          <div className="stat-card-value">{apiData.checkIntervalSeconds}s</div>
          <div className="stat-card-detail">Automated polling cycle</div>
        </div>

        <div className="glass-card stat-card purple">
          <div className="stat-card-header">
            <span className="stat-card-label">Expected Status</span>
            <div className="stat-card-icon">🎯</div>
          </div>
          <div className="stat-card-value">{apiData.expectedStatusCodes || '200'}</div>
          <div className="stat-card-detail">Valid HTTP response code</div>
        </div>

        <div className="glass-card stat-card">
          <div className="stat-card-header">
            <span className="stat-card-label">Timeout Limit</span>
            <div className="stat-card-icon">🛡</div>
          </div>
          <div className="stat-card-value">{apiData.timeoutMillis} ms</div>
          <div className="stat-card-detail">Socket / connection timeout</div>
        </div>
      </div>

      {/* Latency Response Trend Chart */}
      <div className="glass-card" style={{ padding: 24, marginBottom: 28 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <div>
            <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)' }}>
              Response Time & P95 Trends
            </h2>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
              Hourly latency measurements over the selected monitoring window
            </p>
          </div>

          <div style={{ display: 'flex', gap: 6 }}>
            {[6, 12, 24, 72].map((hours) => (
              <button
                key={hours}
                className="btn btn-sm"
                onClick={() => setWindowHours(hours)}
                style={{
                  background: windowHours === hours ? 'var(--gradient-primary)' : 'var(--glass-bg)',
                  color: windowHours === hours ? '#fff' : 'var(--text-secondary)',
                  border: `1px solid ${windowHours === hours ? 'transparent' : 'var(--glass-border)'}`,
                }}
              >
                {hours}h
              </button>
            ))}
          </div>
        </div>

        {trends.length === 0 ? (
          <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-tertiary)', fontSize: 14 }}>
            No latency trends recorded in the selected window. Run a check or allow background checks to collect telemetry.
          </div>
        ) : (
          <div style={{ width: '100%', height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trends} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="latencyGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="p95Grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                <XAxis dataKey="formattedTime" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" tick={{ fontSize: 11 }} unit="ms" />
                <Tooltip
                  contentStyle={{
                    background: 'rgba(17, 24, 39, 0.95)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    borderRadius: 8,
                    boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
                  }}
                  itemStyle={{ fontSize: 12, color: '#f0f4f8' }}
                />
                <Area type="monotone" dataKey="averageLatencyMs" name="Avg Latency (ms)" stroke="#3b82f6" strokeWidth={2} fillOpacity={1} fill="url(#latencyGrad)" />
                <Area type="monotone" dataKey="p95LatencyMs" name="P95 Latency (ms)" stroke="#8b5cf6" strokeWidth={2} strokeDasharray="3 3" fillOpacity={1} fill="url(#p95Grad)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      {/* AI Recommendations Section */}
      <div className="glass-card" style={{ padding: 24, marginBottom: 28 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 6 }}>
          💡 Optimization Recommendations
        </h2>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 18 }}>
          Algorithmic heuristics based on historical error rate, standard deviation, and response time distributions
        </p>

        {recommendations.length === 0 ? (
          <div style={{ color: 'var(--text-tertiary)', fontSize: 13 }}>
            No recommendations generated. Telemetry indicates endpoint is optimal.
          </div>
        ) : (
          <div style={{ display: 'grid', gap: 14 }}>
            {recommendations.map((rec, idx) => (
              <div
                key={idx}
                style={{
                  background: 'rgba(255, 255, 255, 0.02)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: 'var(--radius-md)',
                  padding: 16,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8, flexWrap: 'wrap', gap: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent-blue)', background: 'rgba(59,130,246,0.1)', padding: '2px 8px', borderRadius: 4 }}>
                      {rec.category}
                    </span>
                    <strong style={{ fontSize: 15, color: 'var(--text-primary)' }}>{rec.title}</strong>
                  </div>
                  <ImpactBadge impact={rec.impact} />
                </div>
                <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 10 }}>
                  {rec.description}
                </p>
                {rec.actionItems && rec.actionItems.length > 0 && (
                  <div style={{ marginTop: 8, paddingLeft: 12, borderLeft: '2px solid rgba(59,130,246,0.4)' }}>
                    <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', marginBottom: 4 }}>
                      Recommended Action Items:
                    </div>
                    <ul style={{ margin: 0, paddingLeft: 16, fontSize: 12, color: 'var(--text-secondary)' }}>
                      {rec.actionItems.map((item, i) => (
                        <li key={i} style={{ marginBottom: 4 }}>• {item}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Health Check Audit Logs Table */}
      <div className="glass-card" style={{ padding: 24 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 6 }}>
          📜 Recent Health Check Audit Logs
        </h2>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 18 }}>
          Detailed chronological logs of executed HTTP health checks
        </p>

        {checks.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--text-tertiary)', fontSize: 14 }}>
            No check logs found. Click "Run Health Check" to execute an immediate probe.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Status</th>
                  <th>HTTP Code</th>
                  <th>Response Time</th>
                  <th>Message / Error</th>
                </tr>
              </thead>
              <tbody>
                {checks.map((chk) => (
                  <tr key={chk.id}>
                    <td>{chk.checkedAt ? new Date(chk.checkedAt).toLocaleString() : '-'}</td>
                    <td>
                      <StatusBadge status={chk.status} />
                    </td>
                    <td className="primary">
                      {chk.httpStatusCode || '-'}
                    </td>
                    <td>
                      <span
                        style={{
                          fontWeight: 600,
                          color:
                            chk.responseTimeMillis > (apiData.latencyThresholdMillis || 500)
                              ? 'var(--status-slow)'
                              : 'var(--status-up)',
                        }}
                      >
                        {chk.responseTimeMillis != null ? `${chk.responseTimeMillis} ms` : '-'}
                      </span>
                    </td>
                    <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {chk.errorMessage || (chk.errorType ? `${chk.errorType}` : 'OK')}
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
