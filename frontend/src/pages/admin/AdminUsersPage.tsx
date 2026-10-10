import { useState } from 'react';
import { errorMessage } from '../../api/client';
import { usersApi } from '../../api/endpoints';
import { ConfirmDialog, Field, PageHeader } from '../../components/ui';
import { formatDateTime } from '../../lib/format';
import { useNotifications } from '../../notifications/NotificationContext';

type Action = 'activate' | 'deactivate';

interface LogEntry {
  at: string;
  userId: number;
  action: Action;
  ok: boolean;
  message: string;
}

/**
 * The backend exposes activate/deactivate by user id but no endpoint to list users,
 * so the admin enters the id directly (e.g. the renter id shown on bookings).
 */
export function AdminUsersPage() {
  const { notify } = useNotifications();
  const [userId, setUserId] = useState('');
  const [confirm, setConfirm] = useState<Action | null>(null);
  const [busy, setBusy] = useState(false);
  const [log, setLog] = useState<LogEntry[]>([]);

  const id = Number(userId);
  const validId = Number.isInteger(id) && id > 0;

  const run = async () => {
    if (!confirm || !validId) return;
    setBusy(true);
    const action = confirm;
    try {
      await usersApi[action](id);
      const message = action === 'activate' ? `User #${id} activated.` : `User #${id} deactivated.`;
      notify(message, 'success');
      setLog((l) => [{ at: new Date().toISOString(), userId: id, action, ok: true, message }, ...l]);
    } catch (err) {
      const message = errorMessage(err);
      notify(message, 'error');
      setLog((l) => [{ at: new Date().toISOString(), userId: id, action, ok: false, message }, ...l]);
    } finally {
      setBusy(false);
      setConfirm(null);
    }
  };

  return (
    <>
      <PageHeader title="User management" subtitle="Approve agencies and activate or deactivate accounts." />

      <div className="admin-grid">
        <form
          className="card form"
          onSubmit={(e) => {
            e.preventDefault();
            if (validId) setConfirm('activate');
          }}
        >
          <h2 className="section-title">Change account status</h2>
          <Field label="User ID" hint="Activating a pending agency also approves it.">
            <input
              type="number"
              min={1}
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="e.g. 4"
              required
            />
          </Field>
          <div className="form-actions start">
            <button type="submit" className="btn btn-success" disabled={!validId || busy}>
              Activate / approve
            </button>
            <button
              type="button"
              className="btn btn-danger"
              disabled={!validId || busy}
              onClick={() => setConfirm('deactivate')}
            >
              Deactivate
            </button>
          </div>
        </form>

        <section className="card">
          <h2 className="section-title">Recent actions</h2>
          {log.length === 0 ? (
            <p className="muted">Actions you take in this session appear here.</p>
          ) : (
            <ul className="activity">
              {log.map((entry, i) => (
                <li key={i} className={entry.ok ? '' : 'failed'}>
                  <span className={`dot ${entry.ok ? 'ok' : 'bad'}`} />
                  <div>
                    <div>{entry.message}</div>
                    <small className="muted">{formatDateTime(entry.at)}</small>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <ConfirmDialog
        open={Boolean(confirm)}
        title={confirm === 'deactivate' ? 'Deactivate user?' : 'Activate user?'}
        message={
          confirm === 'deactivate'
            ? `User #${userId} will be logged out everywhere and unable to log in again until reactivated.`
            : `User #${userId} will be able to log in. If this is a pending agency, it will be approved.`
        }
        confirmLabel={confirm === 'deactivate' ? 'Deactivate' : 'Activate'}
        danger={confirm === 'deactivate'}
        busy={busy}
        onConfirm={run}
        onClose={() => setConfirm(null)}
      />
    </>
  );
}
