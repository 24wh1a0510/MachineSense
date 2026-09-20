import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const client = axios.create({ baseURL: BASE_URL });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('ms_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('ms_token');
      localStorage.removeItem('ms_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default client;

export const api = {
  login: (username, password) => client.post('/api/auth/login', { username, password }),
  dashboardSummary: () => client.get('/api/dashboard/summary'),
  machines: () => client.get('/api/machines'),
  machine: (id) => client.get(`/api/machines/${id}`),
  registerMachine: (payload) => client.post('/api/machines/register', payload),
  sensorHistory: (machineId, limit = 40) => client.get(`/api/sensors/machine/${machineId}?limit=${limit}`),
  predictionHistory: (machineId, limit = 40) => client.get(`/api/predictions/machine/${machineId}?limit=${limit}`),
  latestPrediction: (machineId) => client.get(`/api/predictions/machine/${machineId}/latest`),
  tickets: () => client.get('/api/tickets'),
  ticketsByMachine: (machineId) => client.get(`/api/tickets/machine/${machineId}`),
  updateTicket: (ticketId, payload) => client.patch(`/api/tickets/${ticketId}`, payload),
  ticketHistory: (ticketId) => client.get(`/api/tickets/${ticketId}/history`),
};
