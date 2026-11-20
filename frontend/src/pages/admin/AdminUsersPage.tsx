import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { usersApi } from '../../api/endpoints';
import type { Role, UserStatus, UserSummary } from '../../api/types';
import { ConfirmDialog, EmptyState, ErrorBanner, PageHeader, Pagination, Spinner, StatusBadge } from '../../components/ui';
import { formatDateTime, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';

const PAGE_SIZE = 15;
const ROLES: Role[] = ['CUSTOMER', 'AGENCY', 'ADMIN'];
const STATUSES: UserStatus[] = ['ACTIVE', 'UNAPPROVED_AGENCY', 'UNVERIFIED', 'INACTIVE'];

type Action = 'activate' | 'deactivate';

export function AdminUsersPage() {
  const { notify } = useNotifications();
  const [params, setParams] = useSearchParams();
  const role = (params.get('role') ?? '') as Role | '';
  const status = (params.get('status') ?? '') as UserStatus | '';
  const search = params.get('search') ?? '';
  const page = Number(params.get('page') ?? 0);
  const [searchInput, setSearchInput] = useState(search);
  const [pending, setPending] = useState<{ user: UserSummary; action: Action } | null>(null);
  const [busy, setBusy] = useState(false);

  const { data, error, loading, reload } = useAsync(
    () => usersApi.list({ role, status, search, page, size: PAGE_SIZE }),
    [role, status, search, page],
  );

  const update = (next: Record<string, string | null>) => {
    const p = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) {
      if (v === null || v === '') p.delete(k);
      else p.set(k, v);
    }
    setParams(p, { replace: true });
  };

  const run = async () => {
    if (!pending) return;
    const { user, action } = pending;
    setBusy(true);
    try {
      await usersApi[action](user.id);
      notify(action === 'activate' ? `${user.userName} activated.` : `${user.userName} deactivated.`, 'success');
      reload();
    } catch (err) {
      notify(errorMessage(err), 'error');
    } finally {
      setBusy(false);
      setPending(null);
    }
  };

  return (
    <>
      <PageHeader title="User management" subtitle="Approve agencies and activate or deactivate accounts." />

      <form
        className="toolbar card"
        onSubmit={(e) => {
          e.preventDefault();
          update({ search: searchInput.trim(), page: null });
        }}
      >
        <select value={role} onChange={(e) => update({ role: e.target.value, page: null })} aria-label="Role">
          <option value="">All roles</option>
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {titleCase(r)}
            </option>
          ))}
        </select>
        <select value={status} onChange={(e) => update({ status: e.target.value, page: null })} aria-label="Status">
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {titleCase(s)}
            </option>
          ))}
        </select>
        <div className="search">
          <input
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search username or email"
            aria-label="Search"
          />
          <button type="submit" className="btn btn-primary">
            Search
          </button>
        </div>
      </form>

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState title="No users found" />
      ) : (
        <>
          <div className={`table-wrap card ${loading ? 'is-loading' : ''}`}>
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>User</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Joined</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((u) => (
                  <tr key={u.id}>
                    <td className="muted">{u.id}</td>
                    <td>
                      <strong>{u.userName}</strong>
                      <div className="muted small">{u.email}</div>
                    </td>
                    <td>{titleCase(u.role)}</td>
                    <td>
                      <StatusBadge status={u.status} />
                    </td>
                    <td className="muted small">{formatDateTime(u.createdAt)}</td>
                    <td className="num">
                      {u.status === 'ACTIVE' ? (
                        u.role !== 'ADMIN' && (
                          <button
                            className="btn btn-sm btn-danger"
                            onClick={() => setPending({ user: u, action: 'deactivate' })}
                          >
                            Deactivate
                          </button>
                        )
                      ) : (
                        <button
                          className="btn btn-sm btn-success"
                          onClick={() => setPending({ user: u, action: 'activate' })}
                        >
                          {u.status === 'UNAPPROVED_AGENCY' ? 'Approve' : 'Activate'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onChange={(p) => update({ page: String(p) })}
          />
        </>
      )}

      <ConfirmDialog
        open={Boolean(pending)}
        title={pending?.action === 'deactivate' ? 'Deactivate user?' : 'Activate user?'}
        message={
          pending?.action === 'deactivate'
            ? `${pending.user.userName} will not be able to log in again until reactivated.`
            : `${pending?.user.userName} will be able to log in. A pending agency is approved.`
        }
        confirmLabel={pending?.action === 'deactivate' ? 'Deactivate' : 'Activate'}
        danger={pending?.action === 'deactivate'}
        busy={busy}
        onConfirm={run}
        onClose={() => setPending(null)}
      />
    </>
  );
}
