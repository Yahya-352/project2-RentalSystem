import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { buildUrl } from '../api/client';
import type { BookingResponse } from '../api/types';
import { useAuth } from '../auth/AuthContext';

export type ToastKind = 'success' | 'error' | 'info';

interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

export interface AppNotification {
  id: number;
  event: string;
  booking: BookingResponse | null;
  receivedAt: number;
  read: boolean;
}

interface NotificationContextValue {
  notify: (message: string, kind?: ToastKind) => void;
  notifications: AppNotification[];
  unread: number;
  markAllRead: () => void;
  clear: () => void;
  connected: boolean;
  /** Increments on every booking event so pages can refetch their data. */
  bookingVersion: number;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

const EVENT_TEXT: Record<string, (b: BookingResponse | null) => string> = {
  BOOKING_CREATED: (b) => `New booking request for ${carName(b)}`,
  BOOKING_APPROVED: (b) => `Your booking for ${carName(b)} was approved`,
  BOOKING_REJECTED: (b) => `Your booking for ${carName(b)} was rejected`,
  BOOKING_CANCELLED: (b) => `Booking for ${carName(b)} was cancelled`,
};

function carName(b: BookingResponse | null) {
  return b ? `${b.make} ${b.model}` : 'a car';
}

export function describeEvent(n: AppNotification) {
  return (EVENT_TEXT[n.event] ?? (() => n.event.replace(/_/g, ' ').toLowerCase()))(n.booking);
}

let nextId = 1;

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const { token } = useAuth();
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [connected, setConnected] = useState(false);
  const [bookingVersion, setBookingVersion] = useState(0);
  const timers = useRef(new Map<number, number>());

  const dismiss = useCallback((id: number) => {
    setToasts((list) => list.filter((t) => t.id !== id));
    window.clearTimeout(timers.current.get(id));
    timers.current.delete(id);
  }, []);

  const notify = useCallback(
    (message: string, kind: ToastKind = 'info') => {
      const id = nextId++;
      setToasts((list) => [...list.slice(-3), { id, kind, message }]);
      timers.current.set(id, window.setTimeout(() => dismiss(id), kind === 'error' ? 7000 : 4500));
    },
    [dismiss],
  );

  // Live booking events. EventSource cannot send an Authorization header, so the SSE stream
  // is read with fetch and parsed by hand. Reconnects with backoff if the connection drops.
  useEffect(() => {
    setNotifications([]);
    if (!token) return;

    const controller = new AbortController();
    let retryDelay = 2000;
    let retryTimer: number | undefined;

    const handleEvent = (event: string, data: string) => {
      if (event === 'CONNECTED') return;
      let booking: BookingResponse | null = null;
      try {
        booking = JSON.parse(data);
      } catch {
        /* non-JSON payload */
      }
      const n: AppNotification = { id: nextId++, event, booking, receivedAt: Date.now(), read: false };
      setNotifications((list) => [n, ...list].slice(0, 30));
      setBookingVersion((v) => v + 1);
      notify(describeEvent(n), event === 'BOOKING_REJECTED' ? 'error' : 'info');
    };

    const connect = async () => {
      try {
        const res = await fetch(buildUrl('/notifications/stream'), {
          headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
          signal: controller.signal,
        });
        if (!res.ok || !res.body) throw new Error(`stream ${res.status}`);
        setConnected(true);
        retryDelay = 2000;

        const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
        let buffer = '';
        for (;;) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += value.replace(/\r\n/g, '\n');
          let boundary: number;
          while ((boundary = buffer.indexOf('\n\n')) >= 0) {
            const block = buffer.slice(0, boundary);
            buffer = buffer.slice(boundary + 2);
            let event = 'message';
            const data: string[] = [];
            for (const line of block.split('\n')) {
              if (line.startsWith('event:')) event = line.slice(6).trim();
              else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''));
            }
            if (data.length || event !== 'message') handleEvent(event, data.join('\n'));
          }
        }
      } catch {
        if (controller.signal.aborted) return;
      }
      setConnected(false);
      if (controller.signal.aborted) return;
      retryTimer = window.setTimeout(connect, retryDelay);
      retryDelay = Math.min(retryDelay * 2, 30000);
    };

    connect();
    return () => {
      controller.abort();
      window.clearTimeout(retryTimer);
      setConnected(false);
    };
  }, [token, notify]);

  const markAllRead = useCallback(() => setNotifications((list) => list.map((n) => ({ ...n, read: true }))), []);
  const clear = useCallback(() => setNotifications([]), []);

  const value = useMemo<NotificationContextValue>(
    () => ({
      notify,
      notifications,
      unread: notifications.filter((n) => !n.read).length,
      markAllRead,
      clear,
      connected,
      bookingVersion,
    }),
    [notify, notifications, markAllRead, clear, connected, bookingVersion],
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.kind}`}>
            <span>{t.message}</span>
            <button className="toast-close" onClick={() => dismiss(t.id)} aria-label="Dismiss">
              ×
            </button>
          </div>
        ))}
      </div>
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error('useNotifications must be used inside <NotificationProvider>');
  return ctx;
}
