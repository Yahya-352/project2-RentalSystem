import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError, errorMessage } from '../api/client';

interface AsyncState<T> {
  data: T | undefined;
  error: string | null;
  status: number | null;
  loading: boolean;
  reload: () => void;
  setData: React.Dispatch<React.SetStateAction<T | undefined>>;
}

/** Runs `fn` on mount and whenever `deps` change; ignores results from stale runs. */
export function useAsync<T>(fn: () => Promise<T>, deps: React.DependencyList): AsyncState<T> {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [tick, setTick] = useState(0);
  const runId = useRef(0);

  const run = useCallback(fn, deps);

  useEffect(() => {
    const id = ++runId.current;
    setLoading(true);
    setError(null);
    setStatus(null);
    run()
      .then((result) => {
        if (id === runId.current) setData(result);
      })
      .catch((err) => {
        if (id !== runId.current || (err as Error).name === 'AbortError') return;
        setError(errorMessage(err));
        setStatus(err instanceof ApiError ? err.status : null);
      })
      .finally(() => {
        if (id === runId.current) setLoading(false);
      });
  }, [run, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { data, error, status, loading, reload, setData };
}
