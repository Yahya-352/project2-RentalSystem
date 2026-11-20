import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { paymentsApi, usersApi } from '../../api/endpoints';
import type { PaymentStatus } from '../../api/types';
import { EmptyState, ErrorBanner, PageHeader, Pagination, Spinner, StatusBadge } from '../../components/ui';
import { formatDateTime, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';

const PAGE_SIZE = 15;
const STATUSES: PaymentStatus[] = ['PAID', 'PENDING', 'FAILED', 'REFUNDED'];

export function AdminPaymentsPage() {
  const [params, setParams] = useSearchParams();
  const status = (params.get('status') ?? '') as PaymentStatus | '';
  const ownerParam = params.get('ownerId') ?? '';
  const renterParam = params.get('renterId') ?? '';
  const page = Number(params.get('page') ?? 0);

  // Agencies and customers for the filter dropdowns and to show names instead of ids.
  const agencies = useAsync(() => usersApi.list({ role: 'AGENCY', size: 200 }), []);
  const customers = useAsync(() => usersApi.list({ role: 'CUSTOMER', size: 200 }), []);
  const names = useMemo(() => {
    const map = new Map<number, string>();
    [...(agencies.data?.content ?? []), ...(customers.data?.content ?? [])].forEach((u) => map.set(u.id, u.userName));
    return map;
  }, [agencies.data, customers.data]);

  const { data, error, loading, reload } = useAsync(
    () =>
      paymentsApi.list({
        status,
        ownerId: ownerParam ? Number(ownerParam) : undefined,
        renterId: renterParam ? Number(renterParam) : undefined,
        page,
        size: PAGE_SIZE,
      }),
    [status, ownerParam, renterParam, page],
  );

  const update = (next: Record<string, string | null>) => {
    const p = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) {
      if (v === null || v === '') p.delete(k);
      else p.set(k, v);
    }
    setParams(p, { replace: true });
  };

  const who = (id: number) => names.get(id) ?? `User #${id}`;

  return (
    <>
      <PageHeader title="Payments" subtitle="Every Stripe payment, filterable by agency, renter and status." />

      <div className="toolbar card">
        <select value={ownerParam} onChange={(e) => update({ ownerId: e.target.value, page: null })} aria-label="Agency">
          <option value="">All agencies</option>
          {(agencies.data?.content ?? []).map((u) => (
            <option key={u.id} value={u.id}>
              {u.userName}
            </option>
          ))}
        </select>
        <select
          value={renterParam}
          onChange={(e) => update({ renterId: e.target.value, page: null })}
          aria-label="Renter"
        >
          <option value="">All renters</option>
          {(customers.data?.content ?? []).map((u) => (
            <option key={u.id} value={u.id}>
              {u.userName}
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
      </div>

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState title="No payments found" />
      ) : (
        <>
          <div className={`table-wrap card ${loading ? 'is-loading' : ''}`}>
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Booking</th>
                  <th>Renter</th>
                  <th>Agency</th>
                  <th className="num">Amount</th>
                  <th>Status</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id}>
                    <td className="muted">{p.id}</td>
                    <td>
                      <strong>{p.make && p.model ? `${p.make} ${p.model}` : 'Booking'}</strong>
                      <div className="muted small">Booking #{p.bookingId}</div>
                    </td>
                    <td>{who(p.renterId)}</td>
                    <td>{who(p.ownerId)}</td>
                    <td className="num">
                      {Number(p.amount).toFixed(2)} {p.currency}
                    </td>
                    <td>
                      <StatusBadge status={p.status} />
                    </td>
                    <td className="muted small">{formatDateTime(p.updatedAt ?? p.createdAt)}</td>
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
