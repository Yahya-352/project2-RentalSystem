import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { carsApi } from '../../api/endpoints';
import type { CarResponse } from '../../api/types';
import { CarCarousel } from '../../components/CarPhoto';
import { EmptyState, ErrorBanner, PageHeader, Pagination, Spinner } from '../../components/ui';
import { useCatalog } from '../../lib/useCatalog';
import { formatMoney, titleCase } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';

// The backend applies only ONE filter per request (location, else category, else make),
// so the UI lets the user pick one filter at a time instead of pretending to combine them.
type FilterKey = 'location' | 'category' | 'make';

const SORTS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'pricePerDay,asc', label: 'Price: low to high' },
  { value: 'pricePerDay,desc', label: 'Price: high to low' },
  { value: 'year,desc', label: 'Year: newest' },
];

const PAGE_SIZE = 12;

export function CarsPage() {
  const [params, setParams] = useSearchParams();
  const filterKey = (params.get('by') as FilterKey) || 'location';
  const filterValue = params.get('q') ?? '';
  const sort = params.get('sort') ?? SORTS[0].value;
  const page = Number(params.get('page') ?? 0);

  const { data: catalog } = useCatalog();
  const [draft, setDraft] = useState(filterValue);
  useEffect(() => setDraft(filterValue), [filterValue]);

  const update = (next: Record<string, string | null>) => {
    const p = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) {
      if (v === null || v === '') p.delete(k);
      else p.set(k, v);
    }
    setParams(p, { replace: true });
  };

  const { data, error, loading, reload } = useAsync(
    () =>
      carsApi.search({
        [filterKey]: filterValue || undefined,
        page,
        size: PAGE_SIZE,
        sort,
      }),
    [filterKey, filterValue, page, sort],
  );

  const applyText = (e: React.FormEvent) => {
    e.preventDefault();
    update({ q: draft.trim(), page: null });
  };

  return (
    <>
      <PageHeader title="Find a car" subtitle="Browse available cars from rental agencies." />

      <div className="toolbar card">
        <div className="segmented small-seg" role="tablist" aria-label="Filter by">
          {(['location', 'category', 'make'] as const).map((k) => (
            <button
              key={k}
              role="tab"
              aria-selected={filterKey === k}
              className={filterKey === k ? 'active' : ''}
              onClick={() => update({ by: k, q: null, page: null })}
            >
              {titleCase(k)}
            </button>
          ))}
        </div>

        {filterKey === 'location' ? (
          <form onSubmit={applyText} className="search">
            <input
              type="search"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="City, e.g. Riyadh"
              aria-label="Location"
            />
            <button className="btn btn-primary">Search</button>
          </form>
        ) : (
          <select
            value={filterValue}
            onChange={(e) => update({ q: e.target.value, page: null })}
            aria-label={titleCase(filterKey)}
          >
            <option value="">
              {catalog ? `All ${filterKey === 'make' ? 'makes' : 'categories'}` : 'Loading…'}
            </option>
            {(filterKey === 'make' ? (catalog?.makes ?? []) : (catalog?.categories ?? [])).map((o) => (
              <option key={o.id} value={o.name}>
                {o.name}
              </option>
            ))}
          </select>
        )}

        <select value={sort} onChange={(e) => update({ sort: e.target.value, page: null })} aria-label="Sort">
          {SORTS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>

        {filterValue && (
          <button className="btn btn-ghost btn-sm" onClick={() => update({ q: null, page: null })}>
            Clear filter
          </button>
        )}
      </div>

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner label="Loading cars…" />
      ) : data && data.content.length === 0 ? (
        <EmptyState title="No cars match">
          {filterValue ? 'Try a different filter.' : 'No agency has listed a car yet.'}
        </EmptyState>
      ) : (
        data && (
          <>
            <div className={`car-grid ${loading ? 'is-loading' : ''}`}>
              {data.content.map((car) => (
                <CarCard key={car.id} car={car} />
              ))}
            </div>
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onChange={(p) => update({ page: String(p) })}
            />
          </>
        )
      )}
    </>
  );
}

/**
 * With `actions` (agency fleet) the card is not a link. Otherwise the photos swipe and a tap on the
 * photo or the text opens the car. The photo area is kept outside the <Link> so its arrow buttons
 * aren't nested inside a link.
 */
export function CarCard({ car, actions }: { car: CarResponse; actions?: React.ReactNode }) {
  const navigate = useNavigate();
  const href = `/cars/${car.id}`;

  const photos = (
    <CarCarousel
      imageIds={car.imageIds ?? []}
      make={car.make}
      category={car.category}
      variant="card"
      onOpen={actions ? undefined : () => navigate(href)}
    />
  );

  const details = (
    <>
      <div className="car-card-body">
        <div className="row-between">
          <h3>
            {car.make} {car.model}
          </h3>
          <span className="muted">{car.year}</span>
        </div>
        <p className="muted small">📍 {car.location}</p>
        <ul className="specs">
          <li>{titleCase(car.transmission)}</li>
          <li>{titleCase(car.fuelType)}</li>
          <li>{car.seats} seats</li>
        </ul>
        <div className="row-between car-card-foot">
          <span className="price">
            {formatMoney(car.pricePerDay)}
            <small> /day</small>
          </span>
          {!car.available && <span className="badge badge-cancelled">Unavailable</span>}
        </div>
      </div>
    </>
  );

  if (actions) {
    return (
      <article className="car-card">
        {photos}
        {details}
        <div className="car-card-actions">{actions}</div>
      </article>
    );
  }
  return (
    <article className="car-card car-card-link">
      {photos}
      <Link to={href} className="car-card-details">
        {details}
      </Link>
    </article>
  );
}
