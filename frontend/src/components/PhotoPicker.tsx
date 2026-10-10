import { useEffect, useRef, useState } from 'react';
import { useNotifications } from '../notifications/NotificationContext';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024; // spring.servlet.multipart.max-file-size
const ALLOWED_TYPES = ['image/png', 'image/jpeg'];

interface Props {
  /** The file chosen but not yet uploaded. */
  file: File | null;
  onChange: (file: File | null) => void;
  /** Letter shown when there is nothing to show. */
  fallback: string;
  /** The image already saved on the server (an object URL), shown when no new file is chosen. */
  currentUrl?: string | null;
  noun: string;
  saveLabel: string;
  busy?: boolean;
}

/**
 * Picks a JPG/PNG with a local preview. Uploading is left to the parent, which does it on save
 * (the upload endpoints require the profile to exist first).
 */
export function PhotoPicker({ file, onChange, fallback, currentUrl, noun, saveLabel, busy }: Props) {
  const { notify } = useNotifications();
  const input = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);

  useEffect(() => {
    if (!file) {
      setPreview(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const pick = (chosen: File | undefined) => {
    if (input.current) input.current.value = '';
    if (!chosen) return;
    if (!ALLOWED_TYPES.includes(chosen.type)) {
      notify('Only JPG and PNG images are allowed.', 'error');
      return;
    }
    if (chosen.size > MAX_IMAGE_BYTES) {
      notify('Images must be 5 MB or smaller.', 'error');
      return;
    }
    onChange(chosen);
  };

  const shown = preview ?? currentUrl ?? null;
  const open = () => input.current?.click();

  return (
    <>
      <button type="button" className="photo-picker" onClick={open} disabled={busy} aria-label={`Choose ${noun}`}>
        {shown ? (
          <img src={shown} alt="" className="avatar avatar-lg avatar-img" />
        ) : (
          <span className="avatar avatar-lg">{fallback.charAt(0).toUpperCase()}</span>
        )}
        <span className="photo-picker-overlay">Change</span>
      </button>
      <input ref={input} type="file" accept={ALLOWED_TYPES.join(',')} hidden onChange={(e) => pick(e.target.files?.[0])} />

      <div className="photo-actions">
        <button type="button" className="btn btn-ghost btn-sm" onClick={open} disabled={busy}>
          {file ? 'Choose another' : currentUrl ? `Change ${noun}` : `Upload ${noun}`}
        </button>
        {file && (
          <button type="button" className="link-btn small" onClick={() => onChange(null)} disabled={busy}>
            Undo
          </button>
        )}
      </div>
      <span className="muted small">
        {file ? `${file.name} — saved when you click “${saveLabel}”` : 'JPG or PNG, up to 5 MB'}
      </span>
    </>
  );
}
