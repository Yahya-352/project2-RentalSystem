import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { usersApi } from '../../api/endpoints';
import { Field } from '../../components/ui';
import { AuthShell } from './AuthShell';

/** Emails link to the backend directly, so accept either the raw token or the whole pasted link. */
function extractToken(input: string) {
  const value = input.trim();
  const match = value.match(/[?&]token=([^&\s]+)/);
  return match ? decodeURIComponent(match[1]) : value;
}

/** /verify?token=... — verifies automatically when the token is in the URL, otherwise asks for it. */
export function VerifyPage() {
  const [params] = useSearchParams();
  const [token, setToken] = useState(params.get('token') ?? '');
  const [state, setState] = useState<{ busy: boolean; ok?: string; error?: string }>({ busy: false });
  const autoRan = useRef(false);

  const verify = async (value: string) => {
    setState({ busy: true });
    try {
      setState({ busy: false, ok: (await usersApi.verify(extractToken(value))) || 'Email verified.' });
    } catch (err) {
      setState({ busy: false, error: errorMessage(err) });
    }
  };

  useEffect(() => {
    const t = params.get('token');
    if (t && !autoRan.current) {
      autoRan.current = true;
      verify(t);
    }
  }, [params]);

  return (
    <AuthShell title="Verify your email" footer={<Link to="/login">Back to login</Link>}>
      {state.ok ? (
        <>
          <div className="alert alert-success">{state.ok}</div>
          <Link to="/login" className="btn btn-primary btn-block">
            Continue to login
          </Link>
        </>
      ) : (
        <>
          {state.error && <div className="alert alert-error">{state.error}</div>}
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault();
              verify(token);
            }}
          >
            <Field label="Verification token" hint="Paste the token, or the whole link, from your verification email.">
              <input value={token} onChange={(e) => setToken(e.target.value)} required />
            </Field>
            <button className="btn btn-primary btn-block" disabled={state.busy}>
              {state.busy ? 'Verifying…' : 'Verify email'}
            </button>
          </form>
          <ResendVerification />
        </>
      )}
    </AuthShell>
  );
}

function ResendVerification() {
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState('');
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const [busy, setBusy] = useState(false);

  if (!open)
    return (
      <p className="muted small center">
        Link expired?{' '}
        <button className="link-btn" onClick={() => setOpen(true)}>
          Send a new one
        </button>
      </p>
    );

  return (
    <form
      className="form subtle-box"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        try {
          setMsg({ ok: true, text: await usersApi.resendVerification(email.trim()) });
        } catch (err) {
          setMsg({ ok: false, text: errorMessage(err) });
        } finally {
          setBusy(false);
        }
      }}
    >
      {msg && <div className={`alert ${msg.ok ? 'alert-success' : 'alert-error'}`}>{msg.text}</div>}
      <Field label="Your email">
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      </Field>
      <button className="btn btn-ghost btn-block" disabled={busy}>
        {busy ? 'Sending…' : 'Resend verification email'}
      </button>
    </form>
  );
}

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);

  return (
    <AuthShell
      title="Reset your password"
      subtitle="We'll email you a link to choose a new password."
      footer={<Link to="/login">Back to login</Link>}
    >
      {msg && <div className={`alert ${msg.ok ? 'alert-success' : 'alert-error'}`}>{msg.text}</div>}
      <form
        className="form"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          try {
            setMsg({ ok: true, text: (await usersApi.forgotPassword(email.trim())) || 'Check your email.' });
          } catch (err) {
            setMsg({ ok: false, text: errorMessage(err) });
          } finally {
            setBusy(false);
          }
        }}
      >
        <Field label="Email">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </Field>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Sending…' : 'Send reset link'}
        </button>
      </form>
      <p className="muted small center">
        Already have a token? <Link to="/reset-password">Set a new password</Link>
      </p>
    </AuthShell>
  );
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [form, setForm] = useState({ token: params.get('token') ?? '', password: '', confirm: '' });
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  if (msg?.ok)
    return (
      <AuthShell title="Password updated">
        <div className="alert alert-success">{msg.text}</div>
        <Link to="/login" className="btn btn-primary btn-block">
          Log in
        </Link>
      </AuthShell>
    );

  return (
    <AuthShell title="Choose a new password" footer={<Link to="/login">Back to login</Link>}>
      {msg && <div className="alert alert-error">{msg.text}</div>}
      <form
        className="form"
        onSubmit={async (e) => {
          e.preventDefault();
          if (form.password !== form.confirm) {
            setMsg({ ok: false, text: 'Passwords do not match.' });
            return;
          }
          setBusy(true);
          try {
            const text = await usersApi.resetPassword({ token: extractToken(form.token), password: form.password });
            setMsg({ ok: true, text: text || 'Your password has been reset.' });
          } catch (err) {
            setMsg({ ok: false, text: errorMessage(err) });
          } finally {
            setBusy(false);
          }
        }}
      >
        {!params.get('token') && (
          <Field label="Reset token" hint="Paste the token, or the whole link, from the reset email.">
            <input value={form.token} onChange={set('token')} required />
          </Field>
        )}
        <Field label="New password" hint="8–72 characters">
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
        <Field label="Confirm new password">
          <input type="password" value={form.confirm} onChange={set('confirm')} autoComplete="new-password" required />
        </Field>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Saving…' : 'Reset password'}
        </button>
      </form>
    </AuthShell>
  );
}
