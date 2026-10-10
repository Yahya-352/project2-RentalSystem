import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { bookingsApi, carsApi } from '../../api/endpoints';
import type { CarResponse } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { CarGallery } from '../../components/CarPhoto';
import { EmptyState, ErrorBanner, Field, Spinner } from '../../components/ui';
import { daysBetween, formatMoney, todayIso, titleCase } from '../../lib/format';
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
              📍 {car.location} · Listed by <strong>{car.ownerUsername}</strong>
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
  const [start, setStart] = useState(todayIso(1));
  const [end, setEnd] = useState(todayIso(3));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const days = daysBetween(start, end);
  const total = days > 0 ? days * Number(car.pricePerDay) : 0;

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
    if (days <= 0) {
      setError('The return date must be after the pick-up date.');
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
      <div className="grid-2">
        <Field label="Pick-up">
          <input
            type="date"
            value={start}
            min={todayIso()}
            onChange={(e) => {
              setStart(e.target.value);
              if (e.target.value >= end) setEnd(e.target.value);
            }}
            required
          />
        </Field>
        <Field label="Return">
          <input type="date" value={end} min={start} onChange={(e) => setEnd(e.target.value)} required />
        </Field>
      </div>
      <div className="summary">
        <div className="row-between">
          <span className="muted">
            {formatMoney(car.pricePerDay)} × {Math.max(days, 0)} day{days === 1 ? '' : 's'}
          </span>
          <strong>{formatMoney(total)}</strong>
        </div>
      </div>
      <button className="btn btn-primary btn-block" disabled={busy || days <= 0}>
        {busy ? 'Requesting…' : 'Request booking'}
      </button>
      <p className="muted small center">You won't be charged until the agency approves.</p>
    </form>
  );
}
