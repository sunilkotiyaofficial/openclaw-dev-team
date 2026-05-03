import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api } from '@/lib/api';
import type { UserInfo, Role, LoginRequest, RegisterRequest } from '@/types';

/**
 * Authentication state context.
 *
 * Manages:
 *  - Current user info
 *  - Login/Register/Logout actions
 *  - Role checks (hasRole, hasAnyRole)
 *
 * On app start, attempts to load existing session from sessionStorage.
 */

interface AuthContextValue {
  user: UserInfo | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (req: LoginRequest) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  logout: () => void;
  hasRole: (role: Role) => boolean;
  hasAnyRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const USER_STORAGE_KEY = 'knowledge_hub_user';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // On mount: try to restore user from sessionStorage if access token exists
  useEffect(() => {
    const token = api.getAccessToken();
    const storedUser = sessionStorage.getItem(USER_STORAGE_KEY);
    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch {
        sessionStorage.removeItem(USER_STORAGE_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  const login = async (req: LoginRequest) => {
    const response = await api.auth.login(req);
    api.setAccessToken(response.accessToken);
    setUser(response.user);
    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response.user));
  };

  const register = async (req: RegisterRequest) => {
    const response = await api.auth.register(req);
    api.setAccessToken(response.accessToken);
    setUser(response.user);
    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response.user));
  };

  const logout = () => {
    api.setAccessToken(null);
    sessionStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  };

  const hasRole = (role: Role) => user?.roles.includes(role) ?? false;
  const hasAnyRole = (...roles: Role[]) =>
    roles.some(r => user?.roles.includes(r) ?? false);

  const value: AuthContextValue = {
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    register,
    logout,
    hasRole,
    hasAnyRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
