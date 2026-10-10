// Bahraini Dinar: 3 decimal places (1 BHD = 1000 fils).
export const CURRENCY = 'BHD';
const currency = new Intl.NumberFormat('en-BH', {
  style: 'currency',
  currency: CURRENCY,
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});
const dateFmt = new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
const dateTimeFmt = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

export const formatMoney = (value: number | string | null | undefined) =>
  value === null || value === undefined ? '—' : currency.format(Number(value));

/** LocalDate strings ("2026-10-07") are parsed as local dates, not UTC midnight. */
export function formatDate(value: string | null | undefined) {
  if (!value) return '—';
  const [y, m, d] = value.slice(0, 10).split('-').map(Number);
  return dateFmt.format(new Date(y, m - 1, d));
}

export const formatDateTime = (value: string | null | undefined) =>
  value ? dateTimeFmt.format(new Date(value)) : '—';

export const titleCase = (value: string) =>
  value.charAt(0).toUpperCase() + value.slice(1).toLowerCase().replace(/_/g, ' ');

/** Today as yyyy-mm-dd in the user's local timezone. */
export function todayIso(offsetDays = 0) {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Same rule as BookingService: ChronoUnit.DAYS.between(start, end). */
export function daysBetween(start: string, end: string) {
  if (!start || !end) return 0;
  const ms = Date.parse(`${end}T00:00:00Z`) - Date.parse(`${start}T00:00:00Z`);
  return Math.round(ms / 86_400_000);
}
