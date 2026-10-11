import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';
import { useToast } from '../context/ToastContext';

export default function CachePage() {
  const { addToast } = useToast();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [clearing, setClearing] = useState(false);
  const [benchmarking, setBenchmarking] = useState(false);
  const [benchmarkResult, setBenchmarkResult] = useState(null);
  const [iterations, setIterations] = useState(100);

  const fetchStats = useCallback(async () => {
    try {
      const res = await api.getCacheStats();
      const data = res.data || res;
      setStats(data);
    } catch (err) {
      addToast(err.message || 'Failed to fetch cache stats', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  const handleClearCache = async () => {
    if (!window.confirm('Are you sure you want to purge all in-memory and Redis caches?')) {
      return;
    }
    setClearing(true);
    try {
      await api.clearCache();
      addToast('All caches cleared successfully', 'success');
      await fetchStats();
    } catch (err) {
      addToast(err.message || 'Failed to clear cache', 'error');
    } finally {
      setClearing(false);
    }
  };

  const handleRunBenchmark = async () => {
    setBenchmarking(true);
    try {
      const res = await api.runBenchmark(parseInt(iterations) || 100);
      const data = res.data || res;
      setBenchmarkResult(data);
      addToast('Cache latency benchmark completed', 'success');
    } catch (err) {
      addToast(err.message || 'Benchmark run failed', 'error');
    } finally {
      setBenchmarking(false);
    }
  };

  const hitRate = stats ? (stats.inMemoryHitRate != null ? (stats.inMemoryHitRate * 100).toFixed(1) : '0.0') : '0.0';

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner" />
        <span className="loading-text">Loading cache statistics...</span>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title">Cache & Performance Hub</h1>
          <p className="page-subtitle">Multi-tier in-memory & Redis caching telemetry, hit ratio & benchmarking</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-secondary" onClick={fetchStats} title="Refresh stats">
            🔄 Refresh
          </button>
          <button className="btn btn-danger" onClick={handleClearCache} disabled={clearing}>
            {clearing ? 'Clearing...' : '🗑 Clear Cache'}
          </button>
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="stats-grid">
        <div className="glass-card stat-card blue">
          <div className="stat-card-header">
            <span className="stat-card-label">Hit Rate</span>
            <div className="stat-card-icon">🎯</div>
          </div>
          <div className="stat-card-value">{hitRate}%</div>
          <div className="stat-card-detail">
            {stats?.inMemoryHitCount || 0} hits vs {stats?.inMemoryMissCount || 0} misses
          </div>
        </div>

        <div className="glass-card stat-card green">
          <div className="stat-card-header">
            <span className="stat-card-label">Cache Entries</span>
            <div className="stat-card-icon">💾</div>
          </div>
          <div className="stat-card-value">{stats?.inMemorySize || 0}</div>
          <div className="stat-card-detail">Active items in-memory</div>
        </div>

        <div className="glass-card stat-card purple">
          <div className="stat-card-header">
            <span className="stat-card-label">Evictions</span>
            <div className="stat-card-icon">⚡</div>
          </div>
          <div className="stat-card-value">{stats?.inMemoryEvictionCount || 0}</div>
          <div className="stat-card-detail">Total pruned cache keys</div>
        </div>

        <div className="glass-card stat-card">
          <div className="stat-card-header">
            <span className="stat-card-label">Redis Status</span>
            <div className="stat-card-icon">
              {stats?.redisConnected ? '🟢' : '⚪'}
            </div>
          </div>
          <div
            className="stat-card-value"
            style={{ fontSize: 22, color: stats?.redisConnected ? 'var(--status-up)' : 'var(--text-tertiary)' }}
          >
            {stats?.redisConnected ? 'Connected' : 'Standalone'}
          </div>
          <div className="stat-card-detail">
            {stats?.redisStatus || (stats?.redisConnected ? 'Distributed Cluster OK' : 'Local Caffeine Active')}
          </div>
        </div>
      </div>

      {/* Hit / Miss Ratio Progress Visualizer */}
      <div className="glass-card" style={{ padding: '24px', marginBottom: 28 }}>
        <h3 style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 12 }}>
          Cache Efficiency Ratio
        </h3>
        <p style={{ fontSize: 13, color: 'var(--text-tertiary)', marginBottom: 16 }}>
          Visual ratio of requests served directly from high-speed memory cache versus cache misses.
        </p>

        <div
          style={{
            height: 14,
            background: 'rgba(255, 255, 255, 0.05)',
            borderRadius: 7,
            overflow: 'hidden',
            display: 'flex',
            marginBottom: 12,
            border: '1px solid var(--glass-border)',
          }}
        >
          <div
            style={{
              width: `${Math.min(100, Math.max(0, parseFloat(hitRate)))}%`,
              background: 'var(--gradient-success)',
              transition: 'width 0.5s ease',
            }}
          />
          <div
            style={{
              width: `${100 - Math.min(100, Math.max(0, parseFloat(hitRate)))}%`,
              background: 'rgba(239, 68, 68, 0.3)',
              transition: 'width 0.5s ease',
            }}
          />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, color: 'var(--text-secondary)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 10, height: 10, borderRadius: '50%', background: 'var(--status-up)' }} />
            <span>Hits: {stats?.inMemoryHitCount || 0} ({hitRate}%)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 10, height: 10, borderRadius: '50%', background: 'var(--status-down)' }} />
            <span>Misses: {stats?.inMemoryMissCount || 0}</span>
          </div>
        </div>
      </div>

      {/* Latency Benchmark Suite */}
      <div className="glass-card" style={{ padding: '28px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16, marginBottom: 20 }}>
          <div>
            <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)' }}>
              ⚡ Latency Benchmark Suite
            </h2>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
              Simulate high-throughput reads to measure latency differences across uncached, in-memory (Caffeine), and Redis tiers.
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <label style={{ fontSize: 13, color: 'var(--text-tertiary)' }}>Iterations:</label>
            <select
              className="form-select"
              value={iterations}
              onChange={(e) => setIterations(Number(e.target.value))}
              style={{ width: 'auto', padding: '8px 12px' }}
            >
              <option value={50}>50 iterations</option>
              <option value={100}>100 iterations</option>
              <option value={250}>250 iterations</option>
              <option value={500}>500 iterations</option>
            </select>
            <button className="btn btn-primary" onClick={handleRunBenchmark} disabled={benchmarking}>
              {benchmarking ? 'Benchmarking...' : 'Run Benchmark'}
            </button>
          </div>
        </div>

        {benchmarkResult && (
          <div style={{ marginTop: 20 }}>
            {/* Speedup Highlight */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: 16,
                marginBottom: 24,
              }}
            >
              <div
                style={{
                  background: 'rgba(239, 68, 68, 0.08)',
                  border: '1px solid rgba(239, 68, 68, 0.2)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px',
                }}
              >
                <div style={{ fontSize: 12, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Uncached Latency</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--status-down)', marginTop: 4 }}>
                  {benchmarkResult.uncachedAvgMicroseconds?.toFixed(1)} µs
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-tertiary)', marginTop: 2 }}>Direct DB / target query</div>
              </div>

              <div
                style={{
                  background: 'rgba(59, 130, 246, 0.08)',
                  border: '1px solid rgba(59, 130, 246, 0.2)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px',
                }}
              >
                <div style={{ fontSize: 12, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>In-Memory Latency</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--accent-blue)', marginTop: 4 }}>
                  {benchmarkResult.inMemoryAvgMicroseconds?.toFixed(1)} µs
                </div>
                <div style={{ fontSize: 11, color: 'var(--accent-blue)', fontWeight: 600, marginTop: 2 }}>
                  ⚡ {benchmarkResult.inMemorySpeedupMultiplier?.toFixed(1)}x faster
                </div>
              </div>

              <div
                style={{
                  background: 'rgba(16, 185, 129, 0.08)',
                  border: '1px solid rgba(16, 185, 129, 0.2)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px',
                }}
              >
                <div style={{ fontSize: 12, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Redis Latency</div>
                <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--status-up)', marginTop: 4 }}>
                  {benchmarkResult.redisAvgMicroseconds != null
                    ? `${benchmarkResult.redisAvgMicroseconds.toFixed(1)} µs`
                    : 'N/A'}
                </div>
                <div style={{ fontSize: 11, color: 'var(--status-up)', fontWeight: 600, marginTop: 2 }}>
                  {benchmarkResult.redisSpeedupMultiplier != null
                    ? `⚡ ${benchmarkResult.redisSpeedupMultiplier.toFixed(1)}x faster`
                    : 'Redis offline / bypassed'}
                </div>
              </div>
            </div>

            {/* Analysis Note */}
            {benchmarkResult.analysis && (
              <div
                style={{
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px 20px',
                  fontSize: 13,
                  lineHeight: 1.6,
                  color: 'var(--text-secondary)',
                }}
              >
                <strong style={{ color: 'var(--text-primary)' }}>System Telemetry Analysis: </strong>
                {benchmarkResult.analysis}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
