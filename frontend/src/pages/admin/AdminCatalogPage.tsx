import { useState } from 'react';
import { errorMessage } from '../../api/client';
import { catalogApi } from '../../api/endpoints';
import { ErrorBanner, Field, Modal, PageHeader, Spinner } from '../../components/ui';
import { resetCatalogCache } from '../../lib/useCatalog';
import { useAsync } from '../../lib/useAsync';
import { useNotifications } from '../../notifications/NotificationContext';

type Kind = 'make' | 'category';

interface Item {
  id: number;
  name: string;
}

const byName = (a: Item, b: Item) => a.name.localeCompare(b.name);

/** Admin page for the car makes and categories that agencies pick from when listing a car. */
export function AdminCatalogPage() {
  const makes = useAsync(() => catalogApi.makes().then((list) => [...list].sort(byName)), []);
  const categories = useAsync(() => catalogApi.categories().then((list) => [...list].sort(byName)), []);

  return (
    <>
      <PageHeader title="Makes & categories" subtitle="The lists agencies choose from when they add a car." />
      <div className="admin-grid">
        <CatalogList kind="make" title="Makes" state={makes} />
        <CatalogList kind="category" title="Categories" state={categories} />
      </div>
    </>
  );
}

function CatalogList({
  kind,
  title,
  state,
}: {
  kind: Kind;
  title: string;
  state: { data: Item[] | undefined; error: string | null; loading: boolean; reload: () => void };
}) {
  const { notify } = useNotifications();
  const [newName, setNewName] = useState('');
  const [editing, setEditing] = useState<Item | null>(null);
  const [editName, setEditName] = useState('');
  const [busy, setBusy] = useState(false);

  const save = async (action: () => Promise<unknown>, message: string) => {
    setBusy(true);
    try {
      await action();
      resetCatalogCache();
      notify(message, 'success');
      state.reload();
      return true;
    } catch (err) {
      notify(errorMessage(err), 'error');
      return false;
    } finally {
      setBusy(false);
    }
  };

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    const name = newName.trim();
    if (!name) return;
    const call = kind === 'make' ? () => catalogApi.createMake(name) : () => catalogApi.createCategory(name);
    if (await save(call, `${name} added.`)) setNewName('');
  };

  const rename = async (e: React.FormEvent) => {
    e.preventDefault();
    const name = editName.trim();
    if (!editing || !name) return;
    const id = editing.id;
    const call = kind === 'make' ? () => catalogApi.updateMake(id, name) : () => catalogApi.updateCategory(id, name);
    if (await save(call, `Renamed to ${name}.`)) setEditing(null);
  };

  return (
    <section className="card">
      <h2 className="section-title">{title}</h2>

      <form className="toolbar" onSubmit={create}>
        <div className="search">
          <input
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder={`New ${kind}`}
            aria-label={`New ${kind}`}
          />
          <button type="submit" className="btn btn-primary" disabled={busy || !newName.trim()}>
            Add
          </button>
        </div>
      </form>

      {state.error ? (
        <ErrorBanner message={state.error} onRetry={state.reload} />
      ) : state.loading && !state.data ? (
        <Spinner />
      ) : (
        <table className="table">
          <tbody>
            {(state.data ?? []).map((item) => (
              <tr key={item.id}>
                <td>{item.name}</td>
                <td className="num">
                  <button
                    className="btn btn-sm btn-ghost"
                    onClick={() => {
                      setEditing(item);
                      setEditName(item.name);
                    }}
                  >
                    Rename
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Modal open={Boolean(editing)} title={`Rename ${kind}`} onClose={() => setEditing(null)} width={420}>
        <form className="form" onSubmit={rename}>
          <Field label="Name">
            <input value={editName} onChange={(e) => setEditName(e.target.value)} required autoFocus />
          </Field>
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setEditing(null)} disabled={busy}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={busy || !editName.trim()}>
              {busy ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </Modal>
    </section>
  );
}
