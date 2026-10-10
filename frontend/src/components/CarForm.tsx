import { useState } from 'react';
import type { CarRequest, CarResponse } from '../api/types';
import { FUEL_TYPES, TRANSMISSIONS } from '../lib/constants';
import { titleCase } from '../lib/format';
import { type Catalog, idByName, useCatalog } from '../lib/useCatalog';
import { ErrorBanner, Field, Spinner } from './ui';

interface Props {
  initial?: CarResponse;
  busy?: boolean;
  onSubmit: (body: CarRequest) => void;
  onCancel: () => void;
}

const thisYear = new Date().getFullYear();

/** Loads makes and categories from the backend before showing the form. */
export function CarForm(props: Props) {
  const { data: catalog, error, loading, reload } = useCatalog();

  if (loading) return <Spinner label="Loading makes and categories…" />;
  if (error || !catalog) return <ErrorBanner message={error ?? 'Could not load makes and categories.'} onRetry={reload} />;
  if (catalog.makes.length === 0 || catalog.categories.length === 0) {
    return (
      <div className="alert alert-warn">
        No {catalog.makes.length === 0 ? 'makes' : 'categories'} exist yet. An admin needs to add some before cars can be
        listed.
      </div>
    );
  }
  return <CarFormFields {...props} catalog={catalog} />;
}

// CarResponse carries names, not ids, so an existing car's make/category are matched by name.
function toForm(catalog: Catalog, car?: CarResponse) {
  return {
    makeId: String(car ? (idByName(catalog.makes, car.make) ?? '') : catalog.makes[0].id),
    model: car?.model ?? '',
    categoryId: String(car ? (idByName(catalog.categories, car.category) ?? '') : catalog.categories[0].id),
    location: car?.location ?? '',
    year: String(car?.year ?? thisYear),
    licensePlate: car?.licensePlate ?? '',
    transmission: car?.transmission ?? TRANSMISSIONS[0],
    fuelType: car?.fuelType ?? FUEL_TYPES[0],
    seats: String(car?.seats ?? 5),
    pricePerDay: car ? String(car.pricePerDay) : '',
  };
}

function CarFormFields({ initial, busy, onSubmit, onCancel, catalog }: Props & { catalog: Catalog }) {
  const [form, setForm] = useState(() => toForm(catalog, initial));
  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      makeId: Number(form.makeId),
      model: form.model.trim(),
      categoryId: Number(form.categoryId),
      location: form.location.trim(),
      year: Number(form.year),
      licensePlate: form.licensePlate.trim().toUpperCase(),
      transmission: form.transmission,
      fuelType: form.fuelType,
      seats: Number(form.seats),
      pricePerDay: Number(form.pricePerDay),
    });
  };

  return (
    <form onSubmit={submit} className="form">
      <div className="grid-2">
        <Field label="Make">
          <select value={form.makeId} onChange={set('makeId')} required>
            <option value="" disabled>
              Select a make
            </option>
            {catalog.makes.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Model">
          <input value={form.model} onChange={set('model')} placeholder="e.g. Camry" required />
        </Field>
        <Field label="Category">
          <select value={form.categoryId} onChange={set('categoryId')} required>
            <option value="" disabled>
              Select a category
            </option>
            {catalog.categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Location">
          <input value={form.location} onChange={set('location')} placeholder="e.g. Riyadh" required />
        </Field>
        <Field label="Year">
          <input type="number" min={1980} max={2100} value={form.year} onChange={set('year')} required />
        </Field>
        <Field label="License plate">
          <input value={form.licensePlate} onChange={set('licensePlate')} placeholder="ABC-1234" required />
        </Field>
        <Field label="Transmission">
          <select value={form.transmission} onChange={set('transmission')}>
            {TRANSMISSIONS.map((t) => (
              <option key={t} value={t}>
                {titleCase(t)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Fuel type">
          <select value={form.fuelType} onChange={set('fuelType')}>
            {FUEL_TYPES.map((f) => (
              <option key={f} value={f}>
                {titleCase(f)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Seats">
          <input type="number" min={1} max={20} value={form.seats} onChange={set('seats')} required />
        </Field>
        <Field label="Price per day (BHD)">
          <input
            type="number"
            min={0.001}
            step="0.001"
            value={form.pricePerDay}
            onChange={set('pricePerDay')}
            placeholder="0.000"
            required
          />
        </Field>
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}>
          Cancel
        </button>
        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Saving…' : initial ? 'Save changes' : 'Add car'}
        </button>
      </div>
    </form>
  );
}
