import type { BookingStatus, FuelType, TransmissionType } from '../api/types';

// These mirror the backend enums (FuelType, TransmissionType, BookingStatus).
// Makes and categories are NOT here: they come from GET /makes and GET /categories (see useCatalog).
export const FUEL_TYPES: FuelType[] = ['PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID'];
export const TRANSMISSIONS: TransmissionType[] = ['AUTOMATIC', 'MANUAL'];
export const BOOKING_STATUSES: BookingStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'];
