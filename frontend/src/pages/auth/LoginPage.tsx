import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { usersApi } from '../../api/endpoints';
import { homeFor, useAuth } from '../../auth/AuthContext';
import { Field } from '../../components/ui';
import { AuthShell } from './AuthShell';

// Demo logins come from frontend/.env (gitignored) so they match the accounts the user-service
// seeder created; the fallbacks are only placeholders.
const env = import.meta.env;
const DEMO_ACCOUNTS = [
  { label: 'Customer', email: env.VITE_DEMO_CUSTOMER_EMAIL ?? 'customer@rental.com', password: env.VITE_DEMO_SEED_PASSWORD ?? '' },
  { label: 'Agency', email: env.VITE_DEMO_AGENCY_EMAIL ?? 'agency@rental.com', password: env.VITE_DEMO_SEED_PASSWORD ?? '' },
  { label: 'Admin', email: env.VITE_DEMO_ADMIN_EMAIL ?? 'admin@rental.com', password: env.VITE_DEMO_ADMIN_PASSWORD ?? '' },
];

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const needsVerification = error?.toLowerCase().includes('verify');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      const user = await login({ email: email.trim(), password });
      navigate(from && from !== '/login' ? from : homeFor(user.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const resend = async () => {
    setBusy(true);
    try {
      setInfo(await usersApi.resendVerification(email.trim()));
      setError(null);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell
      title="Welcome back"
      subtitle="Log in to manage your rentals."
      footer={
        <>
          New here? <Link to="/register">Create an account</Link>
        </>
      }
    >
      {error && (
        <div className="alert alert-error" role="alert">
          <span>{error}</span>
          {needsVerification && email && (
            <button className="btn btn-sm btn-ghost" onClick={resend} disabled={busy}>
              Resend email
            </button>
          )}
        </div>
      )}
      {info && <div className="alert alert-success">{info}</div>}

      <form onSubmit={submit} className="form">
        <Field label="Email">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </Field>
        <Field label="Password">
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </Field>
        <div className="row-between">
          <Link to="/forgot-password" className="small">
            Forgot password?
          </Link>
        </div>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Signing in…' : 'Log in'}
        </button>
      </form>

      {import.meta.env.DEV && (
        <div className="demo">
          <span className="muted small">Seeded dev accounts:</span>
          <div className="demo-buttons">
            {DEMO_ACCOUNTS.map((a) => (
              <button
                key={a.email}
                type="button"
                className="chip"
                onClick={() => {
                  setEmail(a.email);
                  setPassword(a.password);
                }}
              >
                {a.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </AuthShell>
  );
}
