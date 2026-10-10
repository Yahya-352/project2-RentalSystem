import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { agencyProfileApi, customerProfileApi } from '../api/endpoints';
import { useAuth } from './AuthContext';

interface ProfileImageContextValue {
  /** Local object URL of the logged-in user's picture (customer) or logo (agency), if any. */
  imageUrl: string | null;
  /** Re-fetch after an upload. */
  refresh: () => void;
}

const ProfileImageContext = createContext<ProfileImageContextValue | null>(null);

/**
 * The image endpoints require the JWT, which an <img> tag cannot send, so the image is fetched
 * once as a blob and shared by the top-bar avatar and the profile pages.
 */
export function ProfileImageProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const load =
      user?.role === 'CUSTOMER' ? customerProfileApi.picture : user?.role === 'AGENCY' ? agencyProfileApi.logo : null;
    if (!load) {
      setImageUrl(null);
      return;
    }

    let objectUrl: string | null = null;
    let cancelled = false;
    load()
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setImageUrl(objectUrl);
      })
      // 404 = no profile or no image uploaded yet: fall back to the initial.
      .catch(() => {
        if (!cancelled) setImageUrl(null);
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [user?.email, user?.role, version]);

  const refresh = useCallback(() => setVersion((v) => v + 1), []);
  const value = useMemo(() => ({ imageUrl, refresh }), [imageUrl, refresh]);

  return <ProfileImageContext.Provider value={value}>{children}</ProfileImageContext.Provider>;
}

export function useProfileImage() {
  const ctx = useContext(ProfileImageContext);
  if (!ctx) throw new Error('useProfileImage must be used inside <ProfileImageProvider>');
  return ctx;
}
