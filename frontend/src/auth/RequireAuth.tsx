import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '../api/types';
import { homeFor, useAuth } from './AuthContext';

/** Guards a group of routes. Without `roles`, any logged-in user may enter. */
export function RequireAuth({ roles }: { roles?: Role[] }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  if (roles && !roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />;
  return <Outlet />;
}

/** For login/register pages: logged-in users are sent to their home page. */
export function GuestOnly() {
  const { user } = useAuth();
  return user ? <Navigate to={homeFor(user.role)} replace /> : <Outlet />;
}
