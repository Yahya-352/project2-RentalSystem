// Mirrors the backend DTOs in com.ga.RentalSystem.dto. Keep these in sync with the Java records.

export type Role = 'ADMIN' | 'CUSTOMER' | 'AGENCY';

export type BookingStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED';

export type FuelType = 'PETROL' | 'DIESEL' | 'ELECTRIC' | 'HYBRID';

export type TransmissionType = 'AUTOMATIC' | 'MANUAL';

/** Spring's LocalDate / LocalDateTime are serialised as ISO strings. */
export type IsoDate = string;
export type IsoDateTime = string;

// ---------- requests ----------

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  userName: string;
  email: string;
  password: string;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface CarRequest {
  makeId: number;
  model: string;
  categoryId: number;
  location: string;
  year: number;
  licensePlate: string;
  transmission: TransmissionType;
  fuelType: FuelType;
  seats: number;
  pricePerDay: number;
}

export interface BookingRequest {
  carId: number;
  startDate: IsoDate;
  endDate: IsoDate;
}

export interface UserProfileRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  profilePictureUrl?: string | null;
  address?: string | null;
  licenseNumber?: string | null;
  licenseFileUrl?: string | null;
}

export interface AgencyProfileRequest {
  businessName: string;
  phoneNumber: string;
  /** Ignored by the backend: the logo is set through POST /profiles/agency/logo. */
  logoUrl?: string | null;
}

// ---------- responses ----------

export interface LoginResponse {
  token: string;
  email: string;
  role: Role;
}

export interface RegisterResponse {
  id: number;
  userName: string;
  email: string;
  status: string;
  message: string;
}

export interface CarResponse {
  id: number;
  make: string;
  model: string;
  category: string;
  location: string;
  year: number;
  licensePlate: string;
  transmission: TransmissionType;
  fuelType: FuelType;
  seats: number;
  pricePerDay: number;
  available: boolean;
  ownerUsername: string;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
  /** Ids for GET /images/{id}, oldest first; the first one is the cover photo. */
  imageIds: number[];
}

export interface BookingResponse {
  id: number;
  carId: number;
  make: string;
  model: string;
  renterId: number;
  startDate: IsoDate;
  endDate: IsoDate;
  totalPrice: number;
  status: BookingStatus;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface UserProfileResponse {
  id: number;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  profilePictureUrl: string | null;
  address: string | null;
  licenseNumber: string | null;
  licenseFileUrl: string | null;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface AgencyProfileResponse {
  id: number;
  businessName: string;
  phoneNumber: string;
  logoUrl: string | null;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface MakeResponse {
  id: number;
  name: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ErrorResponse {
  timestamp?: IsoDateTime;
  status: number;
  error: string;
  message: string;
  path?: string;
}
