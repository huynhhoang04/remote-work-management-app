import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { setSessionExpiredHandler } from '../api/client';
import { clearTokens, clearUser, loadTokens, loadUser, saveUser, setTokens } from '../api/tokenStore';
import type { TokenResponse, UserDto } from '../types/api';

interface SessionContextValue {
  user: UserDto | null;
  isReady: boolean;
  isAuthenticated: boolean;
  signIn: (tokens: TokenResponse) => Promise<void>;
  signOut: () => Promise<void>;
  updateUser: (user: UserDto) => Promise<void>;
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

/**
 * Phiên đăng nhập: token lưu SecureStore, user cache để mở app là có ngay.
 * @reference docs/plan-app/01-phase-1.md §6
 */
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    let mounted = true;

    (async () => {
      await loadTokens();
      const rawUser = await loadUser();
      if (mounted && rawUser) {
        try {
          setUser(JSON.parse(rawUser) as UserDto);
        } catch {
          setUser(null);
        }
      }
      if (mounted) setIsReady(true);
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const signOut = useCallback(async () => {
    await clearTokens();
    await clearUser();
    setUser(null);
  }, []);

  const signIn = useCallback(async (tokens: TokenResponse) => {
    await setTokens(tokens.accessToken, tokens.refreshToken);
    await saveUser(JSON.stringify(tokens.user));
    setUser(tokens.user);
  }, []);

  const updateUser = useCallback(async (nextUser: UserDto) => {
    await saveUser(JSON.stringify(nextUser));
    setUser(nextUser);
  }, []);

  useEffect(() => {
    setSessionExpiredHandler(() => {
      void signOut();
    });
    return () => setSessionExpiredHandler(null);
  }, [signOut]);

  const value = useMemo<SessionContextValue>(
    () => ({ user, isReady, isAuthenticated: user !== null, signIn, signOut, updateUser }),
    [user, isReady, signIn, signOut, updateUser],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext);
  if (!context) throw new Error('useSession must be used inside SessionProvider');
  return context;
}
