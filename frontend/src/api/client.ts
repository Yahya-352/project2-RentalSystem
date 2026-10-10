import type { ErrorResponse } from './types';

/** All requests go through the Vite proxy (see vite.config.ts), which strips this prefix. */
export const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

const TOKEN_KEY = 'rental.token';

export const tokenStore = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set(token: string) {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {
      /* storage unavailable: the session just won't survive a reload */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      /* ignore */
    }
  },
};

/** Thrown for any non-2xx response, carrying the backend's ErrorResponse message when present. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

/** Called when the backend rejects our token; the AuthProvider registers a logout handler here. */
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

type Query = Record<string, string | number | boolean | null | undefined>;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Query;
  signal?: AbortSignal;
  /** 'blob' for binary responses such as images. */
  responseType?: 'json' | 'blob';
}

export function buildUrl(path: string, query?: Query): string {
  const url = `${API_BASE}${path}`;
  if (!query) return url;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') params.append(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, signal, responseType = 'json' } = options;
  const headers: Record<string, string> = { Accept: 'application/json, text/plain' };

  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;

  let payload: BodyInit | undefined;
  if (body instanceof FormData) {
    payload = body; // the browser sets the multipart boundary itself
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), { method, headers, body: payload, signal });
  } catch (err) {
    if ((err as Error).name === 'AbortError') throw err;
    throw new ApiError(0, 'NETWORK_ERROR', 'Cannot reach the server. Is the backend running?');
  }

  if (!response.ok) {
    throw await toApiError(response, Boolean(token));
  }

  if (response.status === 204) return undefined as T;
  if (responseType === 'blob') return (await response.blob()) as T;

  const text = await response.text();
  if (!text) return undefined as T;

  // Several endpoints (verify, forgot-password, change-password, ...) return a plain string.
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) return JSON.parse(text) as T;
  return text as T;
}

async function toApiError(response: Response, hadToken: boolean): Promise<ApiError> {
  let data: Partial<ErrorResponse> = {};
  try {
    data = await response.json();
  } catch {
    /* body was not JSON */
  }

  // Spring Security answers 401/403 with an empty body when the token is missing or invalid.
  // A 403 *with* a message is a business rule ("You do not own this car") and must not log out.
  const tokenRejected = hadToken && (response.status === 401 || (response.status === 403 && !data.message));
  if (tokenRejected && onUnauthorized) onUnauthorized();

  const fallback =
    response.status === 403
      ? 'You do not have permission to do that.'
      : response.status === 404
        ? 'Not found.'
        : `Request failed (${response.status}).`;

  return new ApiError(response.status, data.error ?? String(response.status), data.message || fallback);
}

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error) return err.message;
  return 'Something went wrong.';
}
