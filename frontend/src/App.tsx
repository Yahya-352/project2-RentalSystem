import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { homeFor, useAuth } from './auth/AuthContext';
import { GuestOnly, RequireAuth } from './auth/RequireAuth';
import { Layout } from './components/Layout';
import { EmptyState } from './components/ui';
import { AccountPage } from './pages/AccountPage';
import { AdminBookingsPage } from './pages/admin/AdminBookingsPage';
import { AdminUsersPage } from './pages/admin/AdminUsersPage';
import { AgencyBookingsPage } from './pages/agency/AgencyBookingsPage';
import { AgencyProfilePage } from './pages/agency/AgencyProfilePage';
import { MyCarsPage } from './pages/agency/MyCarsPage';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ForgotPasswordPage, ResetPasswordPage, VerifyPage } from './pages/auth/TokenPages';
import { CarDetailPage } from './pages/customer/CarDetailPage';
import { CarsPage } from './pages/customer/CarsPage';
import { CustomerProfilePage } from './pages/customer/CustomerProfilePage';
import { MyBookingsPage } from './pages/customer/MyBookingsPage';

export default function App() {
  const { user } = useAuth();

  return (
    <Routes>
      <Route path="/" element={<Navigate to={user ? homeFor(user.role) : '/login'} replace />} />

      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      </Route>
      {/* Token links work whether or not someone is logged in. */}
      <Route path="/verify" element={<VerifyPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          {/* Browsing requires a token: the backend protects GET /cars. */}
          <Route path="/cars" element={<CarsPage />} />
          <Route path="/cars/:id" element={<CarDetailPage />} />
          <Route path="/account" element={<AccountPage />} />

          <Route element={<RequireAuth roles={['CUSTOMER']} />}>
            <Route path="/bookings" element={<MyBookingsPage />} />
            <Route path="/profile" element={<CustomerProfilePage />} />
          </Route>

          <Route element={<RequireAuth roles={['AGENCY']} />}>
            <Route path="/agency/cars" element={<MyCarsPage />} />
            <Route path="/agency/bookings" element={<AgencyBookingsPage />} />
            <Route path="/agency/profile" element={<AgencyProfilePage />} />
          </Route>

          <Route element={<RequireAuth roles={['ADMIN']} />}>
            <Route path="/admin/bookings" element={<AdminBookingsPage />} />
            <Route path="/admin/users" element={<AdminUsersPage />} />
          </Route>

          <Route
            path="*"
            element={
              <EmptyState title="Page not found">
                <Link to="/">Go home</Link>
              </EmptyState>
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}
