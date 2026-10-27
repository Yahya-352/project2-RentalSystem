import { request } from './client';
import type {
  AgencyProfileRequest,
  AgencyProfileResponse,
  BookedDateRange,
  BookingRequest,
  BookingResponse,
  BookingStatus,
  CarRequest,
  CarResponse,
  CategoryResponse,
  ChangePasswordRequest,
  CityResponse,
  CountryResponse,
  FuelType,
  IsoDate,
  LoginRequest,
  LoginResponse,
  MakeResponse,
  PageResponse,
  PaymentResponse,
  RegisterRequest,
  RegisterResponse,
  ResetPasswordRequest,
  TransmissionType,
  UserProfileRequest,
  UserProfileResponse,
} from './types';

/** One function per backend endpoint, grouped by controller. */

export const usersApi = {
  login: (body: LoginRequest) => request<LoginResponse>('/users/login', { method: 'POST', body }),
  registerCustomer: (body: RegisterRequest) =>
    request<RegisterResponse>('/users/register', { method: 'POST', body }),
  registerAgency: (body: RegisterRequest) =>
    request<RegisterResponse>('/users/register/agency', { method: 'POST', body }),
  verify: (token: string) => request<string>('/users/verify', { query: { token } }),
  resendVerification: (email: string) =>
    request<string>('/users/resend-verification', { method: 'POST', query: { email } }),
  forgotPassword: (email: string) =>
    request<string>('/users/forgot-password', { method: 'POST', query: { email } }),
  resetPassword: (body: ResetPasswordRequest) =>
    request<string>('/users/reset-password', { method: 'POST', body }),
  changePassword: (body: ChangePasswordRequest) =>
    request<string>('/users/change-password', { method: 'POST', body }),
  activate: (id: number) => request<void>(`/users/${id}/activate`, { method: 'PATCH' }),
  deactivate: (id: number) => request<void>(`/users/${id}/deactivate`, { method: 'PATCH' }),
};

/** Every filter is optional; the backend combines all the ones that are sent. */
export interface CarSearch {
  countryId?: number;
  cityId?: number;
  categoryId?: number;
  makeId?: number;
  minPrice?: number;
  maxPrice?: number;
  minSeats?: number;
  transmission?: TransmissionType;
  fuelType?: FuelType;
  /** Send both or neither: only cars with no approved booking overlapping these dates come back. */
  startDate?: IsoDate;
  endDate?: IsoDate;
  page?: number;
  size?: number;
  sort?: string;
}

export const carsApi = {
  // Note the trailing slash: the backend maps this endpoint to "/cars/".
  search: (params: CarSearch = {}, signal?: AbortSignal) =>
    request<PageResponse<CarResponse>>('/cars/', { query: { ...params }, signal }),
  get: (id: number) => request<CarResponse>(`/cars/${id}`),
  mine: () => request<CarResponse[]>('/cars/my-cars'),
  create: (body: CarRequest) => request<CarResponse>('/cars/create', { method: 'POST', body }),
  update: (id: number, body: CarRequest) =>
    request<CarResponse>(`/cars/update/${id}`, { method: 'PUT', body }),
  remove: (id: number) => request<void>(`/cars/delete/${id}`, { method: 'DELETE' }),
  /** Sends all files in one request; the backend reads them as a list under "file". */
  uploadImages: (id: number, files: File[]) => {
    const form = new FormData();
    files.forEach((file) => form.append('file', file));
    return request<void>(`/cars/${id}/images`, { method: 'POST', body: form });
  },
  image: (imageId: number) => request<Blob>(`/images/${imageId}`, { responseType: 'blob' }),
};

export const catalogApi = {
  makes: () => request<MakeResponse[]>('/makes'),
  categories: () => request<CategoryResponse[]>('/categories'),
};

export const locationsApi = {
  countries: () => request<CountryResponse[]>('/countries'),
  cities: (countryId: number) => request<CityResponse[]>('/cities', { query: { countryId } }),
};

export const bookingsApi = {
  create: (body: BookingRequest) => request<BookingResponse>('/bookings', { method: 'POST', body }),
  mine: () => request<BookingResponse[]>('/bookings/me'),
  forMyCars: () => request<BookingResponse[]>('/bookings/my-cars'),
  approve: (id: number) => request<BookingResponse>(`/bookings/${id}/approve`, { method: 'PATCH' }),
  reject: (id: number) => request<BookingResponse>(`/bookings/${id}/reject`, { method: 'PATCH' }),
  cancel: (id: number) => request<BookingResponse>(`/bookings/${id}/cancel`, { method: 'PATCH' }),
  all: (params: { status?: BookingStatus | ''; page?: number; size?: number; sort?: string }) =>
    request<PageResponse<BookingResponse>>('/bookings', { query: { ...params } }),
  /** Upcoming approved bookings of a car, for the availability calendar. */
  bookedDates: (carId: number) => request<BookedDateRange[]>(`/bookings/car/${carId}/dates`),
};

export const paymentsApi = {
  /** Creates a Stripe Checkout session for an approved booking; the browser is then sent to sessionUrl. */
  checkout: (bookingId: number) => request<PaymentResponse>(`/pay/${bookingId}`, { method: 'POST' }),
};

export const customerProfileApi = {
  me: () => request<UserProfileResponse>('/profiles/customer/me'),
  create: (body: UserProfileRequest) =>
    request<UserProfileResponse>('/profiles/customer', { method: 'POST', body }),
  update: (body: UserProfileRequest) =>
    request<UserProfileResponse>('/profiles/customer/me', { method: 'PUT', body }),
  uploadPicture: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<void>('/profiles/customer/picture', { method: 'POST', body: form });
  },
  picture: () => request<Blob>('/profiles/customer/picture', { responseType: 'blob' }),
};

export const agencyProfileApi = {
  me: () => request<AgencyProfileResponse>('/profiles/agency/me'),
  create: (body: AgencyProfileRequest) =>
    request<AgencyProfileResponse>('/profiles/agency', { method: 'POST', body }),
  update: (body: AgencyProfileRequest) =>
    request<AgencyProfileResponse>('/profiles/agency/me', { method: 'PUT', body }),
  uploadLogo: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<void>('/profiles/agency/logo', { method: 'POST', body: form });
  },
  logo: () => request<Blob>('/profiles/agency/logo', { responseType: 'blob' }),
};
