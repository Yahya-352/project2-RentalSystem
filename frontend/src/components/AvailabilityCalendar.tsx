import { useMemo, useState } from 'react';
import type { BookedDateRange, IsoDate } from '../api/types';
import { todayIso } from '../lib/format';

// Dates are handled as "yyyy-mm-dd" strings: they compare correctly as plain strings
// and match what the backend sends and expects.

const pad = (n: number) => String(n).padStart(2, '0');
const isoOf = (year: number, month0: number, day: number) => `${year}-${pad(month0 + 1)}-${pad(day)}`;

const WEEKDAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
const monthFmt = new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' });

/** A booking blocks its start day up to, but not including, its end day (the return day is free). */
export function isBooked(day: IsoDate, booked: BookedDateRange[]) {
  return booked.some((r) => day >= r.startDate && day < r.endDate);
}

/** Same overlap rule as the backend's isCarAvailable: clash if r.start < end && r.end > start. */
export function isRangeFree(start: IsoDate, end: IsoDate, booked: BookedDateRange[]) {
  return booked.every((r) => !(r.startDate < end && r.endDate > start));
}

interface Props {
  booked: BookedDateRange[];
  /** '' while not chosen. */
  start: IsoDate;
  end: IsoDate;
  onChange: (start: IsoDate, end: IsoDate) => void;
}

/**
 * Month view where the user clicks a pick-up day, then a return day. Taken days are disabled;
 * a taken day can still be picked as the return day, because the car is handed over that morning.
 */
export function AvailabilityCalendar({ booked, start, end, onChange }: Props) {
  const today = todayIso();
  const [view, setView] = useState(() => {
    const [y, m] = (start || today).split('-').map(Number);
    return { year: y, month0: m - 1 };
  });

  const cells = useMemo(() => {
    const firstWeekday = new Date(view.year, view.month0, 1).getDay();
    const daysInMonth = new Date(view.year, view.month0 + 1, 0).getDate();
    const blanks: (IsoDate | null)[] = Array(firstWeekday).fill(null);
    const days = Array.from({ length: daysInMonth }, (_, i) => isoOf(view.year, view.month0, i + 1));
    return [...blanks, ...days];
  }, [view]);

  const choosingReturn = Boolean(start) && !end;
  const asReturnDay = (day: IsoDate) => choosingReturn && day > start && isRangeFree(start, day, booked);
  const canPick = (day: IsoDate) => day >= today && (asReturnDay(day) || !isBooked(day, booked));

  const pick = (day: IsoDate) => {
    if (asReturnDay(day)) onChange(start, day);
    else onChange(day, ''); // a new pick-up day; the return day is chosen next
  };

  const shift = (delta: number) =>
    setView((v) => {
      const d = new Date(v.year, v.month0 + delta, 1);
      return { year: d.getFullYear(), month0: d.getMonth() };
    });
  const [ty, tm] = today.split('-').map(Number);
  const atCurrentMonth = view.year === ty && view.month0 === tm - 1;

  return (
    <div className="cal">
      <div className="cal-head">
        <button type="button" className="cal-nav" onClick={() => shift(-1)} disabled={atCurrentMonth} aria-label="Previous month">
          ‹
        </button>
        <span>{monthFmt.format(new Date(view.year, view.month0, 1))}</span>
        <button type="button" className="cal-nav" onClick={() => shift(1)} aria-label="Next month">
          ›
        </button>
      </div>

      <div className="cal-grid">
        {WEEKDAYS.map((w) => (
          <span key={w} className="cal-weekday">
            {w}
          </span>
        ))}
        {cells.map((day, i) => {
          if (!day) return <span key={`blank-${i}`} />;
          const booked_ = isBooked(day, booked);
          const classes = [
            'cal-day',
            day < today && 'is-past',
            booked_ && 'is-booked',
            day === today && 'is-today',
            (day === start || day === end) && 'is-selected',
            start && end && day > start && day < end && 'in-range',
          ]
            .filter(Boolean)
            .join(' ');
          return (
            <button
              key={day}
              type="button"
              className={classes}
              disabled={!canPick(day)}
              onClick={() => pick(day)}
              aria-pressed={day === start || day === end}
              aria-label={`${day}${booked_ ? ' (taken)' : ''}`}
            >
              {Number(day.slice(8))}
            </button>
          );
        })}
      </div>

      <div className="cal-legend">
        <span className="lg-free">Free</span>
        <span className="lg-booked">Taken</span>
        <span className="lg-selected">Your dates</span>
      </div>
    </div>
  );
}
