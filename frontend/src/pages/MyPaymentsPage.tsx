import { useSearchParams } from 'react-router-dom';
import { paymentsApi } from '../api/endpoints';
import type { PaymentStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { EmptyState, ErrorBanner, PageHeader, Pagination, Spinner, StatusBadge } from '../components/ui';
import { formatDateTime, titleCase } from '../lib/format';
import { useAsync } from '../lib/useAsync';

const PAGE_SIZE = 15;
const STATUSES: PaymentStatus[] = ['PAID', 'PENDING', 'FAILED', 'REFUNDED'];

/**
 * Payment history for the logged-in customer (what they paid) or agency (what they were paid).
 * The backend limits GET /pay/payments to the caller's own payments based on their role.
 */
export function MyPaymentsPage() {
  const { user } = useAuth();
  const isAgency = user?.role === 'AGENCY';
  const [params, setParams] = useSearchParams();
  const status = (params.get('status') ?? '') as PaymentStatus | '';
  const page = Number(params.get('page') ?? 0);

  const { data, error, loading, reload } = useAsync(
    () => paymentsApi.list({ status, page, size: PAGE_SIZE }),
    [status, page],
  );

  // All paid payments, for the totals at the top.
  const paid = useAsync(() => paymentsApi.list({ status: 'PAID', size: 500 }), []);
  const paidTotal = (paid.data?.content ?? []).reduce((sum, p) => sum + Number(p.amount), 0);
  const currency = paid.data?.content[0]?.currency ?? '';

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
        title={isAgency ? 'Earnings' : 'My payments'}
        subtitle={isAgency ? 'Payments received for bookings of your cars.' : 'Everything you paid through Stripe.'}
      />

      <div className="stats">
        <div className="stat card">
          <span className="muted small">{isAgency ? 'Total received' : 'Total paid'}</span>
          <strong>{paid.data ? `${paidTotal.toFixed(2)} ${currency}` : '—'}</strong>
        </div>
        <div className="stat card">
          <span className="muted small">Paid bookings</span>
          <strong>{paid.data?.totalElements ?? '—'}</strong>
        </div>
      </div>

      <div className="chips" role="tablist">
        {(['', ...STATUSES] as const).map((s) => (
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
        <EmptyState title="No payments yet" />
      ) : (
        <>
          <div className={`table-wrap card ${loading ? 'is-loading' : ''}`}>
            <table className="table">
              <thead>
                <tr>
                  <th>Car</th>
                  {isAgency && <th>Renter</th>}
                  <th className="num">Amount</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <strong>{p.make && p.model ? `${p.make} ${p.model}` : 'Booking'}</strong>
                      <div className="muted small">Booking #{p.bookingId}</div>
                    </td>
                    {isAgency && <td>User #{p.renterId}</td>}
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
