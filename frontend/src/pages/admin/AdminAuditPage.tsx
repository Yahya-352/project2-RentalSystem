import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { auditApi } from '../../api/endpoints';
import { EmptyState, ErrorBanner, PageHeader, Pagination, Spinner } from '../../components/ui';
import { formatDateTime, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';

const PAGE_SIZE = 20;
const ENTITY_TYPES = ['User', 'Car', 'Booking', 'Payment'];
const ACTIONS = [
  'USER_REGISTERED',
  'EMAIL_VERIFIED',
  'USER_ACTIVATED',
  'AGENCY_APPROVED',
  'USER_DEACTIVATED',
  'PASSWORD_CHANGED',
  'PASSWORD_RESET',
  'PROFILE_CREATED',
  'PROFILE_UPDATED',
  'CAR_CREATED',
  'CAR_UPDATED',
  'CAR_DELETED',
  'BOOKING_CREATED',
  'BOOKING_APPROVED',
  'BOOKING_REJECTED',
  'BOOKING_CANCELLED',
  'BOOKINGS_COMPLETED',
  'BOOKINGS_AUTO_REJECTED',
  'PAYMENT_COMPLETED',
  'PAYMENT_FAILED',
  'PAYMENT_REFUNDED',
];

export function AdminAuditPage() {
  const [params, setParams] = useSearchParams();
  const action = params.get('action') ?? '';
  const entityType = params.get('entityType') ?? '';
  const userIdParam = params.get('userId') ?? '';
  const page = Number(params.get('page') ?? 0);
  const [userIdInput, setUserIdInput] = useState(userIdParam);
  const userId = userIdParam ? Number(userIdParam) : undefined;

  const { data, error, loading, reload } = useAsync(
    () => auditApi.list({ action, entityType, userId, page, size: PAGE_SIZE }),
    [action, entityType, userIdParam, page],
  );

  const update = (next: Record<string, string | null>) => {
    const p = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) {
      if (v === null || v === '') p.delete(k);
      else p.set(k, v);
    }
    setParams(p, { replace: true });
  };

  return (
    <>
      <PageHeader
        title="Audit log"
        subtitle="Every important action, collected by the audit service from Kafka events."
      />

      <form
        className="toolbar card"
        onSubmit={(e) => {
          e.preventDefault();
          update({ userId: userIdInput.trim(), page: null });
        }}
      >
        <select value={action} onChange={(e) => update({ action: e.target.value, page: null })} aria-label="Action">
          <option value="">All actions</option>
          {ACTIONS.map((a) => (
            <option key={a} value={a}>
              {titleCase(a)}
            </option>
          ))}
        </select>
        <select
          value={entityType}
          onChange={(e) => update({ entityType: e.target.value, page: null })}
          aria-label="Entity type"
        >
          <option value="">All entities</option>
          {ENTITY_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <div className="search">
          <input
            type="number"
            min={1}
            value={userIdInput}
            onChange={(e) => setUserIdInput(e.target.value)}
            placeholder="User id"
            aria-label="User id"
          />
          <button type="submit" className="btn btn-primary">
            Filter
          </button>
        </div>
      </form>

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState title="No audit entries found" />
      ) : (
        <>
          <div className={`table-wrap card ${loading ? 'is-loading' : ''}`}>
            <table className="table">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Action</th>
                  <th>Entity</th>
                  <th>By</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((entry) => (
                  <tr key={entry.id}>
                    <td className="muted small">{formatDateTime(entry.createdAt)}</td>
                    <td>
                      <strong>{titleCase(entry.action)}</strong>
                    </td>
                    <td>
                      {entry.entityType}
                      {entry.entityId != null && <span className="muted"> #{entry.entityId}</span>}
                    </td>
                    <td>{entry.userId != null ? `User #${entry.userId}` : <span className="muted">System</span>}</td>
                    <td className="small">{entry.details}</td>
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
    </>
  );
}
