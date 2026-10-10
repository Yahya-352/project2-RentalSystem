import { useEffect, useState } from 'react';
import { errorMessage } from '../../api/client';
import { customerProfileApi } from '../../api/endpoints';
import type { UserProfileRequest } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { useProfileImage } from '../../auth/ProfileImageContext';
import { PhotoPicker } from '../../components/PhotoPicker';
import { ErrorBanner, Field, PageHeader, Spinner } from '../../components/ui';
import { formatDateTime } from '../../lib/format';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';

const EMPTY: UserProfileRequest = {
  firstName: '',
  lastName: '',
  phoneNumber: '',
  address: '',
  licenseNumber: '',
  profilePictureUrl: '',
  licenseFileUrl: '',
};

export function CustomerProfilePage() {
  const { user } = useAuth();
  const { notify } = useNotifications();
  const { imageUrl, refresh: refreshImage } = useProfileImage();
  // A 404 simply means the profile hasn't been created yet.
  const { data, error, status, loading, reload, setData } = useAsync(() => customerProfileApi.me(), []);
  const exists = Boolean(data);

  const [form, setForm] = useState<UserProfileRequest>(EMPTY);
  const [photo, setPhoto] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (data) {
      setForm({
        firstName: data.firstName ?? '',
        lastName: data.lastName ?? '',
        phoneNumber: data.phoneNumber ?? '',
        address: data.address ?? '',
        licenseNumber: data.licenseNumber ?? '',
        profilePictureUrl: data.profilePictureUrl ?? '',
        licenseFileUrl: data.licenseFileUrl ?? '',
      });
    }
  }, [data]);

  const set = (key: keyof UserProfileRequest) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    // profilePictureUrl is ignored by the backend; only the upload endpoint sets it.
    const body = Object.fromEntries(
      Object.entries(form).map(([k, v]) => [k, typeof v === 'string' && v.trim() === '' ? null : v?.trim()]),
    ) as unknown as UserProfileRequest;
    try {
      let saved = exists ? await customerProfileApi.update(body) : await customerProfileApi.create(body);
      // The picture endpoint requires an existing profile, so upload only after saving the details.
      if (photo) {
        try {
          await customerProfileApi.uploadPicture(photo);
          saved = await customerProfileApi.me();
          setPhoto(null);
          refreshImage();
        } catch (err) {
          setData(saved);
          notify(`Details saved, but the photo failed to upload: ${errorMessage(err)}`, 'error');
          return;
        }
      }
      setData(saved);
      notify(exists ? 'Profile updated.' : 'Profile created.', 'success');
    } catch (err) {
      notify(errorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Spinner />;
  if (error && status !== 404) return <ErrorBanner message={error} onRetry={reload} />;

  return (
    <>
      <PageHeader
        title="Your profile"
        subtitle={exists ? `Last updated ${formatDateTime(data!.updatedAt)}` : 'Complete your profile before your first rental.'}
      />
      <form className="profile-layout" onSubmit={submit}>
        <aside className="card profile-side">
          <PhotoPicker
            file={photo}
            onChange={setPhoto}
            fallback={form.firstName || user?.email || '?'}
            currentUrl={imageUrl}
            noun="photo"
            saveLabel={exists ? 'Save changes' : 'Create profile'}
            busy={busy}
          />
          <strong>{exists ? `${data!.firstName} ${data!.lastName}` : 'New customer'}</strong>
          <span className="muted small truncate">{user?.email}</span>
        </aside>

        <div className="card form">
          {!exists && <div className="alert alert-info">You don't have a profile yet — fill this in to create one.</div>}
          <h2 className="section-title">Personal details</h2>
          <div className="grid-2">
            <Field label="First name">
              <input value={form.firstName} onChange={set('firstName')} required />
            </Field>
            <Field label="Last name">
              <input value={form.lastName} onChange={set('lastName')} required />
            </Field>
            <Field label="Phone number">
              <input type="tel" value={form.phoneNumber} onChange={set('phoneNumber')} placeholder="+973 3xxx xxxx" required />
            </Field>
            <Field label="Address">
              <input value={form.address ?? ''} onChange={set('address')} />
            </Field>
          </div>
          <h2 className="section-title">Driving license</h2>
          <div className="grid-2">
            <Field label="License number">
              <input value={form.licenseNumber ?? ''} onChange={set('licenseNumber')} />
            </Field>
            <Field label="License file URL" hint="Link to a scan of your license">
              <input type="url" value={form.licenseFileUrl ?? ''} onChange={set('licenseFileUrl')} />
            </Field>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" disabled={busy}>
              {busy ? 'Saving…' : exists ? 'Save changes' : 'Create profile'}
            </button>
          </div>
        </div>
      </form>
    </>
  );
}
