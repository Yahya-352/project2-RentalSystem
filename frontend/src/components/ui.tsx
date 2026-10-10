import { useEffect, useRef } from 'react';
import type { BookingStatus } from '../api/types';
import { titleCase } from '../lib/format';

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="spinner-wrap" role="status">
      <span className="spinner" aria-hidden="true" />
      <span className="muted">{label}</span>
    </div>
  );
}

export function ErrorBanner({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="alert alert-error" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button className="btn btn-sm btn-ghost" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, children }: { title: string; children?: React.ReactNode }) {
  return (
    <div className="empty">
      <div className="empty-icon" aria-hidden="true">
        ◌
      </div>
      <h3>{title}</h3>
      {children && <div className="muted">{children}</div>}
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: React.ReactNode }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}

export function StatusBadge({ status }: { status: BookingStatus | string }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{titleCase(status)}</span>;
}

export function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}

export function Modal({
  title,
  open,
  onClose,
  children,
  width = 560,
}: {
  title: string;
  open: boolean;
  onClose: () => void;
  children: React.ReactNode;
  width?: number;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      className="modal"
      style={{ maxWidth: width }}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClick={(e) => {
        if (e.target === ref.current) onClose(); // click on backdrop
      }}
    >
      {open && (
        <div className="modal-body">
          <div className="modal-head">
            <h2>{title}</h2>
            <button className="icon-btn" onClick={onClose} aria-label="Close">
              ×
            </button>
          </div>
          {children}
        </div>
      )}
    </dialog>
  );
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  danger,
  busy,
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  message: React.ReactNode;
  confirmLabel?: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <Modal open={open} title={title} onClose={onClose} width={420}>
      <p>{message}</p>
      <div className="form-actions">
        <button className="btn btn-ghost" onClick={onClose} disabled={busy}>
          Keep it
        </button>
        <button className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm} disabled={busy}>
          {busy ? 'Working…' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return <p className="muted pager-summary">{totalElements} result(s)</p>;
  return (
    <nav className="pager" aria-label="Pagination">
      <button className="btn btn-sm btn-ghost" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        ← Prev
      </button>
      <span className="muted">
        Page {page + 1} of {totalPages} · {totalElements} results
      </span>
      <button className="btn btn-sm btn-ghost" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next →
      </button>
    </nav>
  );
}

/** Stylised placeholder: the backend stores car images but exposes no endpoint to read them. */
export function CarArt({ make, category }: { make: string; category: string }) {
  const hue = [...make].reduce((h, c) => (h * 31 + c.charCodeAt(0)) % 360, 7);
  return (
    <div className="car-art" style={{ '--hue': hue } as React.CSSProperties} aria-hidden="true">
      <svg viewBox="0 0 120 48" className="car-svg">
        <path
          d="M8 34c0-5 3-8 8-9l14-3 12-10c3-2 6-3 10-3h22c4 0 7 1 10 4l9 9 13 2c5 1 8 4 8 9v3c0 2-2 4-4 4h-6a10 10 0 0 0-20 0H42a10 10 0 0 0-20 0h-10c-2 0-4-2-4-4z"
          fill="currentColor"
        />
        <circle cx="32" cy="41" r="7" className="wheel" />
        <circle cx="94" cy="41" r="7" className="wheel" />
      </svg>
      <span className="car-art-tag">{category}</span>
    </div>
  );
}
