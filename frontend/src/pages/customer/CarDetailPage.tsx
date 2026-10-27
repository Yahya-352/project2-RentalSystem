import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ApiError, errorMessage } from '../../api/client';
import { bookingsApi, carsApi } from '../../api/endpoints';
import type { CarResponse } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { AvailabilityCalendar, isRangeFree } from '../../components/AvailabilityCalendar';
import { CarGallery } from '../../components/CarPhoto';
import { EmptyState, ErrorBanner, Spinner } from '../../components/ui';
import { daysBetween, formatDate, formatMoney, formatPlace, todayIso, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';

export function CarDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const { data: car, error, status, loading, reload } = useAsync(() => carsApi.get(Number(id)), [id]);

  if (loading) return <Spinner />;
  if (status === 404) return <EmptyState title="Car not found">It may have been removed by its agency.</EmptyState>;
  if (error || !car) return <ErrorBanner message={error ?? 'Could not load car.'} onRetry={reload} />;

  return (
    <>
      <Link to="/cars" className="back-link">
        ← Back to cars
      </Link>
      <div className="detail">
        <section className="card detail-main">
          <CarGallery imageIds={car.imageIds ?? []} make={car.make} category={car.category} />
          <div className="detail-body">
            <h1>
              {car.make} {car.model} <span className="muted">{car.year}</span>
            </h1>
            <p className="muted">
              📍 {formatPlace(car.city, car.country)} · Listed by <strong>{car.ownerUsername}</strong>
            </p>
            <dl className="spec-grid">
              <div>
                <dt>Category</dt>
                <dd>{car.category}</dd>
              </div>
              <div>
                <dt>Transmission</dt>
                <dd>{titleCase(car.transmission)}</dd>
              </div>
              <div>
                <dt>Fuel</dt>
                <dd>{titleCase(car.fuelType)}</dd>
              </div>
              <div>
                <dt>Seats</dt>
                <dd>{car.seats}</dd>
              </div>
              <div>
                <dt>Plate</dt>
                <dd>{car.licensePlate}</dd>
              </div>
              <div>
                <dt>Daily rate</dt>
                <dd>{formatMoney(car.pricePerDay)}</dd>
              </div>
            </dl>
          </div>
        </section>

        <aside className="card booking-box">
          {user?.role === 'CUSTOMER' ? (
            <BookingForm car={car} />
          ) : (
            <>
              <h2>{formatMoney(car.pricePerDay)} / day</h2>
              <p className="muted">Only customer accounts can book cars.</p>
            </>
          )}
        </aside>
      </div>
    </>
  );
}

function BookingForm({ car }: { car: CarResponse }) {
  const navigate = useNavigate();
  const { notify } = useNotifications();
  // Dates chosen on the search page arrive as ?start=...&end=... and start out selected.
  const [params] = useSearchParams();
  const [start, setStart] = useState(() => {
    const s = params.get('start') ?? '';
    return s >= todayIso() ? s : '';
  });
  const [end, setEnd] = useState(() => {
    const e = params.get('end') ?? '';
    return start && e > start ? e : '';
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const taken = useAsync(() => bookingsApi.bookedDates(car.id), [car.id]);
  const booked = taken.data ?? [];

  const days = start && end ? daysBetween(start, end) : 0;
  const total = days > 0 ? days * Number(car.pricePerDay) : 0;
  // Pre-selected dates (or a page that went stale) may overlap a booking approved meanwhile.
  const clash = Boolean(start && end) && !isRangeFree(start, end, booked);

  if (!car.available) {
    return (
      <>
        <h2>{formatMoney(car.pricePerDay)} / day</h2>
        <div className="alert alert-warn">This car is currently not available for booking.</div>
      </>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!start || !end || days <= 0) {
      setError('Pick a pick-up day and then a return day on the calendar.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await bookingsApi.create({ carId: car.id, startDate: start, endDate: end });
      notify('Booking requested — the agency will review it shortly.', 'success');
      navigate('/bookings');
    } catch (err) {
      setError(errorMessage(err));
      // 409: someone else's booking was approved for these days after the calendar loaded.
      if (err instanceof ApiError && err.status === 409) taken.reload();
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit} className="form">
      <h2>
        {formatMoney(car.pricePerDay)} <small className="muted">/ day</small>
      </h2>
      {error && <div className="alert alert-error">{error}</div>}
      {taken.error && (
        <div className="alert alert-warn">
          Couldn't load the taken dates. You can still request; the agency checks availability.
        </div>
      )}
      {clash && <div className="alert alert-error">These dates overlap an existing booking. Pick other days.</div>}

      {taken.loading && !taken.data ? (
        <Spinner label="Loading availability…" />
      ) : (
        <AvailabilityCalendar
          booked={booked}
          start={start}
          end={end}
          onChange={(s, e) => {
            setStart(s);
            setEnd(e);
            setError(null);
          }}
        />
      )}
      <p className="muted small">
        {!start ? 'Click your pick-up day.' : !end ? 'Now click your return day.' : 'Click any day to start over.'}
      </p>

      <dl className="spec-grid">
        <div>
          <dt>Pick-up</dt>
          <dd>{formatDate(start)}</dd>
        </div>
        <div>
          <dt>Return</dt>
          <dd>{formatDate(end)}</dd>
        </div>
      </dl>
      <div className="summary">
        <div className="row-between">
          <span className="muted">
            {formatMoney(car.pricePerDay)} × {Math.max(days, 0)} day{days === 1 ? '' : 's'}
          </span>
          <strong>{formatMoney(total)}</strong>
        </div>
      </div>
      <button className="btn btn-primary btn-block" disabled={busy || days <= 0 || clash}>
        {busy ? 'Requesting…' : 'Request booking'}
      </button>
      <p className="muted small center">You won't be charged until the agency approves.</p>
    </form>
  );
}
