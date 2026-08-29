/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Role } from '../types';
import { api, AuthResponse } from '../api/client';

interface AuthContextType {
  user: User | null;
  activeRole: Role;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => Promise<void>;
  setActiveRole: (role: Role) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = typeof window !== 'undefined' && typeof localStorage !== 'undefined' && localStorage.getItem
      ? localStorage.getItem('resolveiq_user')
      : null;
    return saved ? JSON.parse(saved) : null;
  });

  const [activeRole, setActiveRole] = useState<Role>(() => {
    const saved = typeof window !== 'undefined' && typeof localStorage !== 'undefined' && localStorage.getItem
      ? (localStorage.getItem('resolveiq_active_role') as Role)
      : null;
    return saved || 'CUSTOMER';
  });

  const [token, setToken] = useState<string | null>(() => {
    return api.getToken();
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    api.refresh()
      .then(handleAuthSuccess)
      .catch(() => {
        setUser(null);
        setToken(null);
      })
      .finally(() => setIsLoading(false));
  }, []);

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

  const logout = async () => {
    try {
      await api.logout();
    } finally {
      setToken(null);
      setUser(null);
      localStorage.removeItem('resolveiq_user');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        activeRole,
        token,
        isAuthenticated: !!token || !!user,
        isLoading,
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
