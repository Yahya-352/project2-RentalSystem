import { useState } from 'react';
import { errorMessage } from '../api/client';
import { usersApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { Field, PageHeader } from '../components/ui';
import { useNotifications } from '../notifications/NotificationContext';

export function AccountPage() {
  const { user } = useAuth();
  const { notify } = useNotifications();
  const [form, setForm] = useState({ oldPassword: '', newPassword: '', confirm: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (form.newPassword !== form.confirm) {
      setError('New passwords do not match.');
      return;
    }
    if (form.newPassword === form.oldPassword) {
      setError('Choose a password different from your current one.');
      return;
    }
    setBusy(true);
    try {
      const msg = await usersApi.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword });
      notify(msg || 'Password changed.', 'success');
      setForm({ oldPassword: '', newPassword: '', confirm: '' });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="Account security" subtitle={user?.email} />
      <form className="card form narrow" onSubmit={submit}>
        <h2 className="section-title">Change password</h2>
        {error && <div className="alert alert-error">{error}</div>}
        <Field label="Current password">
          <input
            type="password"
            value={form.oldPassword}
            onChange={set('oldPassword')}
            autoComplete="current-password"
            required
          />
        </Field>
        <Field label="New password" hint="8–72 characters">
          <input
            type="password"
            value={form.newPassword}
            onChange={set('newPassword')}
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
            required
          />
        </Field>
        <Field label="Confirm new password">
          <input type="password" value={form.confirm} onChange={set('confirm')} autoComplete="new-password" required />
        </Field>
        <div className="form-actions">
          <button className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : 'Update password'}
          </button>
        </div>
      </form>
    </>
  );
}
