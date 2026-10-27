import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { bookingsApi, paymentsApi } from '../../api/endpoints';
import type { BookingResponse, BookingStatus } from '../../api/types';
import { ConfirmDialog, EmptyState, ErrorBanner, PageHeader, PaidBadge, Spinner, StatusBadge } from '../../components/ui';
import { BOOKING_STATUSES } from '../../lib/constants';
import { daysBetween, formatDate, formatDateTime, formatMoney, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';

/** Statuses the backend still allows to be cancelled (see BookingService.cancelBooking). */
export const isCancellable = (s: BookingStatus) => s === 'PENDING' || s === 'APPROVED';

/** Only approved bookings can be paid (see StripeService.checkout), and only once. */
const isPayable = (b: BookingResponse) => b.status === 'APPROVED' && !b.paid;

export function MyBookingsPage() {
  const { notify, bookingVersion } = useNotifications();
  const { data, error, loading, reload, setData } = useAsync(() => bookingsApi.mine(), []);
  const [filter, setFilter] = useState<BookingStatus | 'ALL'>('ALL');
  const [cancelling, setCancelling] = useState<BookingResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [payingId, setPayingId] = useState<number | null>(null);

  // The card form is Stripe's hosted Checkout page; we only create the session and send the browser there.
  // Stripe then returns the customer to /payment/success or /payment/cancel.
  const pay = async (booking: BookingResponse) => {
    setPayingId(booking.id);
    try {
      const session = await paymentsApi.checkout(booking.id);
      window.location.assign(session.sessionUrl);
    } catch (err) {
      notify(errorMessage(err), 'error');
      setPayingId(null);
    }
  };

  // Refresh when a live event (approve/reject) arrives.
  useEffect(() => {
    if (bookingVersion > 0) reload();
  }, [bookingVersion, reload]);

  const bookings = useMemo(
    () =>
      [...(data ?? [])]
        .filter((b) => filter === 'ALL' || b.status === filter)
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [data, filter],
  );

  const cancel = async () => {
    if (!cancelling) return;
    setBusy(true);
    try {
      const updated = await bookingsApi.cancel(cancelling.id);
      setData((list) => list?.map((b) => (b.id === updated.id ? updated : b)));
      notify('Booking cancelled.', 'success');
      setCancelling(null);
    } catch (err) {
      notify(errorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="My bookings"
        subtitle="Track the status of your rental requests."
        actions={
          <Link to="/cars" className="btn btn-primary">
            Book a car
          </Link>
        }
      />

      <StatusFilter value={filter} onChange={setFilter} bookings={data ?? []} />

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner />
      ) : bookings.length === 0 ? (
        <EmptyState title={filter === 'ALL' ? 'No bookings yet' : `No ${filter.toLowerCase()} bookings`}>
          {filter === 'ALL' && <Link to="/cars">Find a car to rent →</Link>}
        </EmptyState>
      ) : (
        <div className="booking-list">
          {bookings.map((b) => (
            <article key={b.id} className="card booking-row">
              <div className="booking-main">
                <div className="row-gap">
                  <h3>
                    <Link to={`/cars/${b.carId}`}>
                      {b.make} {b.model}
                    </Link>
                  </h3>
                  <StatusBadge status={b.status} />
                  <PaidBadge paid={b.paid} />
                </div>
                <p className="muted small">
                  {formatDate(b.startDate)} → {formatDate(b.endDate)} · {daysBetween(b.startDate, b.endDate)} days ·
                  requested {formatDateTime(b.createdAt)}
                </p>
              </div>
              <div className="booking-side">
                <strong className="price">{formatMoney(b.totalPrice)}</strong>
                {isPayable(b) && (
                  <button className="btn btn-sm btn-primary" onClick={() => pay(b)} disabled={payingId !== null}>
                    {payingId === b.id ? 'Opening payment…' : 'Pay now'}
                  </button>
                )}
                {isCancellable(b.status) && (
                  <button className="btn btn-sm btn-ghost danger-text" onClick={() => setCancelling(b)}>
                    Cancel
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={Boolean(cancelling)}
        title="Cancel booking?"
        message={
          cancelling && (
            <>
              Cancel your booking for{' '}
              <strong>
                {cancelling.make} {cancelling.model}
              </strong>{' '}
              from {formatDate(cancelling.startDate)} to {formatDate(cancelling.endDate)}? This can't be undone.
              {cancelling.paid && (
                <>
                  {' '}
                  <strong>You've already paid for it:</strong> refunds aren't automatic, so contact the agency.
                </>
              )}
            </>
          )
        }
        confirmLabel="Cancel booking"
        danger
        busy={busy}
        onConfirm={cancel}
        onClose={() => setCancelling(null)}
      />
    </>
  );
}

export function StatusFilter({
  value,
  onChange,
  bookings,
}: {
  value: BookingStatus | 'ALL';
  onChange: (v: BookingStatus | 'ALL') => void;
  bookings: BookingResponse[];
}) {
  const count = (s: BookingStatus | 'ALL') => (s === 'ALL' ? bookings.length : bookings.filter((b) => b.status === s).length);
  return (
    <div className="chips" role="tablist" aria-label="Filter by status">
      {(['ALL', ...BOOKING_STATUSES] as const).map((s) => (
        <button
          key={s}
          role="tab"
          aria-selected={value === s}
          className={`chip ${value === s ? 'active' : ''}`}
          onClick={() => onChange(s)}
        >
          {s === 'ALL' ? 'All' : titleCase(s)} <span className="chip-count">{count(s)}</span>
        </button>
      ))}
    </div>
  );
}
