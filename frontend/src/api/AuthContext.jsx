import { createContext, useContext, useState } from 'react';
import { api } from './client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('ms_user');
    return raw ? JSON.parse(raw) : null;
  });

  const login = async (username, password) => {
    const res = await api.login(username, password);
    const { token, ...userInfo } = res.data;
    localStorage.setItem('ms_token', token);
    localStorage.setItem('ms_user', JSON.stringify(userInfo));
    setUser(userInfo);
    return userInfo;
  };

  const logout = () => {
    localStorage.removeItem('ms_token');
    localStorage.removeItem('ms_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
