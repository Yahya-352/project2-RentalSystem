import { Link } from 'react-router-dom';

export function AuthShell({ title, subtitle, children, footer }: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
}) {
  return (
    <div className="auth">
      <aside className="auth-hero">
        <Link to="/" className="brand brand-light">
          <span className="brand-mark">◆</span> RentRide
        </Link>
        <div>
          <h2>Find the right car, from agencies you can trust.</h2>
          <p>Browse the fleet, request dates, and get approval updates the moment they happen.</p>
        </div>
        <ul className="auth-points">
          <li>Live booking notifications</li>
          <li>Verified rental agencies</li>
          <li>Transparent daily pricing</li>
        </ul>
      </aside>
      <section className="auth-main">
        <div className="auth-card">
          <h1>{title}</h1>
          {subtitle && <p className="muted">{subtitle}</p>}
          {children}
        </div>
        {footer && <div className="auth-footer">{footer}</div>}
      </section>
    </div>
  );
}
