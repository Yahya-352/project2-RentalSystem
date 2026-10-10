import { catalogApi } from '../api/endpoints';
import type { CategoryResponse, MakeResponse } from '../api/types';
import { useAsync } from './useAsync';

export interface Catalog {
  makes: MakeResponse[];
  categories: CategoryResponse[];
}

const byName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name);

// Makes and categories rarely change, so they are fetched once per page load and shared.
let cached: Promise<Catalog> | null = null;

function loadCatalog(): Promise<Catalog> {
  if (!cached) {
    cached = Promise.all([catalogApi.makes(), catalogApi.categories()])
      .then(([makes, categories]) => ({ makes: [...makes].sort(byName), categories: [...categories].sort(byName) }))
      .catch((err) => {
        cached = null; // let the next caller retry
        throw err;
      });
  }
  return cached;
}

/** Makes and categories from GET /makes and GET /categories. */
export function useCatalog() {
  return useAsync(loadCatalog, []);
}

export const idByName = (list: { id: number; name: string }[], name: string) =>
  list.find((item) => item.name.toLowerCase() === name.toLowerCase())?.id;
