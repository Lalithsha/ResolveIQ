/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Role } from '../types';
import { api, AuthResponse } from '../api/client';

interface AuthContextType {
  user: User | null;
  activeRole: Role;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
  setActiveRole: (role: Role) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = typeof window !== 'undefined' && typeof localStorage !== 'undefined' && localStorage.getItem
      ? localStorage.getItem('resolveiq_user')
      : null;
    return saved ? JSON.parse(saved) : {
      id: '22222222-2222-2222-2222-222222222222',
      tenantId: '00000000-0000-0000-0000-000000000001',
      email: 'sarah.chen@resolveiq.local',
      fullName: 'Sarah Chen (Agent)',
      roles: ['AGENT']
    };
  });

  const [activeRole, setActiveRole] = useState<Role>(() => {
    const saved = typeof window !== 'undefined' && typeof localStorage !== 'undefined' && localStorage.getItem
      ? (localStorage.getItem('resolveiq_active_role') as Role)
      : null;
    return saved || 'AGENT';
  });

  const [token, setToken] = useState<string | null>(() => {
    return api.getToken();
  });

  useEffect(() => {
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined' && localStorage.setItem) {
      if (user) {
        localStorage.setItem('resolveiq_user', JSON.stringify(user));
      } else {
        localStorage.removeItem('resolveiq_user');
      }
    }
  }, [user]);

  useEffect(() => {
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined' && localStorage.setItem) {
      localStorage.setItem('resolveiq_active_role', activeRole);
    }
  }, [activeRole]);

  const handleAuthSuccess = (res: AuthResponse) => {
    const newUser: User = {
      id: res.userId,
      tenantId: res.tenantId,
      email: res.email,
      fullName: res.fullName,
      roles: res.roles,
    };
    setUser(newUser);
    setToken(res.accessToken);
    if (res.roles.length > 0) {
      setActiveRole(res.roles[0]);
    }
  };

  const login = async (email: string, password: string) => {
    const res = await api.login(email, password);
    handleAuthSuccess(res);
  };

  const register = async (email: string, password: string, fullName: string) => {
    const res = await api.register(email, password, fullName);
    handleAuthSuccess(res);
  };

  const logout = () => {
    api.setToken(null);
    setToken(null);
    setUser(null);
    localStorage.removeItem('resolveiq_user');
    localStorage.removeItem('resolveiq_token');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        activeRole,
        token,
        isAuthenticated: !!token || !!user,
        login,
        register,
        logout,
        setActiveRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
