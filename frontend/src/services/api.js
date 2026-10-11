const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiService {
  constructor() {
    this.baseUrl = BASE_URL;
  }

  getToken() {
    return localStorage.getItem('token');
  }

  async request(path, options = {}) {
    const token = this.getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    };

    const res = await fetch(`${this.baseUrl}${path}`, {
      ...options,
      headers,
    });

    if (res.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
      throw new Error('Session expired');
    }

    const data = await res.json().catch(() => null);

    if (!res.ok) {
      const msg = data?.message || data?.error || `Request failed (${res.status})`;
      throw new Error(msg);
    }

    return data;
  }

  // ─── Auth ───
  async register(name, email, password) {
    return this.request('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password }),
    });
  }

  async login(email, password) {
    return this.request('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  async getMe() {
    return this.request('/api/v1/auth/me');
  }

  // ─── Monitored APIs ───
  async getApis(page = 0, size = 20) {
    return this.request(`/api/v1/apis?page=${page}&size=${size}`);
  }

  async getApiById(id) {
    return this.request(`/api/v1/apis/${id}`);
  }

  async createApi(payload) {
    return this.request('/api/v1/apis', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  }

  async updateApi(id, payload) {
    return this.request(`/api/v1/apis/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  }

  async deleteApi(id) {
    return this.request(`/api/v1/apis/${id}`, { method: 'DELETE' });
  }

  async triggerCheck(id) {
    return this.request(`/api/v1/apis/${id}/check`, { method: 'POST' });
  }

  async getCheckHistory(id, page = 0, size = 50) {
    return this.request(`/api/v1/apis/${id}/checks?page=${page}&size=${size}`);
  }

  async getRecommendations(id) {
    return this.request(`/api/v1/apis/${id}/recommendations`);
  }

  // ─── Metrics ───
  async getDashboard() {
    return this.request('/api/v1/metrics/dashboard');
  }

  async getApiMetrics(id, windowHours = 24) {
    return this.request(`/api/v1/metrics/apis/${id}?windowHours=${windowHours}`);
  }

  async getLatencyTrends(id, windowHours = 24) {
    return this.request(`/api/v1/metrics/apis/${id}/trends?windowHours=${windowHours}`);
  }

  async getApiSummaries() {
    return this.request('/api/v1/metrics/apis/summaries');
  }

  async getSlowestApis(limit = 5) {
    return this.request(`/api/v1/metrics/apis/slowest?limit=${limit}`);
  }

  // ─── Alerts ───
  async getAlerts(page = 0, size = 20) {
    return this.request(`/api/v1/alerts?page=${page}&size=${size}`);
  }

  async acknowledgeAlert(id) {
    return this.request(`/api/v1/alerts/${id}/acknowledge`, { method: 'POST' });
  }

  async resolveAlert(id) {
    return this.request(`/api/v1/alerts/${id}/resolve`, { method: 'POST' });
  }

  // ─── Cache ───
  async getCacheStats() {
    return this.request('/api/v1/cache/stats');
  }

  async clearCache() {
    return this.request('/api/v1/cache/clear', { method: 'POST' });
  }

  async runBenchmark(iterations = 100) {
    return this.request(`/api/v1/cache/benchmark?iterations=${iterations}`);
  }
}

export default new ApiService();
