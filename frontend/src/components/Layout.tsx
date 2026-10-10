import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '../api/types';
import { homeFor, useAuth } from '../auth/AuthContext';
import { useProfileImage } from '../auth/ProfileImageContext';
import { formatDateTime } from '../lib/format';
import { describeEvent, useNotifications } from '../notifications/NotificationContext';

const NAV: Record<Role, { to: string; label: string }[]> = {
  CUSTOMER: [
    { to: '/cars', label: 'Browse cars' },
    { to: '/bookings', label: 'My bookings' },
    { to: '/profile', label: 'Profile' },
  ],
  AGENCY: [
    { to: '/agency/cars', label: 'My fleet' },
    { to: '/agency/bookings', label: 'Booking requests' },
    { to: '/cars', label: 'Marketplace' },
    { to: '/agency/profile', label: 'Agency profile' },
  ],
  ADMIN: [
    { to: '/admin/bookings', label: 'All bookings' },
    { to: '/admin/users', label: 'Users' },
    { to: '/cars', label: 'Cars' },
  ],
};

export function Layout() {
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <Link to={homeFor(user?.role)} className="brand">
            <span className="brand-mark">◆</span> RentRide
          </Link>

          <button className="icon-btn nav-toggle" onClick={() => setMenuOpen((o) => !o)} aria-label="Menu">
            ☰
          </button>

          <nav className={`nav ${menuOpen ? 'open' : ''}`}>
            {user &&
              NAV[user.role].map((item) => (
                <NavLink key={item.to} to={item.to} className="nav-link" end={item.to === '/cars'}>
                  {item.label}
                </NavLink>
              ))}
          </nav>

          {user && (
            <div className="topbar-right">
              <NotificationBell />
              <UserMenu email={user.email} role={user.role} onLogout={logout} />
            </div>
          )}
        </div>
      </header>

      <main className="container">
        <Outlet />
      </main>
    </div>
  );
}

function useClickOutside(onOutside: () => void) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onOutside();
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [onOutside]);
  return ref;
}

function NotificationBell() {
  const { notifications, unread, markAllRead, clear, connected } = useNotifications();
  const [open, setOpen] = useState(false);
  const ref = useClickOutside(() => setOpen(false));

  const toggle = () => {
    setOpen((o) => !o);
    if (!open) markAllRead();
  };

  return (
    <div className="dropdown" ref={ref}>
      <button className="icon-btn bell" onClick={toggle} aria-label={`Notifications (${unread} unread)`}>
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path
            fill="currentColor"
            d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22zm7-6V11a7 7 0 0 0-5.5-6.84V3.5a1.5 1.5 0 0 0-3 0v.66A7 7 0 0 0 5 11v5l-2 2v1h18v-1z"
          />
        </svg>
        {unread > 0 && <span className="bell-count">{unread}</span>}
        <span className={`live-dot ${connected ? 'on' : ''}`} title={connected ? 'Live' : 'Offline'} />
      </button>
      {open && (
        <div className="dropdown-panel notif-panel">
          <div className="dropdown-head">
            <strong>Notifications</strong>
            {notifications.length > 0 && (
              <button className="link-btn" onClick={clear}>
                Clear
              </button>
            )}
          </div>
          {notifications.length === 0 ? (
            <p className="muted notif-empty">
              No notifications yet. Booking updates appear here in real time{connected ? '' : ' once connected'}.
            </p>
          ) : (
            <ul className="notif-list">
              {notifications.map((n) => (
                <li key={n.id}>
                  <span>{describeEvent(n)}</span>
                  <small className="muted">{formatDateTime(new Date(n.receivedAt).toISOString())}</small>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

function UserMenu({ email, role, onLogout }: { email: string; role: Role; onLogout: () => void }) {
  const [open, setOpen] = useState(false);
  const ref = useClickOutside(() => setOpen(false));
  const { imageUrl } = useProfileImage();

  return (
    <div className="dropdown" ref={ref}>
      <button className="avatar-btn" onClick={() => setOpen((o) => !o)} aria-label="Account menu">
        {imageUrl ? (
          <img src={imageUrl} alt="" className="avatar avatar-img" />
        ) : (
          <span className="avatar">{email.charAt(0).toUpperCase()}</span>
        )}
      </button>
      {open && (
        <div className="dropdown-panel">
          <div className="dropdown-head column">
            <strong className="truncate">{email}</strong>
            <span className={`role-chip role-${role.toLowerCase()}`}>{role.toLowerCase()}</span>
          </div>
          <Link to="/account" className="dropdown-item" onClick={() => setOpen(false)}>
            Change password
          </Link>
          <button className="dropdown-item danger" onClick={onLogout}>
            Log out
          </button>
        </div>
      )}
    </div>
  );
}
