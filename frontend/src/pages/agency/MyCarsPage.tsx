import { useRef, useState } from 'react';
import { errorMessage } from '../../api/client';
import { carsApi } from '../../api/endpoints';
import type { CarRequest, CarResponse } from '../../api/types';
import { CarForm } from '../../components/CarForm';
import { ConfirmDialog, EmptyState, ErrorBanner, Modal, PageHeader, Spinner } from '../../components/ui';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';
import { CarCard } from '../customer/CarsPage';

const MAX_FILE_BYTES = 5 * 1024 * 1024; // spring.servlet.multipart.max-file-size
const MAX_REQUEST_BYTES = 50 * 1024 * 1024; // spring.servlet.multipart.max-request-size (dev profile)

type Editing = { mode: 'create' } | { mode: 'edit'; car: CarResponse } | null;

export function MyCarsPage() {
  const { notify } = useNotifications();
  const { data, error, loading, reload, setData } = useAsync(() => carsApi.mine(), []);
  const [editing, setEditing] = useState<Editing>(null);
  const [deleting, setDeleting] = useState<CarResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [uploadingId, setUploadingId] = useState<number | null>(null);
  const uploadFor = useRef<CarResponse | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  const save = async (body: CarRequest) => {
    setBusy(true);
    setFormError(null);
    try {
      if (editing?.mode === 'edit') {
        const updated = await carsApi.update(editing.car.id, body);
        setData((list) => list?.map((c) => (c.id === updated.id ? updated : c)));
        notify('Car updated.', 'success');
      } else {
        const created = await carsApi.create(body);
        setData((list) => [created, ...(list ?? [])]);
        notify('Car added to your fleet.', 'success');
      }
      setEditing(null);
    } catch (err) {
      setFormError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!deleting) return;
    setBusy(true);
    try {
      await carsApi.remove(deleting.id);
      setData((list) => list?.filter((c) => c.id !== deleting.id));
      notify('Car removed.', 'success');
      setDeleting(null);
    } catch (err) {
      notify(errorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  };

  const pickImage = (car: CarResponse) => {
    uploadFor.current = car;
    fileInput.current?.click();
  };

  const upload = async (fileList: FileList | null) => {
    const car = uploadFor.current;
    const files = Array.from(fileList ?? []);
    if (fileInput.current) fileInput.current.value = '';
    if (files.length === 0 || !car) return;

    // Same limits as the backend: JPG/PNG, 5 MB per file, 50 MB per request.
    const invalid = files.find((f) => !['image/png', 'image/jpeg'].includes(f.type));
    if (invalid) return notify(`${invalid.name}: only JPG and PNG images are allowed.`, 'error');
    const tooBig = files.find((f) => f.size > MAX_FILE_BYTES);
    if (tooBig) return notify(`${tooBig.name} is larger than 5 MB.`, 'error');
    if (files.reduce((sum, f) => sum + f.size, 0) > MAX_REQUEST_BYTES) {
      return notify('Those photos add up to more than 50 MB. Upload fewer at a time.', 'error');
    }

    setUploadingId(car.id);
    try {
      await carsApi.uploadImages(car.id, files);
      // Reload the car so its imageIds include the new photos.
      const updated = await carsApi.get(car.id);
      setData((list) => list?.map((c) => (c.id === updated.id ? updated : c)));
      notify(`${files.length} photo${files.length === 1 ? '' : 's'} uploaded for ${car.make} ${car.model}.`, 'success');
    } catch (err) {
      notify(errorMessage(err), 'error');
    } finally {
      setUploadingId(null);
    }
  };

  const openEditor = (next: Editing) => {
    setFormError(null);
    setEditing(next);
  };

  return (
    <>
      <PageHeader
        title="My fleet"
        subtitle={data ? `${data.length} car${data.length === 1 ? '' : 's'} listed` : undefined}
        actions={
          <button className="btn btn-primary" onClick={() => openEditor({ mode: 'create' })}>
            + Add car
          </button>
        }
      />

      <input
        ref={fileInput}
        type="file"
        accept="image/png,image/jpeg"
        multiple
        hidden
        onChange={(e) => upload(e.target.files)}
      />

      {error ? (
        <ErrorBanner message={error} onRetry={reload} />
      ) : loading && !data ? (
        <Spinner />
      ) : !data || data.length === 0 ? (
        <EmptyState title="Your fleet is empty">
          <button className="btn btn-primary" onClick={() => openEditor({ mode: 'create' })}>
            List your first car
          </button>
        </EmptyState>
      ) : (
        <div className="car-grid">
          {data.map((car) => (
            <CarCard
              key={car.id}
              car={car}
              actions={
                <>
                  <button className="btn btn-sm btn-ghost" onClick={() => openEditor({ mode: 'edit', car })}>
                    Edit
                  </button>
                  <button
                    className="btn btn-sm btn-ghost"
                    onClick={() => pickImage(car)}
                    disabled={uploadingId === car.id}
                  >
                    {uploadingId === car.id
                      ? 'Uploading…'
                      : `Add photos${car.imageIds?.length ? ` (${car.imageIds.length})` : ''}`}
                  </button>
                  <button className="btn btn-sm btn-ghost danger-text" onClick={() => setDeleting(car)}>
                    Delete
                  </button>
                </>
              }
            />
          ))}
        </div>
      )}

      <Modal
        open={Boolean(editing)}
        title={editing?.mode === 'edit' ? `Edit ${editing.car.make} ${editing.car.model}` : 'Add a car'}
        onClose={() => !busy && setEditing(null)}
        width={680}
      >
        {formError && <div className="alert alert-error">{formError}</div>}
        {editing && (
          <CarForm
            key={editing.mode === 'edit' ? editing.car.id : 'new'}
            initial={editing.mode === 'edit' ? editing.car : undefined}
            busy={busy}
            onSubmit={save}
            onCancel={() => setEditing(null)}
          />
        )}
      </Modal>

      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete car?"
        message={
          deleting && (
            <>
              Remove{' '}
              <strong>
                {deleting.make} {deleting.model} ({deleting.licensePlate})
              </strong>{' '}
              from your fleet? Customers will no longer see it.
            </>
          )
        }
        confirmLabel="Delete car"
        danger
        busy={busy}
        onConfirm={remove}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}
