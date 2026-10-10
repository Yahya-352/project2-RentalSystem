import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { setUnauthorizedHandler, tokenStore } from '../api/client';
import { usersApi } from '../api/endpoints';
import type { LoginRequest, Role } from '../api/types';

export interface SessionUser {
  email: string;
  role: Role;
  expiresAt: number | null;
}

interface AuthContextValue {
  user: SessionUser | null;
  token: string | null;
  login: (credentials: LoginRequest) => Promise<SessionUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const USER_KEY = 'rental.user';

/** Reads the `exp` claim. The token is only decoded, never trusted: the backend verifies it. */
function tokenExpiry(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

function restoreSession(): { token: string; user: SessionUser } | null {
  const token = tokenStore.get();
  if (!token) return null;
  try {
    const user = JSON.parse(localStorage.getItem(USER_KEY) ?? 'null') as SessionUser | null;
    if (!user || (user.expiresAt && user.expiresAt <= Date.now())) return null;
    return { token, user };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState(restoreSession);

  const logout = useCallback(() => {
    tokenStore.clear();
    try {
      localStorage.removeItem(USER_KEY);
    } catch {
      /* ignore */
    }
    setSession(null);
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    const res = await usersApi.login(credentials);
    const user: SessionUser = { email: res.email, role: res.role, expiresAt: tokenExpiry(res.token) };
    tokenStore.set(res.token);
    try {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch {
      /* ignore */
    }
    setSession({ token: res.token, user });
    return user;
  }, []);

  // Any request rejected for a bad/expired token logs the user out.
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  // Log out exactly when the JWT expires instead of waiting for a failing request.
  useEffect(() => {
    const expiresAt = session?.user.expiresAt;
    if (!expiresAt) return;
    const timer = window.setTimeout(logout, Math.max(0, expiresAt - Date.now()));
    return () => window.clearTimeout(timer);
  }, [session, logout]);

  const value = useMemo<AuthContextValue>(
    () => ({ user: session?.user ?? null, token: session?.token ?? null, login, logout }),
    [session, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

/** Where each role lands after login. */
export function homeFor(role: Role | undefined): string {
  switch (role) {
    case 'AGENCY':
      return '/agency/cars';
    case 'ADMIN':
      return '/admin/bookings';
    default:
      return '/cars';
  }
}
