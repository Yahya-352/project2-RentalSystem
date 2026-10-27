import { locationsApi } from '../api/endpoints';
import type { CityResponse, CountryResponse } from '../api/types';
import { useAsync } from './useAsync';

const byName = (a: { name: string }, b: { name: string }) => a.name.localeCompare(b.name);

// Countries and cities are seeded reference data, so each list is fetched once per page load and shared.
let countriesCache: Promise<CountryResponse[]> | null = null;
const citiesCache = new Map<number, Promise<CityResponse[]>>();

function loadCountries(): Promise<CountryResponse[]> {
  if (!countriesCache) {
    countriesCache = locationsApi
      .countries()
      .then((list) => [...list].sort(byName))
      .catch((err) => {
        countriesCache = null; // let the next caller retry
        throw err;
      });
  }
  return countriesCache;
}

function loadCities(countryId: number): Promise<CityResponse[]> {
  let cities = citiesCache.get(countryId);
  if (!cities) {
    cities = locationsApi
      .cities(countryId)
      .then((list) => [...list].sort(byName))
      .catch((err) => {
        citiesCache.delete(countryId);
        throw err;
      });
    citiesCache.set(countryId, cities);
  }
  return cities;
}

/** All countries from GET /countries, A–Z. */
export function useCountries() {
  return useAsync(loadCountries, []);
}

/** Cities of the given country from GET /cities, A–Z; an empty list while no country is chosen. */
export function useCities(countryId: number | null | undefined) {
  return useAsync(() => (countryId ? loadCities(countryId) : Promise.resolve([])), [countryId]);
}
