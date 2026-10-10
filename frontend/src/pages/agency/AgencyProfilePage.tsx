import { useEffect, useState } from 'react';
import { errorMessage } from '../../api/client';
import { agencyProfileApi } from '../../api/endpoints';
import { useAuth } from '../../auth/AuthContext';
import { useProfileImage } from '../../auth/ProfileImageContext';
import { PhotoPicker } from '../../components/PhotoPicker';
import { ErrorBanner, Field, PageHeader, Spinner } from '../../components/ui';
import { formatDateTime } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';

export function AgencyProfilePage() {
  const { user } = useAuth();
  const { notify } = useNotifications();
  const { imageUrl, refresh: refreshImage } = useProfileImage();
  const { data, error, status, loading, reload, setData } = useAsync(() => agencyProfileApi.me(), []);
  const exists = Boolean(data);
  const [form, setForm] = useState({ businessName: '', phoneNumber: '' });
  const [logo, setLogo] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (data) setForm({ businessName: data.businessName ?? '', phoneNumber: data.phoneNumber ?? '' });
  }, [data]);

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    // logoUrl is not sent: the backend ignores it and only the upload endpoint sets it.
    const body = {
      businessName: form.businessName.trim(),
      phoneNumber: form.phoneNumber.trim(),
    };
    try {
      let saved = exists ? await agencyProfileApi.update(body) : await agencyProfileApi.create(body);
      // The logo endpoint requires an existing profile, so upload only after saving the details.
      if (logo) {
        try {
          await agencyProfileApi.uploadLogo(logo);
          saved = await agencyProfileApi.me();
          setLogo(null);
          refreshImage();
        } catch (err) {
          setData(saved);
          notify(`Details saved, but the logo failed to upload: ${errorMessage(err)}`, 'error');
          return;
        }
      }
      setData(saved);
      notify(exists ? 'Agency profile updated.' : 'Agency profile created.', 'success');
    } catch (err) {
      notify(errorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Spinner />;
  if (error && status !== 404) return <ErrorBanner message={error} onRetry={reload} />;

  const saveLabel = exists ? 'Save changes' : 'Create profile';

  return (
    <>
      <PageHeader
        title="Agency profile"
        subtitle={exists ? `Last updated ${formatDateTime(data!.updatedAt)}` : 'Tell customers who you are.'}
      />
      <form className="profile-layout" onSubmit={submit}>
        <aside className="card profile-side">
          <PhotoPicker
            file={logo}
            onChange={setLogo}
            fallback={form.businessName || user?.email || '?'}
            currentUrl={imageUrl}
            noun="logo"
            saveLabel={saveLabel}
            busy={busy}
          />
          <strong>{form.businessName || 'Your agency'}</strong>
          <span className="muted small truncate">{user?.email}</span>
        </aside>
        <div className="card form">
          {!exists && <div className="alert alert-info">No agency profile yet — fill this in to create one.</div>}
          <Field label="Business name">
            <input value={form.businessName} onChange={set('businessName')} required />
          </Field>
          <Field label="Phone number">
            <input type="tel" value={form.phoneNumber} onChange={set('phoneNumber')} placeholder="+973 1xxx xxxx" required />
          </Field>
          <div className="form-actions">
            <button className="btn btn-primary" disabled={busy}>
              {busy ? 'Saving…' : saveLabel}
            </button>
          </div>
        </div>
      </form>
    </>
  );
}
