import { useSearchParams } from 'react-router-dom';
import { bookingsApi } from '../../api/endpoints';
import type { BookingStatus } from '../../api/types';
import { EmptyState, ErrorBanner, PageHeader, Pagination, Spinner, StatusBadge } from '../../components/ui';
import { BOOKING_STATUSES } from '../../lib/constants';
import { formatDate, formatDateTime, formatMoney, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';

const PAGE_SIZE = 15;

export function AdminBookingsPage() {
  const [params, setParams] = useSearchParams();
  const status = (params.get('status') ?? '') as BookingStatus | '';
  const page = Number(params.get('page') ?? 0);

  const { data, error, loading, reload } = useAsync(
    () => bookingsApi.all({ status, page, size: PAGE_SIZE, sort: 'createdAt,desc' }),
    [status, page],
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
                    <td>User #{b.renterId}</td>
                    <td>
                      {formatDate(b.startDate)} → {formatDate(b.endDate)}
                    </td>
                    <td className="num">{formatMoney(b.totalPrice)}</td>
                    <td>
                      <StatusBadge status={b.status} />
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
