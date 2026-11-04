import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Snackbar } from 'react-native-paper';

interface SnackbarContextValue {
  show: (message: string) => void;
}

const SnackbarContext = createContext<SnackbarContextValue | undefined>(undefined);

/**
 * Snackbar toàn app — màn hình gọi useSnackbar().show(message).
 * @reference docs/plan-app/01-phase-1.md §4.2
 */
export function SnackbarProvider({ children }: { children: React.ReactNode }) {
  const [message, setMessage] = useState('');
  const [visible, setVisible] = useState(false);

  const show = useCallback((nextMessage: string) => {
    setMessage(nextMessage);
    setVisible(true);
  }, []);

  const value = useMemo<SnackbarContextValue>(() => ({ show }), [show]);

  return (
    <SnackbarContext.Provider value={value}>
      {children}
      <Snackbar visible={visible} onDismiss={() => setVisible(false)} duration={3500}>
        {message}
      </Snackbar>
    </SnackbarContext.Provider>
  );
}

export function useSnackbar(): SnackbarContextValue {
  const context = useContext(SnackbarContext);
  if (!context) throw new Error('useSnackbar must be used inside SnackbarProvider');
  return context;
}
