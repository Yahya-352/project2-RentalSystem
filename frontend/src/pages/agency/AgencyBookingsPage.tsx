import { useEffect, useMemo, useState } from 'react';
import { errorMessage } from '../../api/client';
import { bookingsApi } from '../../api/endpoints';
import type { BookingResponse, BookingStatus } from '../../api/types';
import { ConfirmDialog, EmptyState, ErrorBanner, PageHeader, Spinner, StatusBadge } from '../../components/ui';
import { daysBetween, formatDate, formatDateTime, formatMoney } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';
import { StatusFilter } from '../customer/MyBookingsPage';

type Action = 'approve' | 'reject' | 'cancel';

const ACTION_COPY: Record<Action, { label: string; done: string; danger: boolean }> = {
  approve: { label: 'Approve', done: 'Booking approved.', danger: false },
  reject: { label: 'Reject', done: 'Booking rejected.', danger: true },
  cancel: { label: 'Cancel booking', done: 'Booking cancelled.', danger: true },
};

export function AgencyBookingsPage() {
  const { notify, bookingVersion } = useNotifications();
  const { data, error, loading, reload, setData } = useAsync(() => bookingsApi.forMyCars(), []);
  const [filter, setFilter] = useState<BookingStatus | 'ALL'>('PENDING');
  const [pending, setPending] = useState<{ booking: BookingResponse; action: Action } | null>(null);
  const [busy, setBusy] = useState(false);

  // A customer just requested a booking on one of our cars.
  useEffect(() => {
    if (bookingVersion > 0) reload();
  }, [bookingVersion, reload]);

  const bookings = useMemo(
    () =>
      [...(data ?? [])]
        .filter((b) => filter === 'ALL' || b.status === filter)
        .sort((a, b) => a.startDate.localeCompare(b.startDate)),
    [data, filter],
  );

  const revenue = useMemo(
    () => (data ?? []).filter((b) => b.status === 'APPROVED' || b.status === 'COMPLETED').reduce((s, b) => s + Number(b.totalPrice), 0),
    [data],
  );
  const pendingCount = (data ?? []).filter((b) => b.status === 'PENDING').length;

  const run = async () => {
    if (!pending) return;
    const { booking, action } = pending;
    setBusy(true);
    try {
      const updated = await bookingsApi[action](booking.id);
      setData((list) => list?.map((b) => (b.id === updated.id ? updated : b)));
      notify(ACTION_COPY[action].done, 'success');
      setPending(null);
    } catch (err) {
      notify(errorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="Booking requests" subtitle="Review and manage bookings on your cars." />

      <div className="stats">
        <div className="stat card">
          <span className="muted small">Awaiting review</span>
          <strong>{pendingCount}</strong>
        </div>
        <div className="stat card">
          <span className="muted small">Total bookings</span>
          <strong>{data?.length ?? '—'}</strong>
        </div>
        <div className="stat card">
          <span className="muted small">Approved revenue</span>
          <strong>{formatMoney(revenue)}</strong>
        </div>
      </div>

      <StatusFilter value={filter} onChange={setFilter} bookings={data ?? []} />

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner />
      ) : bookings.length === 0 ? (
        <EmptyState title={filter === 'PENDING' ? 'All caught up' : 'No bookings here'}>
          {filter === 'PENDING' ? 'New requests will appear here — and in your notifications — instantly.' : null}
        </EmptyState>
      ) : (
        <div className="table-wrap card">
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Car</th>
                <th>Renter</th>
                <th>Dates</th>
                <th className="num">Total</th>
                <th>Status</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {bookings.map((b) => (
                <tr key={b.id}>
                  <td className="muted">{b.id}</td>
                  <td>
                    <strong>
                      {b.make} {b.model}
                    </strong>
                    <div className="muted small">requested {formatDateTime(b.createdAt)}</div>
                  </td>
                  <td>User #{b.renterId}</td>
                  <td>
                    {formatDate(b.startDate)} → {formatDate(b.endDate)}
                    <div className="muted small">{daysBetween(b.startDate, b.endDate)} days</div>
                  </td>
                  <td className="num">{formatMoney(b.totalPrice)}</td>
                  <td>
                    <StatusBadge status={b.status} />
                  </td>
                  <td className="actions">
                    {b.status === 'PENDING' && (
                      <>
                        <button className="btn btn-sm btn-success" onClick={() => setPending({ booking: b, action: 'approve' })}>
                          Approve
                        </button>
                        <button className="btn btn-sm btn-ghost danger-text" onClick={() => setPending({ booking: b, action: 'reject' })}>
                          Reject
                        </button>
                      </>
                    )}
                    {b.status === 'APPROVED' && (
                      <button className="btn btn-sm btn-ghost danger-text" onClick={() => setPending({ booking: b, action: 'cancel' })}>
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pending)}
        title={pending ? `${ACTION_COPY[pending.action].label}?` : ''}
        message={
          pending && (
            <>
              {ACTION_COPY[pending.action].label} booking #{pending.booking.id} for{' '}
              <strong>
                {pending.booking.make} {pending.booking.model}
              </strong>{' '}
              ({formatDate(pending.booking.startDate)} → {formatDate(pending.booking.endDate)},{' '}
              {formatMoney(pending.booking.totalPrice)})?
              {pending.action === 'approve' && ' Overlapping requests for the same dates can then no longer be approved.'}
            </>
          )
        }
        confirmLabel={pending ? ACTION_COPY[pending.action].label : ''}
        danger={pending ? ACTION_COPY[pending.action].danger : false}
        busy={busy}
        onConfirm={run}
        onClose={() => setPending(null)}
      />
    </>
  );
}
