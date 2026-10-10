import { useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { usersApi } from '../../api/endpoints';
import type { RegisterResponse } from '../../api/types';
import { Field } from '../../components/ui';
import { AuthShell } from './AuthShell';

type AccountType = 'customer' | 'agency';

export function RegisterPage() {
  const [type, setType] = useState<AccountType>('customer');
  const [form, setForm] = useState({ userName: '', email: '', password: '', confirm: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<RegisterResponse | null>(null);

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (form.password !== form.confirm) {
      setError('Passwords do not match.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const body = { userName: form.userName.trim(), email: form.email.trim(), password: form.password };
      setDone(type === 'agency' ? await usersApi.registerAgency(body) : await usersApi.registerCustomer(body));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (done) {
    return (
      <AuthShell title="Check your inbox" footer={<Link to="/login">Back to login</Link>}>
        <div className="alert alert-success">{done.message || 'Account created.'}</div>
        <p>
          We sent a verification link to <strong>{done.email}</strong>. Open it to activate your account
          {type === 'agency' && ', then wait for an administrator to approve your agency before logging in'}.
        </p>
        <p className="muted small">
          Got a token instead of a link? <Link to="/verify">Enter it here</Link>.
        </p>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      title="Create your account"
      footer={
        <>
          Already have an account? <Link to="/login">Log in</Link>
        </>
      }
    >
      <div className="segmented" role="tablist">
        {(['customer', 'agency'] as const).map((t) => (
          <button
            key={t}
            type="button"
            role="tab"
            aria-selected={type === t}
            className={type === t ? 'active' : ''}
            onClick={() => setType(t)}
          >
            {t === 'customer' ? 'I want to rent' : 'I rent out cars'}
          </button>
        ))}
      </div>
      {type === 'agency' && (
        <p className="muted small">Agency accounts need administrator approval before you can log in.</p>
      )}

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}

      <form onSubmit={submit} className="form">
        <Field label={type === 'agency' ? 'Agency username' : 'Username'}>
          <input value={form.userName} onChange={set('userName')} autoComplete="username" required />
        </Field>
        <Field label="Email">
          <input type="email" value={form.email} onChange={set('email')} autoComplete="email" required />
        </Field>
        <Field label="Password" hint="8–72 characters">
          <input
            type="password"
            value={form.password}
            onChange={set('password')}
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
            required
          />
        </Field>
        <Field label="Confirm password">
          <input
            type="password"
            value={form.confirm}
            onChange={set('confirm')}
            autoComplete="new-password"
            required
          />
        </Field>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthShell>
  );
}
