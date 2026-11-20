import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { bookingsApi, usersApi } from '../../api/endpoints';
import type { BookingStatus } from '../../api/types';
import { EmptyState, ErrorBanner, PageHeader, Pagination, PaidBadge, Spinner, StatusBadge } from '../../components/ui';
import { BOOKING_STATUSES } from '../../lib/constants';
import { formatDate, formatDateTime, formatMoney, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';

const PAGE_SIZE = 15;

export function AdminBookingsPage() {
  const [params, setParams] = useSearchParams();
  const status = (params.get('status') ?? '') as BookingStatus | '';
  const ownerParam = params.get('ownerId') ?? '';
  const renterParam = params.get('renterId') ?? '';
  const paidParam = params.get('paid') ?? '';
  const from = params.get('from') ?? '';
  const to = params.get('to') ?? '';
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
      bookingsApi.all({
        status,
        ownerId: ownerParam ? Number(ownerParam) : undefined,
        renterId: renterParam ? Number(renterParam) : undefined,
        paid: paidParam ? paidParam === 'true' : undefined,
        from: from || undefined,
        to: to || undefined,
        page,
        size: PAGE_SIZE,
        sort: 'createdAt,desc',
      }),
    [status, ownerParam, renterParam, paidParam, from, to, page],
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
  const hasFilters = Boolean(ownerParam || renterParam || paidParam || from || to);

  return (
    <>
      <PageHeader title="All bookings" subtitle="Every booking across the platform." />

      <div className="chips" role="tablist">
        {(['', ...BOOKING_STATUSES] as const).map((s) => (
          <button
            key={s || 'all'}
            role="tab"
            aria-selected={status === s}
            className={`chip ${status === s ? 'active' : ''}`}
            onClick={() => update({ status: s, page: null })}
          >
            {s ? titleCase(s) : 'All'}
          </button>
        ))}
      </div>

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
        <select value={paidParam} onChange={(e) => update({ paid: e.target.value, page: null })} aria-label="Payment">
          <option value="">Paid or unpaid</option>
          <option value="true">Paid</option>
          <option value="false">Unpaid</option>
        </select>
        <input
          type="date"
          value={from}
          onChange={(e) => update({ from: e.target.value, page: null })}
          aria-label="From"
          title="From"
        />
        <input
          type="date"
          value={to}
          min={from || undefined}
          onChange={(e) => update({ to: e.target.value, page: null })}
          aria-label="To"
          title="To"
        />
        {hasFilters && (
          <button
            type="button"
            className="btn btn-sm btn-ghost"
            onClick={() => update({ ownerId: null, renterId: null, paid: null, from: null, to: null, page: null })}
          >
            Clear filters
          </button>
        )}
      </div>

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner />
      ) : !data || data.content.length === 0 ? (
        <EmptyState title="No bookings found" />
      ) : (
        <>
          <div className={`table-wrap card ${loading ? 'is-loading' : ''}`}>
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Car</th>
                  <th>Renter</th>
                  <th>Agency</th>
                  <th>Dates</th>
                  <th className="num">Total</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((b) => (
                  <tr key={b.id}>
                    <td className="muted">{b.id}</td>
                    <td>
                      <strong>
                        {b.make} {b.model}
                      </strong>
                      <div className="muted small">Car #{b.carId}</div>
                    </td>
                    <td>{who(b.renterId)}</td>
                    <td>{who(b.ownerId)}</td>
                    <td>
                      {formatDate(b.startDate)} → {formatDate(b.endDate)}
                    </td>
                    <td className="num">{formatMoney(b.totalPrice)}</td>
                    <td>
                      <StatusBadge status={b.status} />
                      <PaidBadge paid={b.paid} refunded={b.refunded} />
                    </td>
                    <td className="muted small">{formatDateTime(b.createdAt)}</td>
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
