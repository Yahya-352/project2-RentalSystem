import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { type CarSearch, carsApi } from '../../api/endpoints';
import type { CarResponse, FuelType, TransmissionType } from '../../api/types';
import { CarCarousel } from '../../components/CarPhoto';
import { EmptyState, ErrorBanner, Field, PageHeader, Pagination, Spinner } from '../../components/ui';
import { FUEL_TYPES, TRANSMISSIONS } from '../../lib/constants';
import { useCatalog } from '../../lib/useCatalog';
import { useCities, useCountries } from '../../lib/useLocations';
import { formatDate, formatMoney, formatPlace, titleCase, todayIso } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';

// Every filter is optional and the backend combines all the ones that are sent.
// They live in the URL, so a search survives a reload and can be shared.
const FILTER_KEYS = [
  'startDate',
  'endDate',
  'countryId',
  'cityId',
  'categoryId',
  'makeId',
  'transmission',
  'fuelType',
  'minSeats',
  'minPrice',
  'maxPrice',
] as const;
type FilterKey = (typeof FILTER_KEYS)[number];
type Filters = Record<FilterKey, string>;

/** The filters kept behind the "More filters" button; the rest are always in the search bar. */
const MORE_KEYS: FilterKey[] = ['categoryId', 'makeId', 'transmission', 'fuelType', 'minSeats', 'minPrice', 'maxPrice'];

const datesChosenOf = (f: Filters) => Boolean(f.startDate && f.endDate);

const readFilters = (params: URLSearchParams) =>
  Object.fromEntries(FILTER_KEYS.map((k) => [k, params.get(k) ?? ''])) as Filters;

const num = (value: string) => (value === '' ? undefined : Number(value));

function toSearch(f: Filters): CarSearch {
  return {
    startDate: f.startDate || undefined,
    endDate: f.endDate || undefined,
    countryId: num(f.countryId),
    cityId: num(f.cityId),
    categoryId: num(f.categoryId),
    makeId: num(f.makeId),
    transmission: (f.transmission || undefined) as TransmissionType | undefined,
    fuelType: (f.fuelType || undefined) as FuelType | undefined,
    minSeats: num(f.minSeats),
    minPrice: num(f.minPrice),
    maxPrice: num(f.maxPrice),
  };
}

/** The same rules the backend enforces, checked here so the user doesn't wait for a 400. */
function validate(f: Filters): string | null {
  if (Boolean(f.startDate) !== Boolean(f.endDate)) return 'Pick both a pick-up and a return date.';
  if (f.startDate && f.startDate < todayIso()) return "The pick-up date can't be in the past.";
  if (f.startDate && f.endDate <= f.startDate) return 'The return date must be after the pick-up date.';
  if (f.minPrice && f.maxPrice && Number(f.minPrice) > Number(f.maxPrice)) {
    return "The minimum price can't be higher than the maximum.";
  }
  return null;
}

const SORTS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'pricePerDay,asc', label: 'Price: low to high' },
  { value: 'pricePerDay,desc', label: 'Price: high to low' },
  { value: 'year,desc', label: 'Year: newest' },
];

const PAGE_SIZE = 12;

export function CarsPage() {
  const [params, setParams] = useSearchParams();
  const applied = readFilters(params);
  const appliedKey = FILTER_KEYS.map((k) => applied[k]).join('|');
  const sort = params.get('sort') ?? SORTS[0].value;
  const page = Number(params.get('page') ?? 0);

  const { data: catalog } = useCatalog();
  const { data: countries } = useCountries();

  // The form edits a draft; nothing is searched until the user presses Search.
  const [draft, setDraft] = useState<Filters>(applied);
  const [formError, setFormError] = useState<string | null>(null);
  useEffect(() => setDraft(readFilters(params)), [appliedKey]); // eslint-disable-line react-hooks/exhaustive-deps
  const cities = useCities(num(draft.countryId));

  const update = (next: Record<string, string | null>) => {
    const p = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) {
      if (v === null || v === '') p.delete(k);
      else p.set(k, v);
    }
    setParams(p, { replace: true });
  };

  const { data, error, loading, reload } = useAsync(
    () => carsApi.search({ ...toSearch(applied), page, size: PAGE_SIZE, sort }),
    [appliedKey, page, sort],
  );

  const set = (key: FilterKey) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setDraft((d) => ({ ...d, [key]: e.target.value }));

  const apply = (e: React.FormEvent) => {
    e.preventDefault();
    const problem = validate(draft);
    setFormError(problem);
    if (!problem) update({ ...draft, page: null });
  };

  const clear = () => {
    setFormError(null);
    update({ ...Object.fromEntries(FILTER_KEYS.map((k) => [k, null])), page: null });
  };

  const activeCount = FILTER_KEYS.filter((k) => applied[k] !== '').length;
  const [moreOpen, setMoreOpen] = useState(false);
  const moreCount = MORE_KEYS.filter((k) => applied[k] !== '').length;

  // One removable chip per applied filter. Dates go together; removing the country also removes the city.
  const nameOf = (list: { id: number; name: string }[] | undefined, id: string) =>
    list?.find((x) => String(x.id) === id)?.name;
  const chips: { label: string; keys: FilterKey[] }[] = [];
  if (datesChosenOf(applied)) {
    chips.push({ label: `${formatDate(applied.startDate)} → ${formatDate(applied.endDate)}`, keys: ['startDate', 'endDate'] });
  }
  if (applied.countryId) {
    chips.push({ label: nameOf(countries, applied.countryId) ?? 'Country', keys: ['countryId', 'cityId'] });
  }
  if (applied.cityId) chips.push({ label: nameOf(cities.data, applied.cityId) ?? 'City', keys: ['cityId'] });
  if (applied.categoryId) {
    chips.push({ label: nameOf(catalog?.categories, applied.categoryId) ?? 'Category', keys: ['categoryId'] });
  }
  if (applied.makeId) chips.push({ label: nameOf(catalog?.makes, applied.makeId) ?? 'Make', keys: ['makeId'] });
  if (applied.transmission) chips.push({ label: titleCase(applied.transmission), keys: ['transmission'] });
  if (applied.fuelType) chips.push({ label: titleCase(applied.fuelType), keys: ['fuelType'] });
  if (applied.minSeats) chips.push({ label: `${applied.minSeats}+ seats`, keys: ['minSeats'] });
  if (applied.minPrice) chips.push({ label: `From ${formatMoney(applied.minPrice)}/day`, keys: ['minPrice'] });
  if (applied.maxPrice) chips.push({ label: `Up to ${formatMoney(applied.maxPrice)}/day`, keys: ['maxPrice'] });
  const datesChosen = datesChosenOf(applied);
  // Carry the dates to the car page so its booking calendar starts with them selected.
  const carLinkSearch = datesChosen ? `?start=${applied.startDate}&end=${applied.endDate}` : '';

  return (
    <>
      <PageHeader
        title="Find a car"
        subtitle={
          datesChosen
            ? `Cars free from ${formatDate(applied.startDate)} to ${formatDate(applied.endDate)}.`
            : 'Browse available cars from rental agencies.'
        }
      />

      <form onSubmit={apply}>
        {/* Main bar: where and when */}
        <div className="card search-bar">
          <Field label="Country">
            <select
              value={draft.countryId}
              onChange={(e) => setDraft((d) => ({ ...d, countryId: e.target.value, cityId: '' }))}
            >
              <option value="">{countries ? 'Any country' : 'Loading…'}</option>
              {(countries ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="City">
            <select value={draft.cityId} onChange={set('cityId')} disabled={!draft.countryId || cities.loading}>
              <option value="">
                {!draft.countryId ? 'Pick a country first' : cities.loading ? 'Loading…' : 'Any city'}
              </option>
              {!cities.loading &&
                (cities.data ?? []).map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
            </select>
          </Field>
          <Field label="Pick-up">
            <input
              type="date"
              value={draft.startDate}
              min={todayIso()}
              onChange={(e) => {
                const startDate = e.target.value;
                // A return date on or before the new pick-up date no longer makes sense.
                setDraft((d) => ({ ...d, startDate, endDate: d.endDate && d.endDate <= startDate ? '' : d.endDate }));
              }}
            />
          </Field>
          <Field label="Return">
            <input
              type="date"
              value={draft.endDate}
              min={draft.startDate || todayIso(1)}
              onChange={set('endDate')}
            />
          </Field>
          <button className="btn btn-primary search-bar-btn">Search</button>
        </div>

        <div className="search-toolbar">
          <button
            type="button"
            className={`btn btn-ghost btn-sm ${moreOpen ? 'is-open' : ''}`}
            onClick={() => setMoreOpen((o) => !o)}
            aria-expanded={moreOpen}
            aria-controls="more-filters"
          >
            ⚙ More filters{moreCount > 0 && ` (${moreCount})`}
          </button>
          <span className="spacer" />
          <select value={sort} onChange={(e) => update({ sort: e.target.value, page: null })} aria-label="Sort">
            {SORTS.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </div>

        {/* Secondary filters, hidden until "More filters" is pressed */}
        {moreOpen && (
          <div id="more-filters" className="card more-filters">
            <Field label="Category">
              <select value={draft.categoryId} onChange={set('categoryId')}>
                <option value="">{catalog ? 'Any category' : 'Loading…'}</option>
                {(catalog?.categories ?? []).map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Make">
              <select value={draft.makeId} onChange={set('makeId')}>
                <option value="">{catalog ? 'Any make' : 'Loading…'}</option>
                {(catalog?.makes ?? []).map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Transmission">
              <select value={draft.transmission} onChange={set('transmission')}>
                <option value="">Any</option>
                {TRANSMISSIONS.map((t) => (
                  <option key={t} value={t}>
                    {titleCase(t)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Fuel">
              <select value={draft.fuelType} onChange={set('fuelType')}>
                <option value="">Any</option>
                {FUEL_TYPES.map((f) => (
                  <option key={f} value={f}>
                    {titleCase(f)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Seats (at least)">
              <input type="number" min={1} max={20} value={draft.minSeats} onChange={set('minSeats')} placeholder="Any" />
            </Field>
            <Field label="Min price / day">
              <input
                type="number"
                min={0}
                step="0.001"
                value={draft.minPrice}
                onChange={set('minPrice')}
                placeholder="0.000"
              />
            </Field>
            <Field label="Max price / day">
              <input
                type="number"
                min={0.001}
                step="0.001"
                value={draft.maxPrice}
                onChange={set('maxPrice')}
                placeholder="Any"
              />
            </Field>
            <div className="more-filters-actions">
              <button className="btn btn-primary btn-sm">Apply filters</button>
            </div>
          </div>
        )}

        {formError && <div className="alert alert-error">{formError}</div>}

        {/* What is currently applied; × removes one filter */}
        {chips.length > 0 && (
          <div className="active-filters">
            {chips.map((c) => (
              <button
                key={c.label}
                type="button"
                className="filter-chip"
                onClick={() => update({ ...Object.fromEntries(c.keys.map((k) => [k, null])), page: null })}
                aria-label={`Remove filter ${c.label}`}
              >
                {c.label} <span aria-hidden="true">×</span>
              </button>
            ))}
            <button type="button" className="link-btn small" onClick={clear}>
              Clear filters ({activeCount})
            </button>
          </div>
        )}
      </form>

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner label="Loading cars…" />
      ) : data && data.content.length === 0 ? (
        <EmptyState title="No cars match">
          {activeCount > 0 ? 'Try changing or clearing some filters.' : 'No agency has listed a car yet.'}
        </EmptyState>
      ) : (
        data && (
          <>
            <div className={`car-grid ${loading ? 'is-loading' : ''}`}>
              {data.content.map((car) => (
                <CarCard key={car.id} car={car} linkSearch={carLinkSearch} />
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
export function CarCard({
  car,
  actions,
  linkSearch = '',
}: {
  car: CarResponse;
  actions?: React.ReactNode;
  /** Appended to the car's link, e.g. "?start=...&end=..." to pre-select dates. */
  linkSearch?: string;
}) {
  const navigate = useNavigate();
  const href = `/cars/${car.id}${linkSearch}`;

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
        <p className="muted small">📍 {formatPlace(car.city, car.country)}</p>
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
